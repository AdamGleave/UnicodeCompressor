#!/usr/bin/env python3

# Copyright (C) 2016, Adam Gleave
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import argparse, csv, functools, itertools
import math, multiprocessing, os, sys, traceback

import celery

import numpy as np

import matplotlib
matplotlib.use('PDF')
import prettyplotlib as ppl
import matplotlib.pyplot as plt

import benchmark.general as general
import benchmark.plot as plot
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

def per_file_test(test):
  def f(pool, files, test_name, **kwargs):
    runner = functools.partial(test, test_name, **kwargs)
    for fname in files:
      def callback(res):
        print("FINISHED: {0}: {1}".format(test_name, res))
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
  fig = plot.new_figure()

  # shade illegal parameter region, a + b <= 0
  min_x, max_x = xlim
  min_y, max_y = ylim
  x_vertices = [min_x, min_x, max_x]
  lowest = min(min_y, -max_x)
  y_vertices = [-min_x, lowest, lowest]
  plt.fill(x_vertices, y_vertices, 'gray', alpha=0.3)
  plt.plot([min_x, max_x], [-min_x, lowest], color='k')

  #ppl.fill_between([min_x, max_x], [lowest, lowest], [-min_x, lowest], color='gray', alpha=0.2)

  # turn grid on
  plt.grid(b=True)

  # xticks
  plt.xticks(np.arange(0, 1.1, 0.1))

  # axes
  plt.xlim(xlim)
  plt.ylim(ylim)

  plt.xlabel(r'discount $\beta$')
  plt.ylabel(r'strength $\alpha$')

  return fig

def plot_optimum(optimum, label, marker, size=8, color='k', offset=(2,2),
                 horizontalalignment='left', verticalalignment='bottom'):
  # plot optimum
  optimum_a, optimum_b = optimum
  plt.scatter(optimum_b, optimum_a, marker=marker, color=color)
  plt.annotate(label, xy=(optimum_b, optimum_a), xycoords='data', xytext=offset,
               textcoords='offset points', ha=horizontalalignment, va=verticalalignment,
               size=size, color=color)

def plot_contour_labels(contour, fmt, manual, **kwargs):
  labels = plt.clabel(contour, fmt=fmt, manual=manual, **kwargs)
  if manual == True:
    # print coordinates of manually placed label, so can put them in config for posterity
    coords = [(label._x, label._y) for label in labels]
    print(coords)

def plot_contour_lines(optimum, evals, big_levels, big_delta, small_per_big,
                       big_formatter, big_manual, inner_formatter, inner_manual,
                       big_linewidth, small_linewidth, colors=None):
  if not colors:
    colors = ppl.brewer2mpl.get_map('Spectral', 'diverging', big_levels).mpl_colors

  optimum_z = optimum[1]

  # plot contours
  small_delta = big_delta / small_per_big
  rounded_z_small = round_up_to(small_delta, optimum_z)
  rounded_z_big = round_up_to(big_delta, optimum_z)

  num_inner_levels = int((rounded_z_big - rounded_z_small) / small_delta)
  inner_levels = rounded_z_small + np.arange(1, num_inner_levels)*small_delta
  small_levels = rounded_z_big + np.arange((big_levels - 1) * small_per_big + 1)*small_delta
  big_levels = rounded_z_big + np.arange(big_levels)*big_delta

  (a, b), z = evals
  if len(inner_levels):
    inner_colors = [colors[0]] * len(inner_levels)
    inner_cs = plt.contour(b, a, z, levels=inner_levels, linewidths=small_linewidth,
                           colors=inner_colors)
  small_colors = np.repeat(colors[1:], small_per_big, axis=0)
  # set linewidth to 0 when overlapping with big contours
  small_linewidths = [0] + [small_linewidth] * (small_per_big - 1)
  plt.contour(b, a, z, levels=small_levels, linewidths=small_linewidths, colors=small_colors)
  big_cs = plt.contour(b, a, z, levels=big_levels, linewidths=big_linewidth, colors=colors)

  # labels
  if len(inner_levels) and inner_formatter:
    plot_contour_labels(inner_cs, fmt=inner_formatter, manual=inner_manual, fontsize=6,
                        inline=1, inline_spacing=2)
  if big_formatter:
    plot_contour_labels(big_cs, fmt=big_formatter, manual=big_manual, fontsize=8,
                        inline=1, inline_spacing=3)

