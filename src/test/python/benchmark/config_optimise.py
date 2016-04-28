import functools, os

from benchmark.config import *

CACHE_DIR = os.path.join(OUTPUT_DIR, 'cache')
FIGURE_DIR = os.path.join(OUTPUT_DIR, 'figures')
TABLE_DIR = os.path.join(OUTPUT_DIR, 'tables')

TESTCASES = {}

PPM_ALPHA_START = -1
PPM_ALPHA_END = 3
PPM_BETA_START = 0
PPM_BETA_END = 0.99

PPM_CONTOUR_NUM_ITERATIONS = 2
PPM_CONTOUR_SHRINK_FACTOR = 2
PPM_CONTOUR_GRANULARITY = 100
PPM_CONTOUR_DELTA = 0.1 # bits/byte
PPM_CONTOUR_NUM_LEVELS = 10

# for parameter estimation grid search just needs to give a good initial guess, so can be lower
# resolution than for contours
PPM_PARAMETER_GRANULARITY = 20
PPM_PARAMETER_DEPTHS = range(0, 10, 1) # 0..9

def ppm(prior, d, a, b):
  assert(int(d) == d)

  if a + b <= 0.01:
    # a + b > 0 to be legal; make it 0.01 to ensure numerical stability
    return None
  else:
    algo_config = ['ppm:d={0}:a={1}:b={2}'.format(int(d),a,b)]
    return my_compressor(prior, algo_config)