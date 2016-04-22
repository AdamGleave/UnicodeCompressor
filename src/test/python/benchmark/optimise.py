#!/usr/bin/env python3.4

import functools, os, sys

import numpy as np
from scipy import optimize
import matplotlib
import matplotlib.pyplot as plt

from mode import CompressionMode
import config_optimise as config

BUFSIZE = 1024 * 1024 # 1 MB
def efficiency(params, compressor, in_fname):
  original_size = os.path.getsize(in_fname)

  c = compressor(*params)
  if verbose:
    print(params)
    sys.stdout.flush()
  if c:
    with open(in_fname, 'r') as f:
      compressed = c(f, None, CompressionMode.compress)
      return len(compressed) / original_size * 8
  else:
    return float('inf')


def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  return (point - new_width / 2, point + new_width / 2)

def grid_search(compressor, fname, n, a_range, b_range, Ns):
  def helper(n, a_range, b_range):
    res = optimize.brute(func=efficiency,
                         args=(compressor, fname),
                         ranges=(a_range, b_range), Ns=Ns,
                         full_output=True,
                         finish=None,
                         disp=verbose)

    if n <= 1:
      return (res[0:2], [res[2:]])
    else:
      argmin = res[0]
      new_a_range = range_around(argmin[0], a_range, 4)
      new_b_range = range_around(argmin[1], b_range, 4)
      optimum, evals = helper(n - 1, new_a_range, new_b_range)

      return (optimum, [res[2:]] + evals)
  return helper(n, a_range, b_range)

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
  a, b, z = combine_evals(evals)
  #(a, b), z = evals[0]

  plt.figure()
  # plot optimum
  # TODO: this isn't really the optimum. Could use finish to find it, perhaps?
  optimum_i = np.argmin(z)
  optimum_a, optimum_b, optimum_z = a.flat[optimum_i], b.flat[optimum_i], z.flat[optimum_i]
  plt.scatter(optimum_b, optimum_a, marker='x')
  plt.annotate('OPT', xy=(optimum_b, optimum_a), xycoords='data',
               xytext=(2,2), textcoords='offset points')

  # plot contours
  levels = optimum_z + np.arange(1, num_levels + 1)*delta
  #TODO: tricontour is noisier than contour, but more flexible. Anyway to get best of both?
  plt.tricontour(b, a, z, levels=levels)

  # shade illegal parameter region, a + b <= 0
  b_vertices = [np.min(b), np.min(b), np.max(b)]
  lowest = min(np.min(a), -np.max(b))
  a_vertices = [-np.min(b), lowest, lowest]
  plt.fill(b_vertices, a_vertices, 'gray', alpha=0.5)

  # axes
  plt.xlim(config.CONTOUR_XLIM)
  plt.ylim(config.CONTOUR_YLIM)

  plt.xlabel(r'discount $\beta$')
  plt.ylabel(r'strength $\alpha$')


  plt.show()

def optimal_alpha_beta(compressor, in_fname):
  '''compressor(a,b) should be a function taking two floating-point values
     and returning a floating point value to minimise.'''
  # TODO: try other methods, e.g. Powell?
  # TODO: impose bounds? Not all parameters are valid. But it's unlikely to end up searching that space...
  initial_guess = (0, 0.5) # PPMD
  return optimize.minimize(fun=efficiency,
                           args=(compressor, in_fname),
                           x0=initial_guess,
                           method='Nelder-Mead',
                           options={'disp': verbose})

def optimal_alpha_beta_grid(compressor, fname):
  '''compressor(a,b) should be a function taking two floating-point values
     and returning a floating point value to minimise.
     Coarse grid search, followed by optimisation to 'polish'.'''
  # TODO: try other methods, e.g. Powell?
  # TODO: impose bounds? Not all parameters are valid. But it's unlikely to end up searching that space...
  res = optimize.brute(func=efficiency,
                       args=(compressor, fname),
                       ranges=(config.PPM_ALPHA_RANGE, config.PPM_BETA_RANGE),
                       Ns=config.PPM_GRID_GRANULARITY,
                       finish=optimize.fmin,
                       disp=verbose)
  return res

verbose = True

# TODO: what output should it produce?
if __name__ == "__main__":
  # TODO: specify file with regexp as in benchmark
  fname = os.path.join(config.CORPUS_DIR,  'canterbury/canterbury/alice29.txt') #'unit_tests/quickbrown.txt'
  ppm5ub = functools.partial(config.ppm, 'uniform_byte', 5)
  res = optimal_alpha_beta_grid(ppm5ub, fname)
  print(res)