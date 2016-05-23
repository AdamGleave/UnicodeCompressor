import collections, os
import matplotlib.pyplot as plt

from benchmark.config import *

## Input/output paths
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

STANDARD_CORPUS_GROUPED = collections.OrderedDict()
STANDARD_CORPUS_GROUPED['ASCII'] = [
   'canterbury/canterbury/alice29.txt',
   'canterbury/canterbury/asyoulik.txt',
   'canterbury/canterbury/cp.html',
   'canterbury/canterbury/fields.c',
   'canterbury/canterbury/grammar.lsp',
   'canterbury/canterbury/lcet10.txt',
   'canterbury/canterbury/plrabn12.txt',
   'canterbury/canterbury/xargs.1'
]
STANDARD_CORPUS_GROUPED['Unicode'] = [
  'single_language/crime_and_punishment.txt',
  'single_language/genji/all.txt',
  'single_language/genji/chapter2.txt',
  'single_language/kokoro.txt',
]
STANDARD_CORPUS_GROUPED['Mixed'] = [
  'single_language/beowulf.txt',
  'single_language/ziemia_obiecana.txt',
  'mixed_language/cedict_small.txt',
  'mixed_language/creativecommonsukranian.html',
]
STANDARD_CORPUS_GROUPED['Binary'] = [
  'canterbury/canterbury/kennedy.xls',
  'canterbury/canterbury/ptt5',
  'canterbury/canterbury/sum',
  'text_binary/genji.tar',
  'text_binary/kokoroziemia.tar'
]

STANDARD_TEXT_CORPUS = STANDARD_CORPUS_GROUPED['ASCII'] \
                     + STANDARD_CORPUS_GROUPED['Unicode'] \
                     + STANDARD_CORPUS_GROUPED['Mixed']

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
      ('Reference', ['ref_cmix', 'ref_paq8hp12', 'ref_bzip2']),
    ],
    'files': STANDARD_CORPUS,
    'scale': (0.75, 3.5),
  },
}

## Score summaries

BEST_ALGOS = ['ppm_training_group_opt_uniform_byte',
              'ppm_training_group_opt_polya_token',
              'ref_PPMd',
              'ref_cmix',
              'ref_paq8hp12']
SCORE_SUMMARIES = {
  'ppm_summary': {
    'algos': BEST_ALGOS,
    'files': {k: STANDARD_CORPUS_GROUPED[k] for k in ['ASCII', 'Unicode']},
    'scale': (1.4, 2.5),
  }
}

## Parameters

PARAMETER_ALGOS = collections.OrderedDict()
PARAMETER_ALGOS['uniform_byte'] = ('Byte', 'Uniform')
PARAMETER_ALGOS['uniform_token'] = ('Token', 'Uniform')
PARAMETER_ALGOS['polya_token'] = ('Token', r"P\'{o}lya")

PARAMETER_TABLES = {
  'trainingvstest': {
    'algos': PARAMETER_ALGOS,
    'files': STANDARD_TEXT_CORPUS,
    'test_parameters': os.path.join(OUTPUT_DIR, 'tables', 'test_ppm_multi_optimal_alpha_beta:depths=0,1,2,3,4,5,6,7,8,9:granularity=10:prior=consolidated.csv'),
  }
}

## Resources

RESOURCE_ALGOS = BEST_ALGOS
RESOURCE_ALPHA = 0.95 # uncertainty in confidence intervals

RESOURCE_TABLES = {
  'runtime_table': {
    'col': 'runtime',
    'files': ['single_language/genji/chapter2.txt'],
    'algos': RESOURCE_ALGOS,
  },
  'memory_table': {
    'col': 'memory',
    'files': ['single_language/genji/chapter2.txt'],
    'algos': RESOURCE_ALGOS,
  },
}

RESOURCE_FIGURES = {
  'runtime_fig': {
    'col': 'runtime',
    'file': 'single_language/genji/chapter2.txt',
    'algos': RESOURCE_ALGOS,
    'style': '1col_double',
  },
  'memory_fig': {
    'col': 'memory',
    'file': 'single_language/genji/chapter2.txt',
    'algos': RESOURCE_ALGOS,
    'style': '1col_double',
  }
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

TESTS = merge([
  (SCORE_TABLES, 'score_full'),
  (SCORE_SUMMARIES, 'score_summary'),
  (PARAMETER_TABLES, 'parameter_table'),
  (RESOURCE_TABLES, 'resource_table'),
  (RESOURCE_FIGURES, 'resource_figure'),
])