#!/usr/bin/env python3

import argparse, csv, filecmp, functools, itertools
import math, multiprocessing, os, sys, tempfile, traceback

import numpy as np

import matplotlib
matplotlib.use('PDF')
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

import benchmark.general
import benchmark.tasks
import benchmark.config_optimise as config

def error_callback(location, exception):
  print("Error in {0}: {1}".format(location, exception),
        file=sys.stderr)

def sanitize_fname(fname):
  fname = os.path.relpath(fname, config.CORPUS_DIR)
  return fname.replace('/', '_')

def corpus_size(fname):
  return os.path.getsize(os.path.join(config.CORPUS_DIR, fname))

def save_figure(fig, output_dir, fname):
  fig_fname = sanitize_fname(fname) + ".pdf"
  fig_dir = os.path.join(config.FIGURE_DIR, output_dir)
  os.makedirs(fig_dir, exist_ok=True)

  fig_path = os.path.join(fig_dir, fig_fname)
  with PdfPages(fig_path) as out:
    if verbose:
      print("Writing figure to " + fig_path)
    out.savefig(fig)
  return fig_path

def per_file_test(test):
  def f(pool, files, test_name, **kwargs):
    runner = functools.partial(test, test_name, **kwargs)
    for fname in files:
      def callback(res):
        print("FINISHED: {0} on {1}: {2}".format(test_name, fname, res))
      ec = functools.partial(error_callback, "per_file_test - {0} on {1}".format(test_name, fname))
      pool.apply_async(runner, (fname, ), callback=callback, error_callback=ec)
  return f

def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  return (point - new_width / 2, point + new_width / 2)

def ppm_grid_search(fname, paranoia, prior, depth, iterations, shrink_factor,
                    alpha_range, beta_range, granularity):
  optimum = None
  evals = []
  for i in range(iterations):
    res = benchmark.tasks.optimise_brute(fname, paranoia, prior, depth,
                                          alpha_range, beta_range, granularity)
    optimum, eval = res.get()
    evals.append(eval)

    argmin = optimum[0]
    alpha_range = range_around(argmin[0], alpha_range, shrink_factor)
    beta_range = range_around(argmin[1], beta_range, shrink_factor)
  return optimum, evals

def contour(optimum, evals, delta, num_levels, xlim, ylim):
  fig = plt.figure()

  # plot optimum
  (optimum_a, optimum_b), optimum_z = optimum
  plt.scatter(optimum_b, optimum_a, marker='x')
  plt.annotate('OPT', xy=(optimum_b, optimum_a), xycoords='data',
               xytext=(2,2), textcoords='offset points')

  # plot contours
  rounded_z = math.ceil(optimum_z / delta) * delta
  levels = rounded_z + np.arange(1, num_levels + 1)*delta
  # TODO: multi-resolution
  (a, b), z = evals[0]
  cs = plt.contour(b, a, z, levels=levels, linewidths=1)
  # SOMEDAY: can manually specify locations, will probably want to do this in final version
  plt.clabel(cs, fontsize=10, inline=1)

  # shade illegal parameter region, a + b <= 0
  min_x, max_x = xlim
  min_y, max_y = ylim
  x_vertices = [min_x, min_x, max_x]
  lowest = min(min_y, -max_x)
  y_vertices = [-min_x, lowest, lowest]
  plt.fill(x_vertices, y_vertices, 'gray', alpha=0.5)

  # axes
  plt.xlim(xlim)
  plt.ylim(ylim)

  plt.xlabel(r'discount $\beta$')
  plt.ylabel(r'strength $\alpha$')

  return fig

def grid_to_efficiency(optimum, evals, original_size):
  optimum = (optimum[0], optimum[1] * 8 / original_size)
  def eval_helper(eval):
    return (eval[0], eval[1] * 8 / original_size)
  evals = list(map(eval_helper, evals))
  return (optimum, evals)