def contour_settings(default, overrides, test_name, fnames):
  kwargs = dict(default)
  fnames = frozenset(fnames)
  if fnames in overrides:
    if test_name in overrides[fnames]:
      to_update = overrides[fnames][test_name]
      for k, v in to_update.items():
        kwargs[k].update(v)
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
  settings = contour_settings(config.PPM_CONTOUR_DEFAULT_ARGS,
                            config.PPM_CONTOUR_OVERRIDES, test_name, fnames)

  fig = plot_contour_base(xlim=beta_range, ylim=alpha_range)
  plot_optimum(optimum[0], **settings['optimum'])
  for kwargs in settings['markers'].values():
    plot_optimum(**kwargs)
  plot_contour_lines(optimum, evals, **settings['lines'])
  return plot.save_figure(fig, test_name, fnames)
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
      optimum, evals = file_res
      label = short_name(config.SHORT_FILE_NAME, fname)
      plot_optimum(optimum, label=label, color=color)
      plot_contour_lines(optimum, evals, colors=color, **kwargs)

    return plot.save_figure(fig, test_name, fnames)

  runner = functools.partial(contour_data_helper, prior, paranoia, depth,
                             alpha_range, beta_range, granularity)
  work = [[fname] for fname in fnames]
  ec = functools.partial(error_callback,
                         "ppm_group_file_contour_plot - {0} on {1}".format(test_name, fnames))
  pool.map_async(runner, work, chunksize=1, callback=callback, error_callback=ec)

