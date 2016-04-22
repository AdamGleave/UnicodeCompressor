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
# TODO: fixed grid? should be able to do multi-resolution.
# TODO: different test cases, e.g. actually minimise?
PPM_CONFIG = (range(0,10,1), (-1, 3), (0, 0.99), 2)
TESTCASES['uniform_byte_ppm'] = (functools.partial(ppm, 'uniform_byte'), PPM_CONFIG)
TESTCASES['uniform_token_ppm'] = (functools.partial(ppm, 'uniform_token'), PPM_CONFIG)
TESTCASES['polya_token_ppm'] = (functools.partial(ppm, 'polya_token'), PPM_CONFIG)