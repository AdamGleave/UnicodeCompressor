#!/usr/bin/env python3.4

import functools, os

import numpy as np
from scipy import optimize

from mode import CompressionMode
import config_optimise as config

BUFSIZE = 1024 * 1024 # 1 MB
def filesize(params, compressor, in_fname):
  c = compressor(*params)
  if c:
    with open(in_fname, 'r') as f:
      r,w = os.pipe()
      c(f, w, CompressionMode.compress)
    os.close(w)

    bytes_read = 0
    while True:
      x = os.read(r, BUFSIZE)
      if len(x) == 0:
        break
      bytes_read += len(x)
    return bytes_read
  else:
    return float('inf')

def grid_search(testcase, fname):
  compressor, params = testcase
  d_range, a_range, b_range, Ns = params
  return optimize.brute(func=filesize,
                        args=(compressor, fname),
                        ranges=(d_range, a_range, b_range), Ns=Ns,
                        full_output=True,
                        finish=None,
                        disp=True)

def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  return (point - new_width / 2, point + new_width / 2)

def n_grid_search(n, testcase, fname):
  compressor, params = testcase
  d_range, a_range, b_range, Ns = params

  def helper(compressor, n, a_range, b_range):
    res = None
    with open(fname, 'r') as f:
      res = optimize.brute(func=filesize,
                           args=(compressor, fname),
                           ranges=(a_range, b_range), Ns=Ns,
                           full_output=True,
                           finish=None,
                           disp=True)

    if n == 0:
      return [res]
    else:
      argmin = res[0]
      new_a_range = range_around(argmin[0], a_range, 4)
      new_b_range = range_around(argmin[1], b_range, 4)
      deep_res = helper(compressor, n - 1, new_a_range, new_b_range)

      return [res] + deep_res

    return helper(compressor, n, a_range, b_range)

  # TODO: could parallelise this?
  def run(d):
    new_compressor = functools.partial(compressor, d)
    return helper(new_compressor, n, a_range, b_range)

  return list(map(run, d_range))


# TODO: what output should it produce?
if __name__ == "__main__":
  # TODO: specify file with regexp as in benchmark
  fname = os.path.join(config.CORPUS_DIR, 'unit_tests/quickbrown.txt')
  res = n_grid_search(2, config.TESTCASES['uniform_byte_ppm'], fname)
  print(res)