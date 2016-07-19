# Copyright (C) 2016, Adam Gleave
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

import csv, itertools, os, re

from benchmark.tasks import my_compressor, ext_compressor

from benchmark.config import *

COMPRESSORS = {}
COMPRESSORS['ref_bzip2'] = (ext_compressor, {'name': 'bzip2'})
COMPRESSORS['ref_compress'] = (ext_compressor, {'name': 'compress'})
COMPRESSORS['ref_cmix'] = (ext_compressor, {'name': 'cmix'})
COMPRESSORS['ref_gzip'] = (ext_compressor, {'name': 'gzip'})
COMPRESSORS['ref_lzma'] = (ext_compressor, {'name': 'LZMA'})
COMPRESSORS['ref_paq8hp12'] = (ext_compressor, {'name': 'paq8hp12'})
COMPRESSORS['ref_PPMd'] = (ext_compressor, {'name': 'PPMd'})
COMPRESSORS['ref_SCSU'] = (ext_compressor, {'name': 'SCSU'})
COMPRESSORS['ref_zpaq'] = (ext_compressor, {'name': 'zpaq'})

algos = {'none': [], 'crp': ['crp:a=1:b=0'], 'lzw': ['lzwEscape']}
for d in [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]:
  algos['ppm{0}'.format(d)] = ['ppm:d={0}'.format(d)]

PRIORS = {'uniform_token': 'uniform_token',
          'categorical_token': 'categorical_token',
          'lzw_byte': 'lzw_byte',
          'polya_token': 'polya_token',
          'polya_token_uniform_token': 'polya_token_uniform_token',
          'uniform_stoken': 'uniform_stoken',
          'polya_stoken': 'polya_stoken',
          'polya_stoken_uniform_byte': 'polya_stoken_uniform_byte',
          'polya_stoken_uniform_token': 'polya_stoken_uniform_token',
          'uniform_byte': 'uniform_byte',
          'polya_byte': 'polya_byte'}

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
  for (prior_name, prior_config) in PRIORS.items():
    name = algo_name + '_' + prior_name
    if not is_excluded(name):
      COMPRESSORS[name] = (my_compressor, {'base': prior_config, 'algorithms': algo_config})

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
        COMPRESSORS[name] = (my_compressor, {'base': PRIORS[prior],
                                             'algorithms': ['ppm:d={0}:a={1}:b={2}'.format(d, a, b)]})

        e = float(row['mean_efficiency'])
        if e < best_e:
          best_e, best_name = e, name
      COMPRESSORS['ppm_{0}_group_opt_{1}'.format(group, prior)] = COMPRESSORS[best_name]
  except FileNotFoundError:
    print("WARNING: could not open " + path)

for group, prior in itertools.product(['training', 'test'],
                                      ['uniform_byte', 'uniform_token', 'polya_token',
                                       'polya_stoken_uniform_byte', 'polya_stoken_uniform_token']):
  group_parameters(group, prior)

# For resources.py
NUM_REPLICATIONS = 5