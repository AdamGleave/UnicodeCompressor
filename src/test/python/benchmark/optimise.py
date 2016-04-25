#!/usr/bin/env python3.4

import filecmp, functools, os, sys, tempfile
from multiprocessing import Pool

import numpy as np
from scipy import optimize
import matplotlib.pyplot as plt

import general
from mode import CompressionMode
import config_optimise as config

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

  original_size = os.path.getsize(original_fname)
  compressed_size = os.path.getsize(compressed_fname)
  os.unlink(compressed_fname)

  return compressed_size / original_size * 8

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

def grid_search(compressor, fname, n,
                alpha_range=config.PPM_ALPHA_RANGE,
                beta_range=config.PPM_BETA_RANGE,
                Ns=config.PPM_GRID_GRANULARITY):
  def helper(n, alpha_range, beta_range):
    finish = None
    if n <= 1:
      # on finest grid, use Nelder-Mead to find optimum
      finish = optimize.fmin
    res = optimize.brute(func=efficiency,
                         args=(compressor, fname),
                         ranges=(alpha_range, beta_range),
                         Ns=Ns,
                         full_output=True,
                         finish=finish,
                         disp=verbose)

    if n <= 1:
      return (res[0:2], [res[2:]])
    else:
      argmin = res[0]
      new_a_range = range_around(argmin[0], alpha_range, 2)
      new_b_range = range_around(argmin[1], beta_range, 2)
      optimum, evals = helper(n - 1, new_a_range, new_b_range)

      return (optimum, [res[2:]] + evals)
  return helper(n, alpha_range, beta_range)

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

def contour(grid_res, delta=config.CONTOUR_DELTA, num_levels=config.CONTOUR_NUM_LEVELS):
  # process data
  optimum, evals = grid_res

  plt.figure()
  # plot optimum
  (optimum_a, optimum_b), optimum_z = optimum
  plt.scatter(optimum_b, optimum_a, marker='x')
  plt.annotate('OPT', xy=(optimum_b, optimum_a), xycoords='data',
               xytext=(2,2), textcoords='offset points')

  # plot contours
  levels = optimum_z + np.arange(1, num_levels + 1)*0.1
  (a, b), z = evals[0]
  plt.contour(b, a, z, levels=levels, linewidths=1)
  # levels = optimum_z + np.arange(1, num_levels + 1)*0.01
  # (a, b), z = evals[1]
  # plt.contour(b, a, z, levels=levels, linewidths=0.2, )

  # shade illegal parameter region, a + b <= 0
  min_x, max_x = config.CONTOUR_XLIM
  min_y, max_y = config.CONTOUR_YLIM
  x_vertices = [min_x, min_x, max_x]
  lowest = min(min_y, -max_x)
  y_vertices = [-min_x, lowest, lowest]
  plt.fill(x_vertices, y_vertices, 'gray', alpha=0.5)

  # axes
  plt.xlim(config.CONTOUR_XLIM)
  plt.ylim(config.CONTOUR_YLIM)

  plt.xlabel(r'discount $\beta$')
  plt.ylabel(r'strength $\alpha$')

  plt.show()

verbose=True
paranoia=True

if __name__ == "__main__":
  description = "Produce visualisations and find optimal parameters of compression algorithms"
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('--paranoia', dest='paranoia', action='store_true',
                      help='verify correct operation of compression algorithms by decompressing ' +
                           'their output and comparing to the original file.')
  parser.add_argument('--threads', dest='threads', type=int, default=config.NUM_THREADS,
                      help='number of compression algorithms to run concurrently ' +
                           '(default: {0}).'.format(config.NUM_THREADS))
  parser.add_argument('--include', dest='include', nargs='+',
                      help='paths which match the specified regex are included; ' +
                           'if unspecified, defaults to *.')
  parser.add_argument('--exclude', dest='exclude', nargs='+',
                      help='paths which match the specified regex are excluded.')

  args = vars(parser.parse_args())
  verbose = args['verbose']
  num_threads = args['threads']
  paranoia = args['paranoia']

  files = general.include_exclude_files(args['include'], args['exclude'])

  with Pool(num_threads) as p:
    if verbose:
      print("Splitting work across {0} processes".format(num_threads))

  # TBC
