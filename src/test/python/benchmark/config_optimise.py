import functools

from config import *

def ppm(prior, d, a, b):
  assert(int(d) == d)

  if a + b <= 0.01:
    # a + b > 0 to be legal; make it 0.01 to ensure numerical stability
    return None
  else:
    algo_config = ['ppm:d={0}:a={1}:b={2}'.format(int(d),a,b)]
    return my_compressor(prior, algo_config)

TESTCASES = {}

PPM_ALPHA_RANGE = (-1, 3) # minimum lies within this range for all files I've seen
PPM_BETA_RANGE = (0, 0.99) # legal range is [0, 1]: some numerical issues when too close to 1
PPM_DEPTHS = range(0, 10, 1) # 0..9
PPM_GRID_GRANULARITY = 3 # TODO: bump, this is just for testing

CONTOUR_DELTA = 0.025 # bits/byte
CONTOUR_NUM_LEVELS = 10
CONTOUR_XLIM = (0, 1)
CONTOUR_YLIM = (-1, 3)

PPM_CONFIG = (PPM_DEPTHS, PPM_ALPHA_RANGE, PPM_BETA_RANGE, 2)
TESTCASES['uniform_byte_ppm'] = (functools.partial(ppm, 'uniform_byte'), PPM_CONFIG)
TESTCASES['uniform_token_ppm'] = (functools.partial(ppm, 'uniform_token'), PPM_CONFIG)
TESTCASES['polya_token_ppm'] = (functools.partial(ppm, 'polya_token'), PPM_CONFIG)