#!/usr/bin/env python3

import argparse, csv, functools, itertools
import math, multiprocessing, os, pickle, sys, traceback

import numpy as np

import matplotlib
matplotlib.use('PDF')
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

import benchmark.general
import benchmark.tasks
import benchmark.config_optimise as config

def worker_wrapper(func, *args, **kwargs):
  try:
    return func(*args, **kwargs)
  except Exception:
    traceback.print_exc()
    raise

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

def load_pickle(output_dir, fname):
  pickled_fname = sanitize_fname(fname) + ".o"
  pickled_dir = os.path.join(config.DATA_DIR, output_dir)

  pickled_path = os.path.join(pickled_dir, pickled_fname)
  with open(pickled_path, 'rb') as input:
    return pickle.load(input)

def save_pickle(o, output_dir, fname):
  pickled_fname = sanitize_fname(fname) + ".o"
  pickled_dir = os.path.join(config.DATA_DIR, output_dir)
  os.makedirs(pickled_dir, exist_ok=True)

  pickled_path = os.path.join(pickled_dir, pickled_fname)
  with open(pickled_path, 'wb') as out:
    if verbose:
      print("Pickling data to " + pickled_path)
    pickle.dump(o, out)

def per_file_test(test):
  def f(pool, files, test_name, **kwargs):
    runner = functools.partial(test, test_name, **kwargs)
    for fname in files:
      def callback(res):
        print("FINISHED: {0} on {1}: {2}".format(test_name, fname, res))
      ec = functools.partial(error_callback, "per_file_test - {0} on {1}".format(test_name, fname))
      pool.apply_async(worker_wrapper, (runner, fname, ), callback=callback, error_callback=ec)
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
    optimum, eval = benchmark.tasks.optimise_brute(fname, paranoia, prior, depth,
                                                   alpha_range, beta_range, granularity)
    evals.append(eval)

    argmin = optimum[0]
    alpha_range = range_around(argmin[0], alpha_range, shrink_factor)
    beta_range = range_around(argmin[1], beta_range, shrink_factor)
  return optimum, evals

def round_up_to(multiple, x):
  return math.ceil(x / multiple) * multiple

def contour(optimum, evals, big_delta, small_per_big, big_levels, xlim, ylim):
  fig = plt.figure()

  # plot optimum
  (optimum_a, optimum_b), optimum_z = optimum
  plt.scatter(optimum_b, optimum_a, marker='x')
  plt.annotate('OPT', xy=(optimum_b, optimum_a), xycoords='data',
               xytext=(2,2), textcoords='offset points')

  # plot contours
  small_delta = big_delta / small_per_big
  rounded_z_small = round_up_to(small_delta, optimum_z)
  rounded_z_big = round_up_to(big_delta, optimum_z)

  inner_levels = int((rounded_z_big - rounded_z_small) / small_delta)
  num_levels = inner_levels + big_levels * small_per_big
  levels = rounded_z_small + np.arange(1, num_levels + 1)*small_delta

  small_linewidth, big_linewidth = 0.1, 1
  inner_linewidths = np.concatenate((np.tile([small_linewidth], inner_levels), [big_linewidth]))
  usual_linewidths = np.concatenate((np.tile([small_linewidth], small_per_big - 1), [big_linewidth]))
  linewidths = np.concatenate((inner_linewidths, np.tile(usual_linewidths, big_levels)))
  (a, b), z = evals[0]
  cs = plt.contour(b, a, z, levels=levels, linewidths=linewidths)

  # labels
  def big_formatter(level):
    if int(level / big_delta) * big_delta == level:
      return str(level)
    else:
      return ""
  def inner_formatter(level):
    if level < rounded_z_big:
      return str(level)
    else:
      return ""
  #plt.clabel(cs, fmt=inner_formatter, fontsize=6, inline=0)
  plt.clabel(cs, fmt=big_formatter, fontsize=10, inline=0)

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
  opt_success, opt_res = benchmark.tasks.ppm_minimize.delay(fname, paranoia, prior,
                                                            depth, optimum[0]).get()
  if opt_success:
    if opt_res.status != 0:
      print('ppm_contour_plot_helper: warning, abnormal termination of minimize, ' +
            'result may be inaccurate: ' + opt_res.message)
    optimum = opt_res.x, opt_res.fun
  else:
    print('ppm_contour_plot_helper: warning, error in ppm_minimize ' +
          '-- falling back to grid search: ' + str(opt_res))

  original_size = corpus_size(fname)
  optimum, evals = grid_to_efficiency(optimum, evals, original_size)

  save_pickle((optimum, evals), test_name, fname)

  fig = contour(optimum, evals, num_levels=int(num_levels), delta=float(delta),
                xlim=beta_range, ylim=alpha_range)
  return save_figure(fig, test_name, fname)