def ppm_contour_plot_helper(test_name, fname, prior, depth,
                            granularity=config.PPM_CONTOUR_GRANULARITY,
                            alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                            beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END,
                            iterations=config.PPM_CONTOUR_NUM_ITERATIONS,
                            shrink_factor=config.PPM_CONTOUR_SHRINK_FACTOR,
                            num_levels=config.PPM_CONTOUR_NUM_LEVELS,
                            delta=config.PPM_CONTOUR_DELTA):
  depth = int(depth)
  granularity = int(granularity)

  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  optimum, evals = ppm_grid_search(fname, paranoia, prior, depth, int(iterations),
                                   int(shrink_factor), alpha_range, beta_range, granularity)
  optres = benchmark.tasks.ppm_minimize.delay(fname, paranoia, prior, depth, optimum[0]).get()
  optimum = optres.x, optres.fun

  original_size = corpus_size(fname)
  optimum, evals = grid_to_efficiency(optimum, evals, original_size)
  fig = contour(optimum, evals, num_levels=int(num_levels), delta=float(delta),
                xlim=beta_range, ylim=alpha_range)

  return save_figure(fig, test_name, fname)
ppm_contour_plot = per_file_test(ppm_contour_plot_helper)

def ppm_find_optimal_alpha_beta(fname, paranoia, prior, depth, granularity, method):
  initial_guess = (0, 0.5) # PPMD

  if granularity >= 1:
    res = ppm_grid_search(fname, paranoia, prior, depth,
                          iterations=1, shrink_factor=1, granularity=granularity,
                          alpha_range=(config.PPM_ALPHA_START, config.PPM_ALPHA_END),
                          beta_range=(config.PPM_BETA_START, config.PPM_BETA_END))
    optimum, evals = res
    initial_guess, _ = optimum

  optres = benchmark.tasks.ppm_minimize.delay(fname, paranoia, prior,
                                              depth, initial_guess, method).get()
  return (optres.x, optres.fun)

def parse_depths(depths):
  if type(depths) == str: # parameter passed at CLI
    return list(map(int, depths.split(",")))
  else:
    return depths

def best_parameters_by_depth(res):
  best_alpha = float('nan')
  best_beta = float('nan')
  best_depth = -1
  best_val = float('inf')

  for depth, x in res.items():
    (alpha, beta), val = x
    if val < best_val:
      best_val = val
      best_alpha, best_beta = alpha, beta
      best_depth = depth

  return (best_depth, best_alpha, best_beta, best_val)

def ppm_optimal_parameters_helper(paranoia, prior, granularity, method, x):
  fname, depth = x
  return ppm_find_optimal_alpha_beta(fname, paranoia, prior, depth, granularity, method)

def ppm_optimal_parameters(pool, files, test_name, prior,
                           granularity=config.PPM_PARAMETER_GRANULARITY,
                           depths=config.PPM_PARAMETER_DEPTHS, method='Nelder-Mead'):
  depths = parse_depths(depths)
  granularity = int(granularity)
  work = list(itertools.product(files, depths))

  def callback(res):
    k = 0
    res_by_file = {}
    for (file, depth) in work:
      d = res_by_file.get(file, {})
      d[depth] = res[k]
      res_by_file[file] = d
      k += 1

    os.makedirs(config.TABLE_DIR, exist_ok=True)
    csv_path = os.path.join(config.TABLE_DIR, test_name + '.csv')
    with open(csv_path, 'w') as f:
      writer = csv.writer(f)
      fieldnames = ['file', 'depth', 'alpha', 'beta', 'compressed_size', 'efficiency']
      writer.writerow(fieldnames)
      for fname, values in res_by_file.items():
        original_size = corpus_size(fname)
        depth, alpha, beta, size = best_parameters_by_depth(values)
        efficiency = size * 8 / original_size
        rel_fname = os.path.relpath(fname, config.CORPUS_DIR)
        writer.writerow([rel_fname, depth, alpha, beta, int(size), efficiency])

  runner = functools.partial(ppm_optimal_parameters_helper, paranoia, prior, granularity, method)
  ec = functools.partial(error_callback, "ppm_optimal_parameters - {0} on {1}".format(test_name, files))
  pool.map_async(runner, work, chunksize=1, callback=callback, error_callback=ec)

