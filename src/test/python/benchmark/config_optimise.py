import collections, os
import matplotlib.pyplot as plt

from benchmark.config import *

## Directories
CACHE_DIR = os.path.join(OUTPUT_DIR, 'cache')

## Default parameters
PPM_ALPHA_START = -1
PPM_ALPHA_END = 3
PPM_ALPHA_RANGE = (PPM_ALPHA_START, PPM_ALPHA_END)
PPM_BETA_START = 0
PPM_BETA_END = 0.99
PPM_BETA_RANGE = (PPM_BETA_START, PPM_BETA_END)

# Contour
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

# Group contour
PPM_GROUP_CONTOUR_DEFAULT_ARGS = {
  'big_levels': 5,
  'big_delta': 0.05,
  'small_per_big': 1,
  'big_formatter': None,
  'big_manual': False,
  'inner_formatter': None,
  'inner_manual': False,
  'colormap': plt.cm.Set1,
}

PPM_GROUP_CONTOUR_OVERRIDES = {

}

# Efficiency by depth
PPM_EFFICIENCY_BY_DEPTH_FILESETS = collections.OrderedDict()
PPM_EFFICIENCY_BY_DEPTH_FILESETS['1-byte codewords'] = [
  'training/austen.txt',
  'training/doyle.txt',
  'training/forsberg.txt',
  'training/rizal.txt',
  'training/russel.html'
]
PPM_EFFICIENCY_BY_DEPTH_FILESETS['2-byte codewords'] = [
  'training/aristotle.txt',
  'training/gogol.txt'
]
PPM_EFFICIENCY_BY_DEPTH_FILESETS['3-byte codewords'] = [
  'training/confucius.txt',
  'training/jushi.txt',
  'training/shimazaki.txt'
]

PPM_EFFICIENCY_BY_DEPTH_PRIOR_LINESTYLES = {
  'uniform_byte': 'solid',
  'uniform_token': 'dashed',
  'polya_token': 'dotted',
}

PPM_EFFICIENCY_BY_DEPTH_PRIOR_MARKERS = {
  'uniform_byte': 'o',
  'uniform_token': 's',
  'polya_token': 'p',
}

PPM_EFFICIENCY_BY_DEPTH_COLORMAP = plt.cm.Set1

# Parameter estimation
PPM_PARAMETER_GRANULARITY = 10
PPM_PARAMETER_DEPTHS = range(0, 10, 1) # 0..9

## Abbreviations
SHORT_FILE_NAME = {
  'canterbury/canterbury/alice29.txt': 'alice29.txt',
  'canterbury/canterbury/lcet10.txt': 'lcet10.txt',
}

SHORT_PRIOR_NAME = {
  'uniform_byte': 'UB',
  'uniform_token': 'UT',
  'polya_token': 'PT',
}

# This limits the number of high-level operations which can be performed in parallel.
# As a rule of thumb, it should be close to the *total number* of cores in the cluster.
NUM_WORKERS = 64