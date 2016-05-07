#!/usr/bin/env python3

import argparse, csv, functools, hashlib, itertools
import math, multiprocessing, os, sys, traceback

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
  return fname.replace('/', '_')

def save_figure(fig, output_dir, fnames):
  if len(fnames) == 1:
    fig_fname = sanitize_fname(fnames[0]) + ".pdf"
  else:
    m = hashlib.md5()
    for fname in fnames:
      m.update(fname.encode('utf8'))
    fig_fname = "group-" + m.hexdigest() + ".pdf"
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
      pool.apply_async(worker_wrapper, (runner, fname, ), callback=callback, error_callback=ec)
  return f

def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  return (point - new_width / 2, point + new_width / 2)

def round_up_to(multiple, x):
  return math.ceil(x / multiple) * multiple

def plot_contour_base(xlim, ylim):
  fig = plt.figure()

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

def plot_optimum(optimum, label='OPT', marker='x', offset=(2,2), color='k'):
  # plot optimum
  (optimum_a, optimum_b), optimum_z = optimum
  plt.scatter(optimum_b, optimum_a, marker=marker, color=color)
  plt.annotate(label, xy=(optimum_b, optimum_a), xycoords='data',
               xytext=offset, textcoords='offset points', color=color)

def plot_contour_lines(optimum, evals, big_levels, big_delta, small_per_big,
                       big_formatter, big_manual, inner_formatter, inner_manual, colors=None):
  optimum_z = optimum[1]

  # plot contours
  small_delta = big_delta / small_per_big
  rounded_z_small = round_up_to(small_delta, optimum_z)
  rounded_z_big = round_up_to(big_delta, optimum_z)

  num_inner_levels = int((rounded_z_big - rounded_z_small) / small_delta)
  inner_levels = rounded_z_small + np.arange(1, num_inner_levels)*small_delta
  small_levels = rounded_z_big + np.arange((big_levels - 1) * small_per_big + 1)*small_delta
  big_levels = rounded_z_big + np.arange(big_levels)*big_delta

  small_linewidth, big_linewidth = 0.05, 0.5
  (a, b), z = evals
  if inner_levels:
    inner_cs = plt.contour(b, a, z, levels=inner_levels, linewidths=small_linewidth, colors='k')
  small_cs = plt.contour(b, a, z, levels=small_levels, linewidths=small_linewidth, colors='k')
  big_cs = plt.contour(b, a, z, levels=big_levels, linewidths=big_linewidth, colors=colors)

  # labels
  if inner_levels and inner_formatter:
    plt.clabel(inner_cs, fmt=inner_formatter, manual=inner_manual, fontsize=6, inline=0)
  if big_formatter:
    plt.clabel(big_cs, fmt=big_formatter, manual=big_manual, fontsize=10, inline=0)

def contour_settings(default, overrides, test_name, fnames):
  kwargs = dict(default)
  fnames = frozenset(fnames)
  if fnames in overrides:
    if test_name in overrides[fnames]:
      kwargs.update(overrides[fnames][test_name])
  return kwargs

