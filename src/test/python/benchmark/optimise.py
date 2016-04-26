#!/usr/bin/env python3.4

import argparse, filecmp, functools, os, sys, tempfile
from multiprocessing import Pool

import numpy as np
from scipy import optimize

import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

from joblib import Memory

import general
from mode import CompressionMode
import config_optimise as config

memory = Memory(cachedir=config.CACHE_DIR, verbose=0)
use_cache = True

def maybe_cache(*args, **kwargs):
  memoized = memory.cache(*args, **kwargs)
  def wrapper(*args, **kwargs):
    print("wrapper: " + str(args) + " / " + str(kwargs))
    if use_cache:
      memoized(*args, **kwargs)
    else:
      # still persist result to cache, but don't read from it
      memoized.call(*args, **kwargs)
  return wrapper

def ppm(prior, d, a, b):
  assert(int(d) == d)

  if a + b <= 0.01:
    # a + b > 0 to be legal; make it 0.01 to ensure numerical stability
    return None
  else:
    algo_config = ['ppm:d={0}:a={1}:b={2}'.format(int(d),a,b)]
    #TODO: nasty this is calling config, factor out?
    return config.my_compressor(prior, algo_config)

BUFSIZE = 1024 * 1024 # 1 MB
def efficiency(params, compressor, original_fname):
  c = compressor(*params)
  if verbose:
    print(params)
    sys.stdout.flush()
  if not c:
    return float('inf')

  compressed_file, compressed_fname = tempfile.mkstemp(suffix='compression_optimise_en')
  with open(original_fname, 'r') as original:
    c(original, compressed_file, CompressionMode.compress)
  if paranoia:
    decompressed_file, decompressed_fname = tempfile.mkstemp(suffix='compression_optimised_de')
    with open(compressed_fname, 'r') as compressed:
      c(compressed, decompressed_file, CompressionMode.decompress)
    if not filecmp.cmp(original_fname, decompressed_fname):
      print("WARNING: decompressed file differs from original, with compressor " +
            str(compressor) + " under parameters " + str(params))
      return float('inf')
    os.unlink(decompressed_fname)
    os.close(decompressed_file)

  original_size = os.path.getsize(original_fname)
  compressed_size = os.path.getsize(compressed_fname)
  os.unlink(compressed_fname)
  os.close(compressed_file)

  return compressed_size / original_size * 8

@maybe_cache
def optimal_alpha_beta(compressor, in_fname):
  '''compressor(a,b) should be a function taking two floating-point values
     and returning a floating point value to minimise.'''
  # SOMEDAY: try other optimisation methods, e.g. Powell?
  # SOMEDAY: strictly the optimisation should be bounded, a + b >= 0. But optimiser is unlikely to
  # explore this space if given a sensible initial guess.
  initial_guess = (0, 0.5) # PPMD
  return optimize.minimize(fun=efficiency,
                           args=(compressor, in_fname),
                           x0=initial_guess,
                           method='Nelder-Mead',
                           options={'disp': verbose})

def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  return (point - new_width / 2, point + new_width / 2)

@maybe_cache
def grid_search(compressor, fname, iterations, shrink_factor, alpha_range, beta_range, Ns):
  def helper(iterations, alpha_range, beta_range):
    finish = None
    if iterations <= 1:
      # on finest grid, use Nelder-Mead to find optimum
      finish = optimize.fmin
    res = optimize.brute(func=efficiency,
                         args=(compressor, fname),
                         ranges=(alpha_range, beta_range),
                         Ns=Ns,
                         full_output=True,
                         finish=finish,
                         disp=verbose)

    if iterations <= 1:
      return (res[0:2], [res[2:]])
    else:
      argmin = res[0]
      new_a_range = range_around(argmin[0], alpha_range, shrink_factor)
      new_b_range = range_around(argmin[1], beta_range, shrink_factor)
      optimum, evals = helper(iterations - 1, new_a_range, new_b_range)

      return (optimum, [res[2:]] + evals)
  return helper(iterations, alpha_range, beta_range)

def combine_evals(evals):
  acc_x = np.zeros((0,))
  acc_y = np.zeros((0,))
  acc_z = np.zeros((0,))

  for eval in evals:
    (x, y), z = eval

    acc_x = np.concatenate((acc_x, np.ndarray.flatten(x)))
    acc_y = np.concatenate((acc_y, np.ndarray.flatten(y)))
    acc_z = np.concatenate((acc_z, np.ndarray.flatten(z)))

    M = np.array([acc_x, acc_y, acc_z])
    M = M[:, M[2,:] != float('inf')] # filter out illegal values
    return (M[0, :], M[1, :], M[2, :])

