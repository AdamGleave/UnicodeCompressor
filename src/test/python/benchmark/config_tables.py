import os
import matplotlib.pyplot as plt

from benchmark.config import *

## Input/output paths
DISSERTATION_DIR = os.path.join(PROJECT_DIR, '..', 'dissertation')
BENCHMARK_INPUT = os.path.join(OUTPUT_DIR, 'tables', 'benchmark.csv')
RESOURCES_INPUT = os.path.join(OUTPUT_DIR, 'tables', 'resources.csv')
LATEX_OUTPUT_DIR = os.path.join(DISSERTATION_DIR, 'tables')
JSON_OUTPUT = os.path.join(OUTPUT_DIR, 'tables', 'benchmark.js')

def abbreviate_by_fname(root_path):
  dir = os.path.join(CORPUS_DIR, root_path)
  d = {}
  for fname in os.listdir(dir):
    d[os.path.join(root_path, fname)] = fname
  return d

STANDARD_CORPUS = [
  ('cantb',
   ['canterbury/canterbury/alice29.txt',
    'canterbury/canterbury/asyoulik.txt',
    'canterbury/canterbury/cp.html',
    'canterbury/canterbury/fields.c',
    'canterbury/canterbury/grammar.lsp',
    'canterbury/canterbury/kennedy.xls',
    'canterbury/canterbury/lcet10.txt',
    'canterbury/canterbury/plrabn12.txt',
    'canterbury/canterbury/ptt5',
    'canterbury/canterbury/sum',
    'canterbury/canterbury/xargs.1']
  ),
  ('single_language',
   ['single_language/beowulf.txt',
    'single_language/crime_and_punishment.txt',
    'single_language/genji/all.txt',
    'single_language/genji/chapter2.txt',
    'single_language/kokoro.txt',
    'single_language/ziemia_obiecana.txt']
   ),
  ('mixed_language',
   ['mixed_language/cedict_small.txt',
    'mixed_language/creativecommonsukranian.html']
  ),
  ('binary',
   ['text_binary/genji.tar',
    'text_binary/kokoroziemia.tar']
  ),
]

FULL_CORPUS = STANDARD_CORPUS + [
  ('training',
   ['training/aristotle.txt',
    'training/austen.txt',
    'training/confucius.txt',
    'training/doyle.txt',
    'training/forsberg.txt',
    'training/gogol.txt',
    'training/jushi.txt',
    'training/rizal.txt',
    'training/russel.html',
    'training/shimazaki.txt']
  ),
]

FILE_ABBREVIATIONS = {
  'single_language/beowulf.txt': 'beowulf.txt',
  'single_language/crime_and_punishment.txt': 'dostoevsky.txt',
  'single_language/genji/all.txt': 'genji.txt',
  'single_language/genji/chapter2.txt': 'genji02.txt',
  'single_language/kokoro.txt': 'kokoro.txt',
  'single_language/ziemia_obiecana.txt': 'obiecana.txt',
  'mixed_language/cedict_small.txt': 'dictionary.txt',
  'mixed_language/creativecommonsukranian.html': 'license.html',
  'text_binary/genji.tar': 'genji.tar',
  'text_binary/kokoroziemia.tar': 'kokoziem.tar',
  'resource_consumption/alice.txt': 'alice29.txt',
  'resource_consumption/genji.txt': 'genji.txt',
  'resource_consumption/obiecana.txt': 'obiecana.txt',
}
FILE_ABBREVIATIONS.update(abbreviate_by_fname('canterbury/canterbury'))
FILE_ABBREVIATIONS.update(abbreviate_by_fname('training/'))

## Algorithms