def ppm_optimal_alpha_beta_helper(test_name, fname, prior,
                                  granularity=config.PPM_PARAMETER_GRANULARITY,
                                  depths=config.PPM_PARAMETER_DEPTHS,
                                  method='Nelder-Mead'):
  granularity = int(granularity)
  depths = parse_depths(depths)

  original_size = corpus_size(fname)

  csv_dir = os.path.join(config.TABLE_DIR, test_name)
  os.makedirs(csv_dir, exist_ok=True)
  csv_path = os.path.join(csv_dir, sanitize_fname(fname) + '.csv')
  with open(csv_path, 'w') as f:
    fieldnames = ['depth', 'alpha', 'beta', 'compressed_size', 'efficiency']
    writer = csv.writer(f)
    writer.writerow(fieldnames)

    for d in depths:
      (alpha, beta), compressed_size = ppm_find_optimal_alpha_beta(fname, paranoia, prior,
                                                              d, granularity, method)
      writer.writerow([d, alpha, beta, int(compressed_size), compressed_size / original_size * 8])
    return csv_path
ppm_optimal_alpha_beta = per_file_test(ppm_optimal_alpha_beta_helper)

# TODO: canonical sorting
def to_kwargs(xs):
  d = {}
  for x in xs:
    kv = x.split("=", 1)
    if len(kv) < 2:
      print("ERROR: malformed key-value pair '" + x + "'")
    else:
      k, v = kv
      d[k] = v
  return d

def canonical_name(name, kwargs):
  res = str(name)
  for k, v in sorted(kwargs.items()):
    res += ":{0}={1}".format(k, v)
  return res

verbose=False
paranoia=False

TESTS = {
  'ppm_contour_plot': ppm_contour_plot,
  'ppm_optimal_parameters': ppm_optimal_parameters,
  'ppm_optimal_alpha_beta': ppm_optimal_alpha_beta,
}

# None of the workers are resource intensive in their own right, so OK to exceed the # of cores
NUM_WORKERS = 16

def main():
  description = "Produce visualisations and find optimal parameters of compression algorithms"
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('--paranoia', dest='paranoia', action='store_true',
                      help='verify correct operation of compression algorithms by decompressing ' +
                           'their output and comparing to the original file.')
  parser.add_argument('--rerun', dest='rerun', action='store_true',
                      help='regenerate the data, even if there is a cached result.')
  parser.add_argument('--include', dest='include', nargs='+',
                      help='paths which match the specified regex are included; ' +
                           'if unspecified, defaults to *.')
  parser.add_argument('--exclude', dest='exclude', nargs='+',
                      help='paths which match the specified regex are excluded.')
  parser.add_argument('tests', nargs='*',
                      help='list of tests to conduct; format is test_name[:parameter1=value1[:...]]')

  args = vars(parser.parse_args())
  global verbose, paranoia, use_cache
  verbose = args['verbose']
  paranoia = args['paranoia']
  use_cache = not args['rerun']

  files = benchmark.general.include_exclude_files(args['include'], args['exclude'])
  files = list(map(lambda fname: os.path.join(config.CORPUS_DIR, fname), files))

  pool = multiprocessing.Pool(NUM_WORKERS)
  if verbose:
    print("Splitting work across {0} processes".format(NUM_WORKERS))

  if not args['tests']:
    print("WARNING: no tests specified", file=sys.stderr)
  for test in args['tests']:
    if not test: # empty string
      print("ERROR: test name cannot be an empty string", file=sys.stderr)
      continue
    test_name, *test_args = test.split(":")
    test_kwargs = to_kwargs(test_args)
    test_id = canonical_name(test_name, test_kwargs)

    if test_name in TESTS:
      test_runner = TESTS[test_name]
      if verbose:
        print("Running " + test_id)
      test_runner(pool, files, test_id, **test_kwargs)
    else:
      print("ERROR: unrecognised test '" + test_name + "'")

  pool.close()
  pool.join()
  print("All tests finished.")

if __name__ == "__main__":
  main()
