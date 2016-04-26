import functools, os

from config import *

FIGURE_DIR = os.path.join(OUTPUT_DIR, 'figures')

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

PPM_DEPTHS = range(0, 10, 1) # 0..9