ALGO_ABBREVIATIONS = {
  'none_uniform_byte': r'\noneuniformbyte',
  'none_uniform_token': r'\noneuniformtoken',
  'none_polya_token': r'\nonepolyatoken',
  'crp_uniform_byte': r'\dirichletuniformbyte',
  'crp_uniform_token': r'\dirichletuniformtoken',
  'none_lzw_byte': r'\nonelzwbyte',
  'lzw_uniform_byte': r'\lzwuniformbyte',
  'lzw_uniform_token': r'\lzwuniformtoken',
  'lzw_polya_token': r'\lzwpolyatoken',
  'ppm5_uniform_byte': r'\ppmd',
  'ppm_training_group_opt_uniform_byte': r'\ppmtraininguniformbyte',
  'ppm_training_group_5_uniform_byte': r'\ppmtraininguniformbytefive',
  'ppm_training_group_opt_uniform_token': r'\ppmtraininguniformtoken',
  'ppm_training_group_opt_polya_token': r'\ppmtrainingpolyatoken',
  'ref_compress': r'\compress',
  'ref_bzip2': r'\bziptwo',
  'ref_gzip': r'\gzip',
  'ref_paq8hp12': r'\paqhp',
  'ref_PPMd': r'\ppmii',
  'ref_cmix': r'\cmix'
}

## Score tables

def get_leading(algo):
  '''typical number of digits before decimal place'''
  if algo == 'none_uniform_token':
    return 2
  else:
    return 1

def get_column_type(algo):
  if algo == 'ref_bzip2':
    return 'l'
  elif algo == 'ref_cmix':
    return 'r'
  elif algo == 'ref_PPMd':
    return 'l'
  else:
    return 'c'

def get_padding(algo):
  if algo == 'ref_bzip2':
    return ('\hspace{-3pt}', '')
  elif algo == 'ref_PPMd':
    return ('\hspace{-5pt}', '')
  elif algo == 'ref_cmix':
    return ('', '\hspace{-8pt}')
  elif algo == 'ppm5_uniform_byte':
    return ('\hspace{-8pt}', '\hspace{-8pt}')
  else:
    return ('', '')

# Colors

def constant_colormap(r, g, b, a):
  def f(_x):
    return [r, g, b, a]
  return f
def invert_colormap(cm):
  def f(x):
    return map(lambda x : 1 - x, cm(x))
  return f
def clip_colormap(cm):
  def f(x):
    r, g, b, a = cm(x)
    intensity = (r + g + b) / 3
    if intensity >= 0.5:
      return (1, 1, 1, a)
    else:
      return (0, 0, 0, a)
  return f

BG_COLORMAP = plt.cm.BuGn
FG_COLORMAP = clip_colormap(invert_colormap(BG_COLORMAP))

# Output tables

SCORE_TABLES = {
  'singlesymbol': {
    'algos': [
      ('Static', ['none_uniform_byte', 'none_uniform_token']),
      ('Adaptive', ['crp_uniform_byte', 'crp_uniform_token', 'none_polya_token']),
      ('Reference', ['ref_gzip', 'ref_bzip2']),
    ],
    'files': STANDARD_CORPUS,
    'scale': (1.0, 6.0),
  },
  'lzw': {
    'algos': [
      ('Original', ['ref_compress', 'none_lzw_byte']),
      ('Escaped', ['lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']),
      ('Reference', ['ref_gzip', 'ref_bzip2']),
    ],
    'files': STANDARD_CORPUS,
  },
  'ppm': {
    'algos': [
      ('PPM',
       ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
        'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte', 'ref_PPMd']
      ),
      ('Reference', ['ref_cmix', 'ref_paq8hp12', 'ref_gzip', 'ref_bzip2']),
    ],
    'files': STANDARD_CORPUS,
    'scale': (0.75, 3.5),
  },
}

## Resources

RESOURCE_CORPUS = ['resource_consumption/alice.txt',
                   'resource_consumption/genji.txt']
RESOURCE_ALGOS = ['ppm_training_group_opt_uniform_byte',
                  'ppm_training_group_opt_polya_token',
                  'ref_PPMd',
                  'ref_cmix',
                  'ref_paq8hp12']
RESOURCE_ALPHA = 0.95 # uncertainty in confidence intervals

RESOURCE_TABLES = {
  'runtime': {
    'col': 'runtime',
    'files': RESOURCE_CORPUS,
    'algos': RESOURCE_ALGOS,
  },
  'memory': {
    'col': 'memory',
    'files': RESOURCE_CORPUS,
    'algos': RESOURCE_ALGOS,
  },
}

## All tables
def merge(tables):
  res = {}
  for table, type in tables:
    for k, v in table.items():
      v['type'] = type
      assert(k not in res)
      res[k] = v
  return res

TABLES = merge([
  (SCORE_TABLES, 'score'),
  (RESOURCE_TABLES, 'resource')
])