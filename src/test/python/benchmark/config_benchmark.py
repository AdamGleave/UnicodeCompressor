import csv, itertools, os, re

from benchmark.tasks import my_compressor, ext_compressor

from benchmark.config import *

COMPRESSORS = {}
COMPRESSORS['ref_bzip2'] = (ext_compressor, {'name': 'bzip2'})
COMPRESSORS['ref_compress'] = (ext_compressor, {'name': 'compress'})
COMPRESSORS['ref_gzip'] = (ext_compressor, {'name': 'gzip'})
COMPRESSORS['ref_lzma'] = (ext_compressor, {'name': 'LZMA'})
COMPRESSORS['ref_PPMd'] = (ext_compressor, {'name': 'PPMd'})

algos = {'none': [], 'crp': ['crp:a=1:b=0'], 'lzw': ['lzwEscape']}
for d in [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]:
  algos['ppm{0}'.format(d)] = ['ppm:d={0}'.format(d)]

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
      COMPRESSORS[name] = (my_compressor, {'base': prior_name, 'algorithms': algo_config})

def group_parameters(group, prior):
  fname = '{0}_ppm_multi_optimal_alpha_beta:depths=0,1,2,3,4,5,6,7,8,9:granularity=10:prior={1}.csv'.format(group, prior)
  path = os.path.join(TABLE_DIR, fname)
  try:
    with open(path, 'r') as f:
      reader = csv.DictReader(f)
      best_e, best_name = float('inf'), None
      for row in reader:
        d = int(row['depth'])
        a = float(row['alpha'])
        b = float(row['beta'])

        name = 'ppm_{0}_group_{1}_{2}'.format(group, d, prior)
        COMPRESSORS[name] = (my_compressor, {'base': prior,
                                             'algorithms': ['ppm:d={0}:a={1}:b={2}'.format(d, a, b)]})

        e = float(row['mean_efficiency'])
        if e < best_e:
          best_e, best_name = e, name
      COMPRESSORS['ppm_{0}_group_opt_{1}'.format(group, prior)] = COMPRESSORS[best_name]
  except FileNotFoundError:
    print("WARNING: could not open " + path)

for group, prior in itertools.product(['training', 'test'],
                                      ['uniform_byte', 'uniform_token', 'polya_token']):
  group_parameters(group, prior)