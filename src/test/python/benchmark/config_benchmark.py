import re

from benchmark.tasks import my_compressor, ext_compressor

COMPRESSORS = {}
COMPRESSORS['ref_compress'] = (ext_compressor, {'name': 'compress'})
COMPRESSORS['ref_gzip'] = (ext_compressor, {'name': 'gzip'})
COMPRESSORS['ref_bzip2'] = (ext_compressor, {'name': 'bzip2'})
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

COMPRESSORS['ppm_test_group_opt_uniform_byte'] = (my_compressor, {'base': 'uniform_byte', 'algorithms': ['ppm:d=6:a=-0.105494661063:b=0.47794125429']})
COMPRESSORS['ppm_test_group_opt_uniform_token'] = (my_compressor, {'base': 'uniform_token', 'algorithms': ['ppm:d=4:a=-0.0588102870517:b=0.497974993229']})
COMPRESSORS['ppm_test_group_opt_polya_token'] = (my_compressor, {'base': 'polya_token', 'algorithms': ['ppm:d=4:a=-0.0653849770234:b=0.49912463989']})

COMPRESSORS['ppm_training_group_opt_uniform_byte'] = (my_compressor, {'base': 'uniform_byte', 'algorithms': ['ppm:d=6:a=0.0953509648641:b=0.408977501392']})
COMPRESSORS['ppm_training_group_opt_uniform_token'] = (my_compressor, {'base': 'uniform_token', 'algorithms': ['ppm:d=5:a=0.000907185094224:b=0.513284450993']})
COMPRESSORS['ppm_training_group_opt_polya_token'] = (my_compressor, {'base': 'polya_token', 'algorithms': ['ppm:d=5:a=0.000633029756621:b=0.513015874624']})