def ppm_contour_plot_helper(test_name, fnames, prior, depth,
                            granularity=config.PPM_CONTOUR_GRANULARITY,
                            alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                            beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  if type(fnames) == str:
    fnames = [fnames]

  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  depth = int(depth)
  granularity = int(granularity)

  optimum, evals = benchmark.tasks.contour_data(prior, paranoia, depth,
                                                alpha_range, beta_range, granularity, fnames)
  kwargs = contour_settings(config.PPM_CONTOUR_DEFAULT_ARGS,
                            config.PPM_CONTOUR_OVERRIDES, test_name, fnames)

  fig = plot_contour_base(xlim=beta_range, ylim=alpha_range)
  plot_optimum(optimum)
  plot_contour_lines(optimum, evals, **kwargs)
  return save_figure(fig, test_name, fnames)
ppm_contour_plot = per_file_test(ppm_contour_plot_helper)

def ppm_multi_contour_plot(pool, fnames, test_name, prior, depth,
                           granularity=config.PPM_CONTOUR_GRANULARITY,
                           alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                           beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  ppm_contour_plot_helper(test_name, fnames, prior, depth, granularity,
                          alpha_start, alpha_end, beta_start, beta_end)

def short_name(d, k):
  if k in d:
    return d[k]
  else:
    print("WARNING: no abbreviation for '{0}': ".format(k))
    return k

def contour_data_helper(*args, **kwargs):
  # needed as can't pickle benchmark.tasks.contour_data as it's a MemoizedFunction
  return benchmark.tasks.contour_data(*args, **kwargs)

def ppm_group_file_contour_plot(pool, fnames, test_name, prior, depth,
                                granularity=config.PPM_CONTOUR_GRANULARITY,
                                alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                                beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  depth = int(depth)
  granularity = int(granularity)
  kwargs = contour_settings(config.PPM_GROUP_CONTOUR_DEFAULT_ARGS,
                            config.PPM_GROUP_CONTOUR_OVERRIDES, test_name, fnames)

  def callback(res):
    fig = plot_contour_base(xlim=beta_range, ylim=alpha_range)
    colors = kwargs['colormap'](np.linspace(0, 1, len(fnames)))
    del kwargs['colormap']

    for file_res, fname, color in zip(res, fnames, colors):
      print(fname)
      optimum, evals = file_res
      label = short_name(config.SHORT_FILE_NAME, fname)
      plot_optimum(optimum, label=label, color=color)
      plot_contour_lines(optimum, evals, colors=color, **kwargs)

    return save_figure(fig, test_name, fnames)

  runner = functools.partial(contour_data_helper, prior, paranoia, depth,
                             alpha_range, beta_range, granularity)
  work = [[fname] for fname in fnames]
  ec = functools.partial(error_callback,
                         "ppm_group_file_contour_plot - {0} on {1}".format(test_name, fnames))
  pool.map_async(runner, work, chunksize=1, callback=callback, error_callback=ec)

def ppm_group_prior_contour_plot_helper(test_name, fnames, priors, depth,
                                        granularity=config.PPM_CONTOUR_GRANULARITY,
                                        alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                                        beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  depth = int(depth)
  granularity = int(granularity)
  priors = priors.split(",")
  kwargs = contour_settings(config.PPM_GROUP_CONTOUR_DEFAULT_ARGS,
                            config.PPM_GROUP_CONTOUR_OVERRIDES, test_name, fnames)

  fig = plot_contour_base(xlim=beta_range, ylim=alpha_range)
  colors = kwargs['colormap'](np.linspace(0, 1, len(priors)))
  for prior, color in zip(priors, colors):
    optimum, evals = benchmark.tasks.contour_data(prior, paranoia, depth,
                                                  alpha_range, beta_range, granularity, fnames)
    label = short_name(config.SHORT_PRIOR_NAME, prior)
    plot_optimum(optimum, label=label, color=color)
    plot_contour_lines(optimum, evals, color=color, **kwargs)

  return save_figure(fig, test_name, fnames)
ppm_group_prior_contour_plot = per_file_test(ppm_group_prior_contour_plot_helper)

def ppm_multi_group_prior_contour_plot(pool, fnames, test_name, priors, depth,
                                 granularity=config.PPM_CONTOUR_GRANULARITY,
                                 alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                                 beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  ppm_group_prior_contour_plot(test_name, fnames, priors, depth, granularity,
                               alpha_start, alpha_end, beta_start, beta_end)

def ppm_find_optimal_alpha_beta(fnames, paranoia, prior, granularity, depth):
  if verbose:
     print('ppm_find_optimal_alpha_beta: {0} at depth {1} on {2}'.format(prior, depth, fnames))
  initial_guess = (0, 0.5) # PPMD

  if granularity > 1:
    res = benchmark.tasks.optimise_brute(fnames, paranoia, prior, depth,
                                         (config.PPM_ALPHA_START, config.PPM_ALPHA_END),
                                         (config.PPM_BETA_START, config.PPM_BETA_END),
                                         granularity)
    optimum, evals = res
    initial_guess, _ = optimum

  opt_success, opt_res = benchmark.tasks.ppm_minimize(fnames, paranoia, prior, depth, initial_guess)
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

def ppm_optimal_parameters_helper(paranoia, prior, granularity, x):
  fname, depth = x
  return ppm_find_optimal_alpha_beta([fname], paranoia, prior, granularity, depth)

def ppm_optimal_parameters(pool, files, test_name, prior,
                           granularity=config.PPM_PARAMETER_GRANULARITY,
                           depths=config.PPM_PARAMETER_DEPTHS):
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
      fieldnames = ['file', 'depth', 'alpha', 'beta', 'efficiency', 'status']
      writer.writerow(fieldnames)
      for fname, values in res_by_file.items():
        depth, alpha, beta, efficiency, status = best_parameters_by_depth(values)
        writer.writerow([fname, depth, alpha, beta, efficiency, status])

  runner = functools.partial(worker_wrapper, ppm_optimal_parameters_helper,
                             paranoia, prior, granularity)
  ec = functools.partial(error_callback, 'ppm_optimal_parameters - {0} on {1}'
                                         .format(test_name, files))
  pool.map_async(runner, work, chunksize=1, callback=callback, error_callback=ec)

def ppm_optimal_alpha_beta_helper(test_name, fname, prior,
                                  depths=config.PPM_PARAMETER_DEPTHS,
                                  granularity=config.PPM_PARAMETER_GRANULARITY):
  depths = parse_depths(depths)
  granularity = int(granularity)

  csv_dir = os.path.join(config.TABLE_DIR, test_name)
  os.makedirs(csv_dir, exist_ok=True)
  csv_path = os.path.join(csv_dir, sanitize_fname(fname) + '.csv')
  with open(csv_path, 'w') as f:
    fieldnames = ['depth', 'alpha', 'beta', 'efficiency', 'status']
    writer = csv.writer(f)
    writer.writerow(fieldnames)

    for d in depths:
      res = ppm_find_optimal_alpha_beta([fname], paranoia, prior, granularity, d)
      if res: # optimisation succeeded
        (alpha, beta), efficiency, status = res
        writer.writerow([d, alpha, beta, efficiency, status])
    return csv_path
ppm_optimal_alpha_beta = per_file_test(ppm_optimal_alpha_beta_helper)

def ppm_multi_optimal_alpha_beta(pool, files, test_name, prior,
                                 granularity=config.PPM_PARAMETER_GRANULARITY,
                                 depths=config.PPM_PARAMETER_DEPTHS):
  depths = parse_depths(depths)
  granularity = int(granularity)

  def callback(res):
    csv_path = os.path.join(config.TABLE_DIR, test_name + '.csv')
    with open(csv_path, 'w') as f:
      fieldnames = ['depth', 'alpha', 'beta', 'mean_efficiency', 'status']
      writer = csv.writer(f)
      writer.writerow(fieldnames)

      for depth, row in zip(depths, res):
        (alpha, beta), efficiency, status = row
        writer.writerow([depth, alpha, beta, efficiency, status])

  runner = functools.partial(ppm_find_optimal_alpha_beta, files, paranoia, prior, granularity)
  ec = functools.partial(error_callback, 'ppm_multi_optimal_alpha_beta - {0} on {1}'
                                          .format(test_name, files))
  pool.map_async(runner, depths, chunksize=1, callback=callback, error_callback=ec)

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
  'ppm_multi_contour_plot': ppm_multi_contour_plot,
  'ppm_group_file_contour_plot': ppm_group_file_contour_plot,
  'ppm_group_prior_contour_plot': ppm_group_prior_contour_plot,
  'ppm_multi_group_prior_contour_plot': ppm_multi_group_prior_contour_plot,
  'ppm_optimal_parameters': ppm_optimal_parameters,
  'ppm_optimal_alpha_beta': ppm_optimal_alpha_beta,
  'ppm_multi_optimal_alpha_beta': ppm_multi_optimal_alpha_beta,
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
