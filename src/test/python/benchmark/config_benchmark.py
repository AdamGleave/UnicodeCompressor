import os, re
from config import *

COMPRESSORS = {}
COMPRESSORS['ref_gzip'] = build_compressor(['gzip', '-c'], [], ['-d'])
COMPRESSORS['ref_bzip2'] = build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d'])

PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
COMPRESSORS['ref_PPMd'] = build_compressor([PPMd_EXECUTABLE], ['e'], ['d'])

algos = {'none': [], 'crp': ['crp:a=1:b=0'], 'lzw': ['lzwEscape']}

for d in [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]:
  algos['ppm{0}'.format(d)] = ['ppm:d={0}'.format(d)]
algos['ppm5alice'] = ['ppm:d=5:a=0.16875:b=0.38379687']
algos['ppm5alice_christian'] = ['ppm:d=5:a=0.223:b=0.366']

priors = {'uniform_token': 'uniform_token',
          'categorical_token': 'categorical_token',
          'lzw_byte': 'lzw_byte',
          'polya_token': 'polya_token',
          'uniform_byte': 'uniform_byte',
          'polya_byte': 'polya_byte' }

EXCLUDED = ['crp_polya_.*', # fails as Polya doesn't implement discreteMass
            # only permit none_categorical_token: possible to run with algos, but it's just not
            # that different from uniform_token
            '(?!none).*_categorical_token',
            '(?!none).*_polya_byte', # possible but not particularly interesting
            '^(?!none).*_lzw_byte', # only permit none_lzw_byte, not any other algo
            ]
EXCLUDED = list(map(lambda x: re.compile(x), EXCLUDED))

def is_excluded(name):
  for p in EXCLUDED:
    if p.match(name):
      return True
  return False

for (algo_name, algo_config) in algos.items():
  for (prior_name, prior_config) in priors.items():
    name = algo_name + '_' + prior_name
    if not is_excluded(name):
      COMPRESSORS[name] = my_compressor(prior_name, algo_config)