ppm_contour_plot = per_file_test(ppm_contour_plot_helper)

def ppm_find_optimal_alpha_beta(fname, paranoia, prior, depth, granularity, method):
  if verbose:
     print('ppm_find_optimal_alpha_beta: {0} at depth {1} on {2}'.format(prior, depth, fname))
  initial_guess = (0, 0.5) # PPMD

  if granularity >= 1:
    res = ppm_grid_search(fname, paranoia, prior, depth,
                          iterations=1, shrink_factor=1, granularity=granularity,
                          alpha_range=(config.PPM_ALPHA_START, config.PPM_ALPHA_END),
                          beta_range=(config.PPM_BETA_START, config.PPM_BETA_END))
    optimum, evals = res
    initial_guess, _ = optimum

  opt_success, opt_res = benchmark.tasks.ppm_minimize.delay(fname, paranoia, prior,
                                                            depth, initial_guess, method).get()
  if opt_success:
    status = "Normal" if opt_res.status == 0 else opt_res.message
    return (opt_res.x, opt_res.fun, status)
  else:
    print('ppm_find_optimal_alpha_beta: error in ppm_minimize: ' + str(opt_res))

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
  best_status = "Uninitialised"

  for depth, x in res.items():
    if x: # x can be None if optimisation fails
      (alpha, beta), val, status = x
      if val < best_val:
        best_val = val
        best_alpha, best_beta = alpha, beta
        best_depth = depth
        best_status = status

  return (best_depth, best_alpha, best_beta, best_val, best_status)

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
      fieldnames = ['file', 'depth', 'alpha', 'beta', 'compressed_size', 'efficiency', 'status']
      writer.writerow(fieldnames)
      for fname, values in res_by_file.items():
        original_size = corpus_size(fname)
        depth, alpha, beta, size, status = best_parameters_by_depth(values)
        efficiency = size * 8 / original_size
        rel_fname = os.path.relpath(fname, config.CORPUS_DIR)
        writer.writerow([rel_fname, depth, alpha, beta, size, efficiency, status])

  runner = functools.partial(worker_wrapper, ppm_optimal_parameters_helper,
                             paranoia, prior, granularity, method)
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
    fieldnames = ['depth', 'alpha', 'beta', 'compressed_size', 'efficiency', 'status']
    writer = csv.writer(f)
    writer.writerow(fieldnames)

    for d in depths:
      res = ppm_find_optimal_alpha_beta(fname, paranoia, prior, d, granularity, method)
      if res: # optimisation succeeded
        (alpha, beta), compressed_size, status = res
        writer.writerow([d, alpha, beta, compressed_size,
                         compressed_size / original_size * 8, status])
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
  parser.add_argument('--num-workers', dest='num_workers', default=config.NUM_WORKERS,
                      help='number of local processes (default: {0})'.format(config.NUM_WORKERS))
  parser.add_argument('tests', nargs='*',
                      help='list of tests to conduct; format is test_name[:parameter1=value1[:...]]')

  args = vars(parser.parse_args())
  global verbose, paranoia, use_cache
  verbose = args['verbose']
  paranoia = args['paranoia']
  use_cache = not args['rerun']
  num_workers = int(args['num_workers'])

  files = benchmark.general.include_exclude_files(args['include'], args['exclude'])
  files = list(map(lambda fname: os.path.join(config.CORPUS_DIR, fname), files))

  pool = multiprocessing.Pool(num_workers)
  if verbose:
    print("Splitting work across {0} processes".format(num_workers))

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
