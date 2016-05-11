import os
import matplotlib.pyplot as plt

from benchmark.config import *

DISSERTATION_DIR = os.path.join(PROJECT_DIR, '..', 'dissertation')
BENCHMARK_INPUT = os.path.join(OUTPUT_DIR, 'tables', 'benchmark.csv')
LATEX_OUTPUT_DIR = os.path.join(DISSERTATION_DIR, 'tables')

def abbreviate_by_fname(root_path):
  dir = os.path.join(CORPUS_DIR, root_path)
  d = {}
  for fname in os.listdir(dir):
    d[os.path.join(root_path, fname)] = fname
  return d

STANDARD_CORPUS = [
  'canterbury/canterbury/alice29.txt',
  'canterbury/canterbury/asyoulik.txt',
  'canterbury/canterbury/cp.html',
  'canterbury/canterbury/fields.c',
  'canterbury/canterbury/grammar.lsp',
  'canterbury/canterbury/kennedy.xls',
  'canterbury/canterbury/lcet10.txt',
  'canterbury/canterbury/plrabn12.txt',
  'canterbury/canterbury/ptt5',
  'canterbury/canterbury/sum',
  'canterbury/canterbury/xargs.1',
  'single_language/beowulf.txt',
  'single_language/crime_and_punishment.txt',
  'single_language/genji/all.txt',
  'single_language/genji/chapter2.txt',
  'single_language/kokoro.txt',
  'single_language/ziemia_obiecana.txt',
  'mixed_language/cedict_small.txt',
  'mixed_language/creativecommonsukranian.html',
  'text_binary/genji.tar',
  'text_binary/kokoroziemia.tar',
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
  'text_binary/kokoroziemia.tar': 'kokoziem.tar'
}
FILE_ABBREVIATIONS.update(abbreviate_by_fname('canterbury/canterbury'))

ALGO_ABBREVIATIONS = {
  'none_uniform_byte': 'UB',
  'none_uniform_token': 'UT',
  'none_polya_token': 'PT',
  'crp_uniform_byte': 'CB',
  'crp_uniform_token': 'CT',
  'ref_gzip': 'gzip',
  'ref_bzip2': 'bzip2',
  'ref_PPMd': 'PPMII',
}

TABLES = {
  'singlesymbol': {
    'algos': ['none_uniform_byte', 'none_uniform_token', 'crp_uniform_byte', 'none_polya_token',
              'crp_uniform_token', 'ref_gzip', 'ref_bzip2'],
    'files': STANDARD_CORPUS,
    'scale': (1.0, 6.0),
  }
}

# TODO: choose colormap
COLORMAP = plt.cm.YlGnBu