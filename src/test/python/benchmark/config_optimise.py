import functools, os
import matplotlib.pyplot as plt

from benchmark.config import *

CACHE_DIR = os.path.join(OUTPUT_DIR, 'cache')
DATA_DIR = os.path.join(OUTPUT_DIR, 'data')
FIGURE_DIR = os.path.join(OUTPUT_DIR, 'figures')
TABLE_DIR = os.path.join(OUTPUT_DIR, 'tables')

TESTCASES = {}

PPM_ALPHA_START = -1
PPM_ALPHA_END = 3
PPM_BETA_START = 0
PPM_BETA_END = 0.99

PPM_CONTOUR_GRANULARITY = 200
PPM_CONTOUR_DEFAULT_ARGS = {
  'big_levels': 10,
  'big_delta': 0.1,
  'small_per_big': 10,
  'big_formatter': '%.1f',
  'big_manual': False,
  'inner_formatter': '%.2f',
  'inner_manual': False,
}

PPM_CONTOUR_OVERRIDES = {
  frozenset(['canterbury/canterbury/alice29.txt']): {
    'ppm_contour_plot:depth=4:prior=uniform_byte': {
      'big_levels': 9,
      'small_per_big': 20,
      'inner_formatter': '%.3f',
    }
  }
}

SHORT_FILE_NAME = {
  'canterbury/canterbury/alice29.txt': 'alice29.txt',
  'canterbury/canterbury/lcet10.txt': 'lcet10.txt',
}

SHORT_PRIOR_NAME = {
  'uniform_byte': 'UB',
  'uniform_token': 'UT',
  'polya_token': 'PT',
}

PPM_GROUP_CONTOUR_DEFAULT_ARGS = {
  'big_levels': 5,
  'big_delta': 0.05,
  'small_per_big': '1',
  'big_formatter': None,
  'big_manual': False,
  'inner_formatter': None,
  'inner_manual': False,
  'colormap': plt.cm.Set1,
}

PPM_GROUP_CONTOUR_OVERRIDES = {

}

# for parameter estimation grid search just needs to give a good initial guess, so can be lower
# resolution than for contours
PPM_PARAMETER_GRANULARITY = 20
PPM_PARAMETER_DEPTHS = range(0, 10, 1) # 0..9

# This limits the number of high-level operations which can be performed in parallel.
# As a rule of thumb, it should be close to the *total number* of cores in the cluster.
NUM_WORKERS = 64