def contour(grid_res, delta, num_levels, xlim, ylim):
  # process data
  optimum, evals = grid_res

  fig = plt.figure()
  # plot optimum
  (optimum_a, optimum_b), optimum_z = optimum
  plt.scatter(optimum_b, optimum_a, marker='x')
  plt.annotate('OPT', xy=(optimum_b, optimum_a), xycoords='data',
               xytext=(2,2), textcoords='offset points')

  # plot contours
  levels = optimum_z + np.arange(1, num_levels + 1)*delta
  (a, b), z = evals[0]
  plt.contour(b, a, z, levels=levels, linewidths=1)
  # levels = optimum_z + np.arange(1, num_levels + 1)*0.01
  # (a, b), z = evals[1]
  # plt.contour(b, a, z, levels=levels, linewidths=0.2, )

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

def save_figure_wrapper(output_dir, test, fname, **kwargs):
  fig = test(fname, **kwargs)

  fig_fname = os.path.relpath(fname, config.CORPUS_DIR)
  fig_fname = fig_fname.replace('/', '_') + ".pdf"

  fig_dir = os.path.join(config.FIGURE_DIR, output_dir)
  os.makedirs(fig_dir, exist_ok=True)

  fig_path = os.path.join(fig_dir, fig_fname)
  with PdfPages(fig_path) as out:
    if verbose:
      print("Writing figure to " + fig_path)
    out.savefig(fig)
  return fig_path

# SOMEDAY: Can't seem to use as a decorator, limitation of multiprocessing perhaps?
def per_file_test(test):
  def f(pool, files, test_name, **kwargs):
    runner = functools.partial(save_figure_wrapper, test_name, test, **kwargs)
    return pool.map_async(runner, files)
  return f

def ppm_contour_plot_helper(fname, prior, d, granularity=config.PPM_CONTOUR_GRANULARITY,
                            alpha_start=config.PPM_ALPHA_START, alpha_end=config.PPM_ALPHA_END,
                            beta_start=config.PPM_BETA_START, beta_end=config.PPM_BETA_END,
                            iterations=config.PPM_CONTOUR_NUM_ITERATIONS,
                            shrink_factor=config.PPM_CONTOUR_SHRINK_FACTOR,
                            num_levels=config.PPM_CONTOUR_NUM_LEVELS,
                            delta=config.PPM_CONTOUR_DELTA):
  compressor = functools.partial(ppm, prior, int(d))
  alpha_range = (float(alpha_start), float(alpha_end))
  beta_range = (float(beta_start), float(beta_end))
  grid = grid_search(compressor, fname, int(iterations), int(shrink_factor),
                     alpha_range, beta_range, granularity)
  return contour(grid, num_levels=int(num_levels), delta=float(delta),
                 xlim=beta_range, ylim=alpha_range)
ppm_contour_plot = per_file_test(ppm_contour_plot_helper)

def ppm_optimal_parameters(p, files, prior, granularity=config.PPM_PARAMETER_GRANULARITY):
  # TODO
  pass

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

verbose=False
paranoia=True # TODO: disable by default for speed

TESTS = {
  'ppm_contour_plot': ppm_contour_plot,
  'ppm_optimal_parameters': ppm_optimal_parameters,
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
  parser.add_argument('--threads', dest='threads', type=int, default=config.NUM_THREADS,
                      help='number of compression algorithms to run concurrently ' +
                           '(default: {0}).'.format(config.NUM_THREADS))
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

  files = general.include_exclude_files(args['include'], args['exclude'])
  files = list(map(lambda fname: os.path.join(config.CORPUS_DIR, fname), files))

  pool = Pool(args['threads'])
  if verbose:
    print("Splitting work across {0} processes".format(args['threads']))

  res = {}
  if not args['tests']:
    print("WARNING: no tests specified", file=sys.stderr)
  for test in args['tests']:
    if not test: # empty string
      print("ERROR: test name cannot be an empty string", file=sys.stderr)
      continue
    test_name, *test_args = test.split(":")
    test_kwargs = to_kwargs(test_args)

    if test_name in TESTS:
      test_runner = TESTS[test_name]
      if verbose:
        print("Running " + test_name + " with parameters " + str(test_kwargs))
      res[test] = test_runner(pool, files, test, **test_kwargs)
    else:
      print("ERROR: unrecognised test '" + test_name + "'")

  pool.close()
  pool.join()

if __name__ == "__main__":
  main()

# TODO: cache data