def ppm_group_algo_contour_plot_helper(test_name, fnames, algos,
                                       granularity=config.PPM_CONTOUR_GRANULARITY,
                                       alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                                       beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  if type(fnames) == str:
    fnames = [fnames]
  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  granularity = int(granularity)
  algos = algos.split(",")
  kwargs = contour_settings(config.PPM_GROUP_CONTOUR_DEFAULT_ARGS,
                            config.PPM_GROUP_CONTOUR_OVERRIDES, test_name, fnames)

  fig = plot_contour_base(xlim=beta_range, ylim=alpha_range)
  colors = kwargs['colormap'](np.linspace(0, 1, len(algos)))
  del kwargs['colormap']
  for algo, color in zip(algos, colors):
    prior, depth = algo.split("/")
    depth = int(depth)
    optimum, evals = benchmark.tasks.contour_data(prior, paranoia, depth,
                                                  alpha_range, beta_range, granularity, fnames)
    label = short_name(config.SHORT_PRIOR_NAME, prior)
    plot_optimum(optimum, label=label, color=color)
    plot_contour_lines(optimum, evals, colors=color, **kwargs)

  return plot.save_figure(fig, general.sanitize_fname(test_name), fnames)
ppm_group_algo_contour_plot = per_file_test(ppm_group_algo_contour_plot_helper)

def ppm_multi_group_algo_contour_plot(pool, fnames, test_name, algos,
                                      granularity=config.PPM_CONTOUR_GRANULARITY,
                                      alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                                      beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END):
  ppm_group_algo_contour_plot_helper(test_name, fnames, algos, granularity,
                                     alpha_start, alpha_end, beta_start, beta_end)

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
  return benchmark.tasks.ppm_find_optimal_alpha_beta([fname], paranoia, prior, depth, granularity,
                                                     config.PPM_ALPHA_RANGE, config.PPM_BETA_RANGE)

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
  csv_path = os.path.join(csv_dir, general.sanitize_fname(fname) + '.csv')
  with open(csv_path, 'w') as f:
    fieldnames = ['depth', 'alpha', 'beta', 'efficiency', 'status']
    writer = csv.writer(f)
    writer.writerow(fieldnames)

    for d in depths:
      res = benchmark.tasks.ppm_find_optimal_alpha_beta([fname], paranoia, prior, d, granularity,
                                                        config.PPM_ALPHA_RANGE, config.PPM_BETA_RANGE)
      if res: # optimisation succeeded
        (alpha, beta), efficiency, status = res
        writer.writerow([d, alpha, beta, efficiency, status])
    return csv_path
ppm_optimal_alpha_beta = per_file_test(ppm_optimal_alpha_beta_helper)

def ppm_multi_optimal_alpha_beta_helper(files, prior, granularity, depth):
  return benchmark.tasks.ppm_find_optimal_alpha_beta(files, paranoia, prior, depth, granularity,
                                                     config.PPM_ALPHA_RANGE, config.PPM_BETA_RANGE)
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

  ec = functools.partial(error_callback, 'ppm_multi_optimal_alpha_beta - {0} on {1}'
                                          .format(test_name, files))
  runner = functools.partial(ppm_multi_optimal_alpha_beta_helper, files, prior, granularity)
  pool.map_async(runner, depths, chunksize=1, callback=callback, error_callback=ec)

def ppm_efficiency_by_depth_helper(fnames, granularity, x):
  prior, depth = x

  return benchmark.tasks.ppm_find_optimal_alpha_beta(fnames, paranoia, prior, depth, granularity,
                                                     config.PPM_ALPHA_RANGE, config.PPM_BETA_RANGE)
def ppm_efficiency_by_depth_helper2(fnames, priors, depths, test_name, opts):
  test_files = list(itertools.chain(*config.PPM_EFFICIENCY_BY_DEPTH_FILESETS.values()))
  original_sizes = {f : benchmark.tasks.corpus_size(f) for f in fnames}
  work = []
  for opt, (prior, depth) in zip(opts, itertools.product(priors, depths)):
    x, fun, status = opt
    a, b = x
    work += [benchmark.tasks.my_compressor.s(test_file, paranoia, prior,
                                             ['ppm:d={0}:a={1}:b={2}'.format(depth, a, b)])
            for test_file in test_files]
  raw_res = celery.group(work)().get()

  res = {}
  for effectiveness, (prior, depth, test_file) in zip(raw_res,
                                                      itertools.product(priors, depths, test_files)):
    by_prior = res.get(prior, {})
    by_depth = by_prior.get(depth, {})
    by_depth[test_file] = effectiveness
    by_prior[depth] = by_depth
    res[prior] = by_prior

  fig = plot.new_figure()
  colors = ppl.brewer2mpl.get_map('Set2', 'qualitative', len(config.PPM_EFFICIENCY_BY_DEPTH_FILESETS)).mpl_colors
  for (name, fileset), color in zip(config.PPM_EFFICIENCY_BY_DEPTH_FILESETS.items(), colors):
    for prior in priors:
      y = []
      for d in depths:
        by_file = res[prior][d]
        mean = np.mean([by_file[f] / original_sizes[f] * 8 for f in fileset])
        y.append(mean)

      linestyle = config.PPM_EFFICIENCY_BY_DEPTH_PRIOR_LINESTYLES[prior]
      marker = config.PPM_EFFICIENCY_BY_DEPTH_PRIOR_MARKERS[prior]
      min_i = np.argmin(y)
      markevery = list(range(0, min_i)) + list(range(min_i + 1, len(depths)))
      ppl.plot(depths, y, label='{1} on {0}'.format(name, short_name(config.SHORT_PRIOR_NAME, prior)),
               color=color, linestyle=linestyle, marker=marker, markevery=markevery)

      min_depth = depths[min_i]
      min_y = y[min_i]
      ppl.plot([min_depth], [min_y], color=color, linestyle='None', marker='D')

  plt.xlabel(r'Maximal context depth $d$')
  plt.ylabel(r'Compression effectiveness (bits/byte)')

  # stretch x-axis slightly so markers are visible
  plt.xlim(min(depths) - 0.1, max(depths) + 0.1)

  ppl.legend(handlelength=4, # increase length of line segments so that linestyles can be seen
             numpoints=1 # but only show marker once
             )

  return plot.save_figure(fig, test_name, ["dummy"])

def ppm_efficiency_by_depth(pool, files, test_name, priors,
                            granularity=config.PPM_PARAMETER_GRANULARITY,
                            depths=config.PPM_PARAMETER_DEPTHS):
  granularity = int(granularity)
  depths = parse_depths(depths)
  priors = priors.split(",")

  def callback(opts):
    # TODO: this will block the thread
    worker_wrapper(ppm_efficiency_by_depth_helper2, files, priors, depths, test_name, opts)

  runner = functools.partial(worker_wrapper, ppm_efficiency_by_depth_helper, files, granularity)
  ec = functools.partial(error_callback, 'ppm_efficiency_by_depth - {0} on {1}'
                                         .format(test_name, files))
  work = itertools.product(priors, depths)
  pool.map_async(runner, work, chunksize=1, callback=callback, error_callback=ec)

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
  'ppm_group_algo_contour_plot': ppm_group_algo_contour_plot,
  'ppm_multi_group_algo_contour_plot': ppm_multi_group_algo_contour_plot,
  'ppm_optimal_parameters': ppm_optimal_parameters,
  'ppm_optimal_alpha_beta': ppm_optimal_alpha_beta,
  'ppm_multi_optimal_alpha_beta': ppm_multi_optimal_alpha_beta,
  'ppm_efficiency_by_depth': ppm_efficiency_by_depth,
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
  parser.add_argument('--style', dest='style')
  parser.add_argument('tests', nargs='*',
                      help='list of tests to conduct; format is test_name[:parameter1=value1[:...]]')

  args = vars(parser.parse_args())
  global verbose, paranoia, use_cache
  verbose = args['verbose']
  paranoia = args['paranoia']
  use_cache = not args['rerun']
  num_workers = int(args['num_workers'])
  if args['style']:
    plot.set_style(args['style'])

  files = general.include_exclude_files(args['include'], args['exclude'])
  if verbose:
    print("Operating on: {0}".format(files))

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
