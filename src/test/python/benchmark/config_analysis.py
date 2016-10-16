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

import collections, itertools, os
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

STANDARD_CORPUS = collections.OrderedDict()
STANDARD_CORPUS['cantb'] = [
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
  'canterbury/canterbury/xargs.1'
]
STANDARD_CORPUS['single_language'] = [
  'single_language/beowulf.txt',
  'single_language/crime_and_punishment.txt',
  'single_language/genji/all.txt',
  'single_language/genji/chapter2.txt',
  'single_language/kokoro.txt',
  'single_language/ziemia_obiecana.txt'
]
STANDARD_CORPUS['mixed_language'] = [
  'mixed_language/cedict_small.txt',
  'mixed_language/creativecommonsukranian.html'
]

STANDARD_CORPUS['binary'] = [
  'text_binary/genji.tar',
  'text_binary/kokoroziemia.tar'
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
  #'mixed_language/cedict_small.txt',
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

FULL_CORPUS = collections.OrderedDict(STANDARD_CORPUS)
FULL_CORPUS['training'] = [
  'training/aristotle.txt',
  'training/austen.txt',
  'training/confucius.txt',
  'training/doyle.txt',
  'training/forsberg.txt',
  'training/gogol.txt',
  'training/jushi.txt',
  'training/rizal.txt',
  'training/russel.html',
  'training/shimazaki.txt'
]

DCC_SMALL_CORPUS = collections.OrderedDict()
DCC_SMALL_CORPUS['cantb'] = STANDARD_CORPUS['cantb']
DCC_SMALL_CORPUS['single_language'] = [
  'dcc_small/ar-tabula.txt',
  'dcc_small/hin-baital.txt',
  'dcc_small/ita-histo.txt',
  'dcc_small/lah-udhr.txt',
  'dcc_small/rus-mosco.txt',
  'dcc_small/tel-kolla.txt',
  'dcc_small/spa-trans.txt',
  'dcc_small/zho-hua.txt',
  'dcc_small/zho-lie.txt',
  'dcc_small/zho-you.txt',
]
DCC_SMALL_CORPUS['mixed_language'] = [
  'dcc_small/khuyen.txt',
  'dcc_small/sake.txt'
]

DCC_SMALL_CORPUS_GROUPED = collections.OrderedDict()
DCC_SMALL_CORPUS_GROUPED['ASCII'] = STANDARD_CORPUS_GROUPED['ASCII']
DCC_SMALL_CORPUS_GROUPED['Unicode'] = [
  'dcc_small/ar-tabula.txt',
  'dcc_small/hin-baital.txt',
  'dcc_small/ita-histo.txt',
  'dcc_small/lah-udhr.txt',
  'dcc_small/rus-mosco.txt',
  'dcc_small/tel-kolla.txt',
  'dcc_small/spa-trans.txt',
  'dcc_small/zho-hua.txt',
  'dcc_small/zho-lie.txt',
  'dcc_small/zho-you.txt',
  'dcc_small/sake.txt',
  'dcc_small/khuyen.txt',
]
DCC_SMALL_CORPUS_GROUPED['Binary'] = [
  'canterbury/canterbury/kennedy.xls',
  'canterbury/canterbury/ptt5',
  'canterbury/canterbury/sum',
]

DCC_SMALL_CAPPED_CORPUS = collections.OrderedDict(DCC_SMALL_CORPUS)
DCC_SMALL_CAPPED_CORPUS['single_language'] = [
  'dcc_small_capped/ar-tabula.txt',
  'dcc_small_capped/ben-kobita.txt',
  'dcc_small_capped/hin-baital.txt',
  'dcc_small_capped/jav-tuban.txt',
  'dcc_small_capped/jpn-yujo.txt',
  'dcc_small_capped/lah-udhr.txt',
  'dcc_small_capped/por-noites.txt',
  'dcc_small_capped/rus-mosco.txt',
  'dcc_small_capped/spa-trans.txt',
  'dcc_small_capped/zho-you.txt',
]

DCC_SMALL_CAPPED_CORPUS_GROUPED = collections.OrderedDict(DCC_SMALL_CORPUS_GROUPED)
DCC_SMALL_CAPPED_CORPUS_GROUPED['Unicode'] = [
  'dcc_small_capped/ar-tabula.txt',
  'dcc_small_capped/ben-kobita.txt',
  'dcc_small_capped/hin-baital.txt',
  'dcc_small_capped/jav-tuban.txt',
  'dcc_small_capped/jpn-yujo.txt',
  'dcc_small_capped/lah-udhr.txt',
  'dcc_small_capped/por-noites.txt',
  'dcc_small_capped/rus-mosco.txt',
  'dcc_small_capped/spa-trans.txt',
  'dcc_small_capped/zho-you.txt',
  'dcc_small/sake.txt',
  'dcc_small/khuyen.txt',
]

DCC_SMALL_COMBINED_CORPUS = collections.OrderedDict(DCC_SMALL_CORPUS)
DCC_SMALL_COMBINED_CORPUS['single_language'] = [
  'dcc_small/ar-tabula.txt', # both
  'dcc_small_capped/ben-kobita.txt', # capped only
  'dcc_small/hin-baital.txt', # both
  'dcc_small_capped/jav-tuban.txt', # capped only
  'dcc_small_capped/jpn-yujo.txt', # capped only
  'dcc_small/ita-histo.txt', # uncapped only
  'dcc_small/lah-udhr.txt', # both
  'dcc_small_capped/por-noites.txt', # capped only
  'dcc_small/rus-mosco.txt', # both
  'dcc_small/tel-kolla.txt', # uncapped only
  'dcc_small/spa-trans.txt', # both
  'dcc_small/zho-hua.txt', # uncapped only
  'dcc_small/zho-lie.txt', # uncapped only
  'dcc_small/zho-you.txt', # both
]

DCC_LARGE_CORPUS = collections.OrderedDict()
DCC_LARGE_CORPUS['ar'] = [
	'dcc_large/ar/bookofroadsandkingdoms.txt',
	'dcc_large/ar/canonofmedicine_book1.txt',
	'dcc_large/ar/ibnal-baitar.txt',
	'dcc_large/ar/onethousandandonenights_extract.txt',
	'dcc_large/ar/tabularogeriana.txt',
	'dcc_large/ar/udhr.txt',
]
DCC_LARGE_CORPUS['ben'] = [
	'dcc_large/ben/anandamath.txt',
	'dcc_large/ben/meghnadbodhkavya.txt',
	'dcc_large/ben/mountainofthemoon.txt',
	'dcc_large/ben/shesherkobita.txt',
	'dcc_large/ben/udhr.txt',
]
DCC_LARGE_CORPUS['deu'] = [
	'dcc_large/deu/becker.txt',
	'dcc_large/deu/freud.txt',
	'dcc_large/deu/freytag.txt',
	'dcc_large/deu/schefer.txt',
	'dcc_large/deu/udhr.txt',
	'dcc_large/deu/urzidil.txt',
]
DCC_LARGE_CORPUS['fas'] = [
	'dcc_large/fas/cursingoftheland.txt',
	'dcc_large/fas/datebeyhaqi.txt',
	'dcc_large/fas/kalilawadimna.txt',
	'dcc_large/fas/lawrence.txt',
	'dcc_large/fas/shahnameh_extract.txt',
	'dcc_large/fas/udhr.txt',
]
DCC_LARGE_CORPUS['fra'] = [
	'dcc_large/fra/buysse.txt',
	'dcc_large/fra/confucius.txt',
	'dcc_large/fra/feval.txt',
	'dcc_large/fra/philippe.txt',
	'dcc_large/fra/robida.txt',
	'dcc_large/fra/udhr.txt',
]
DCC_LARGE_CORPUS['hin'] = [
	'dcc_large/hin/baitalpachisi.txt',
	'dcc_large/hin/barfi.txt',
	'dcc_large/hin/specialrelativity.txt',
	'dcc_large/hin/udhr.txt',
]
DCC_LARGE_CORPUS['ita'] = [
	'dcc_large/ita/bracco.txt',
	'dcc_large/ita/concilio.txt',
	'dcc_large/ita/history1.txt',
	'dcc_large/ita/history2.txt',
	'dcc_large/ita/panzini.txt',
	'dcc_large/ita/udhr.txt',
]
DCC_LARGE_CORPUS['jav'] = [
	'dcc_large/jav/franzkafka.txt',
	'dcc_large/jav/kakawinnitisastra.txt',
	'dcc_large/jav/pramoedya.txt',
	'dcc_large/jav/rangsangtuban.txt',
	'dcc_large/jav/udhr.txt',
]
DCC_LARGE_CORPUS['jpn'] = [
	'dcc_large/jpn/lowndes.txt',
	'dcc_large/jpn/morley.txt',
	'dcc_large/jpn/mushanokoji1.txt',
	'dcc_large/jpn/mushanokoji2.txt',
	'dcc_large/jpn/oppenheim.txt',
	'dcc_large/jpn/udhr.txt',
]
DCC_LARGE_CORPUS['kor'] = [
	'dcc_large/kor/choenamseon.txt',
	'dcc_large/kor/leekwangso.txt',
	'dcc_large/kor/soilofslavery.txt',
	'dcc_large/kor/tolstoy.txt',
	'dcc_large/kor/udhr.txt',
]
DCC_LARGE_CORPUS['lah'] = [
	'dcc_large/lah/udhr.txt',
]
DCC_LARGE_CORPUS['mar'] = [
	'dcc_large/mar/dasbodh_extract.txt',
	'dcc_large/mar/ekach.txt',
	'dcc_large/mar/eknathi.txt',
	'dcc_large/mar/sangeet.txt',
	'dcc_large/mar/shyamchiaai.txt',
	'dcc_large/mar/udhr.txt',
]
DCC_LARGE_CORPUS['msa'] = [
	'dcc_large/msa/akta.txt',
	'dcc_large/msa/minang.txt',
	'dcc_large/msa/sultan.txt',
	'dcc_large/msa/udhr.txt',
]
DCC_LARGE_CORPUS['multilingual'] = [
	'dcc_large/multilingual/khuyen.txt',
	'dcc_large/multilingual/sake.txt',
]
DCC_LARGE_CORPUS['por'] = [
	'dcc_large/por/bastos.txt',
	'dcc_large/por/branco.txt',
	'dcc_large/por/escrich.txt',
	'dcc_large/por/feydeau.txt',
	'dcc_large/por/garrett.txt',
	'dcc_large/por/udhr.txt',
]
DCC_LARGE_CORPUS['rus'] = [
	'dcc_large/rus/apostol.txt',
	'dcc_large/rus/derzhavin.txt',
	'dcc_large/rus/pushkin.txt',
	'dcc_large/rus/rachinskii.txt',
	'dcc_large/rus/udhr.txt',
	'dcc_large/rus/zhenskoe.txt',
]
DCC_LARGE_CORPUS['spa'] = [
	'dcc_large/spa/transfusion.txt',
	'dcc_large/spa/udhr.txt',
	'dcc_large/spa/unamuno.txt',
	'dcc_large/spa/valera.txt',
	'dcc_large/spa/valls.txt',
	'dcc_large/spa/verdugo.txt',
]
DCC_LARGE_CORPUS['tam'] = [
	'dcc_large/tam/kambar.txt',
	'dcc_large/tam/nachiyappan.txt',
	'dcc_large/tam/parthasarathy.txt',
	'dcc_large/tam/sundaravadivelu.txt',
	'dcc_large/tam/udhr.txt',
]
DCC_LARGE_CORPUS['tel'] = [
	'dcc_large/tel/agnigundam.txt',
	'dcc_large/tel/eedaari.txt',
	'dcc_large/tel/kattula.txt',
	'dcc_large/tel/kollayi.txt',
	'dcc_large/tel/onamaalu.txt',
	'dcc_large/tel/udhr.txt',
]
DCC_LARGE_CORPUS['tur'] = [
	'dcc_large/tur/ataturk.txt',
	'dcc_large/tur/bildirki.txt',
	'dcc_large/tur/dedekorkut.txt',
	'dcc_large/tur/udhr.txt',
	'dcc_large/tur/yunusemre.txt',
	'dcc_large/tur/ziyagokalp.txt',
]
DCC_LARGE_CORPUS['urd'] = [
	'dcc_large/urd/cricket.txt',
	'dcc_large/urd/ghalib.txt',
	'dcc_large/urd/mirtaqirmir.txt',
	'dcc_large/urd/udhr.txt',
]
DCC_LARGE_CORPUS['vie'] = [
	'dcc_large/vie/hobieuchanh.txt',
	'dcc_large/vie/nguyendo.txt',
	'dcc_large/vie/nguyentrai.txt',
	'dcc_large/vie/spratlyislands.txt',
	'dcc_large/vie/udhr.txt',
]
DCC_LARGE_CORPUS['zho'] = [
	'dcc_large/zho/digong.txt',
	'dcc_large/zho/hua.txt',
	'dcc_large/zho/lie.txt',
	'dcc_large/zho/tao.txt',
	'dcc_large/zho/udhr.txt',
	'dcc_large/zho/you.txt',
]

DCC_CORPUS_GROUPED = collections.OrderedDict()
DCC_CORPUS_GROUPED['English'] = STANDARD_CORPUS_GROUPED['ASCII']
DCC_CORPUS_GROUPED['Binary'] = DCC_SMALL_CORPUS_GROUPED['Binary']
DCC_CORPUS_GROUPED['Small uncapped'] = DCC_SMALL_CORPUS_GROUPED['Unicode']
DCC_CORPUS_GROUPED['Small capped'] = DCC_SMALL_CAPPED_CORPUS_GROUPED['Unicode']
DCC_CORPUS_GROUPED['Large Corpus'] = list(itertools.chain(*DCC_LARGE_CORPUS.values()))

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
FILE_ABBREVIATIONS.update(abbreviate_by_fname('dcc_small/'))
# some of these are abbreviated to the same as in dcc_small, but that's OK (same contents)
FILE_ABBREVIATIONS.update(abbreviate_by_fname('dcc_small_capped/'))

## Algorithms

ALGO_ABBREVIATIONS = {
  'none_uniform_byte': r'\noneuniformbyte',
  'none_uniform_token': r'\noneuniformtoken',
  'none_polya_token': r'\nonepolyatoken',
  'none_polya_token_broken': r'\nonepolyatokenbroken',
  'none_polya_stoken_uniform_token': r'\nonepolyastokenuniformtoken',
  'none_polya_stoken_uniform_byte': r'\nonepolyastokenuniformbyte',
  'crp_uniform_byte': r'\dirichletuniformbyte',
  'crp_uniform_token': r'\dirichletuniformtoken',
  'none_lzw_byte': r'\nonelzwbyte',
  'lzw_uniform_byte': r'\lzwuniformbyte',
  'lzw_uniform_token': r'\lzwuniformtoken',
  'lzw_polya_token': r'\lzwpolyatoken',
  'lzw_polya_token_broken': r'\lzwpolyatokenbroken',
  'lzw_polya_stoken_uniform_token': r'\lzwpolyastokenuniformtoken',
  'lzw_polya_stoken_uniform_byte': r'\lzwpolyastokenuniformbyte',
  'ppm5_uniform_byte': r'\ppmd',
  'ppm_training_group_opt_uniform_byte': r'\ppmtraininguniformbyte',
  'ppm_training_group_5_uniform_byte': r'\ppmtraininguniformbytefive',
  'ppm_training_group_opt_uniform_token': r'\ppmtraininguniformtoken',
  'ppm_training_group_opt_polya_token': r'\ppmtrainingpolyatoken',
  'ppm_training_group_opt_polya_token_broken': r'\ppmtrainingpolyatokenbroken',
  'ppm_training_group_opt_polya_stoken_uniform_token': r'\ppmtrainingpolyastokenuniformtoken',
  'ppm_training_group_optut_polya_stoken_uniform_byte': r'\ppmtrainingpolyastokenuniformbytewithuniformtokenparam',
  'ppm_training_group_opt_polya_stoken_uniform_byte': r'\ppmtrainingpolyastokenuniformbyte',
  'ref_compress': r'\compress',
  'ref_bzip2': r'\bziptwo',
  'ref_gzip': r'\gzip',
  'ref_paq8hp12': r'\paqhp',
  'ref_PPMd': r'\ppmii',
  'ref_SCSU': r'\scsu',
  'ref_cmix': r'\cmix'
}

## Score tables

def get_leading(algo):
  '''typical number of digits before decimal place'''
  if algo == 'none_uniform_token':
    return 2
  else:
    return 1

def default_column_type(algo):
  if algo == 'ref_bzip2':
    return 'l'
  elif algo == 'ref_SCSU':
    return 'r'
  elif algo == 'ref_cmix':
    return 'r'
  elif algo == 'ref_PPMd':
    return 'l'
  else:
    return 'c'

def default_padding(algo):
  if algo == 'ref_bzip2':
    return ('\hspace{-3pt}', '')
  elif algo == 'ref_PPMd':
    return ('\hspace{-5pt}', '')
  elif algo == 'ref_cmix':
    return ('', '\hspace{-8pt}')
  elif algo == 'ref_SCSU':
    return ('', '\hspace{-6pt}')
  elif algo == 'ppm5_uniform_byte':
    return ('\hspace{-8pt}', '\hspace{-8pt}')
  else:
    return ('', '')

def long_column_type(algo):
  if algo == 'ref_PPMd':
    return 'l'
  elif algo == 'ref_SCSU':
    return 'r'
  elif algo == 'ref_paq8hp12any':
    return 'l'
  else:
    return 'c'

def long_padding(algo):
  if algo == 'ref_PPMd':
    return ('\hspace{-5pt}', '') 
  elif algo == 'ref_SCSU':
    return ('', '\hspace{-6pt}')
  elif algo == 'ref_bzip2':
    return ('\hspace{-3pt}', '\hspace{-5pt}')
  elif algo == 'ref_cmix':
    return ('\hspace{-9pt}', '\hspace{-12pt}')
  else:
    return ('', '')

default_font = 'Palatino'

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
def narrowed_colormap(cm, lower, upper):
  def f(x):
    x = max(0, x)
    x = min(1, x)
    y = lower + (upper - lower)*x
    return cm(y)
  return f

BG_COLORMAP = narrowed_colormap(plt.cm.BuGn, 0, 0.8)
FG_COLORMAP = clip_colormap(invert_colormap(BG_COLORMAP))
ERROR_COLOUR = ((1, 0, 0), (1, 1, 1))

FONT_BOLD_ADJUSTMENTS = {
  'Palatino': None,
  'Computer Modern': '0.17',
}
# Output tables

SCORE_TABLES = {
  'singlesymbol': {
    'algos': [
      ('Static', ['none_uniform_byte', 'none_uniform_token']),
      ('Adaptive', ['crp_uniform_byte', 'crp_uniform_token', 'none_polya_token']),
      ('Reference', ['ref_SCSU', 'ref_gzip', 'ref_bzip2']),
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
  'appendix_singlesymbol': {
    'algos': [
      ('Static', ['none_uniform_byte', 'none_uniform_token']),
      ('Adaptive', ['crp_uniform_byte', 'crp_uniform_token', 'none_polya_token']),

    ],
    'files': STANDARD_CORPUS,
  },
  'appendix_lzw': {
    'algos': [
      ('LZW', ['ref_compress', 'none_lzw_byte', 'lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']),
    ],
    'files': STANDARD_CORPUS,
  },
  'appendix_ppm': {
    'algos': [
      ('PPM',
       ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
        'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte', 'ref_PPMd']
       ),
    ],
    'files': STANDARD_CORPUS,
  },
  'appendix_reference': {
    'algos': [
      ('Reference', ['ref_SCSU', 'ref_gzip', 'ref_bzip2', 'ref_cmix', 'ref_paq8hp12']),
    ],
    'files': STANDARD_CORPUS,
  },
  'appendix_long1': {
    'algos': [
      ('Static', ['none_uniform_byte', 'none_uniform_token']),
      ('Adaptive', ['crp_uniform_byte', 'crp_uniform_token', 'none_polya_token']),
      ('LZW', ['ref_compress', 'none_lzw_byte', 'lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']),
    ],
    'files': STANDARD_CORPUS,
    'column_type': long_column_type,
    'padding': long_padding,
    'scale': (1.0, 6.0),
  },
  'appendix_long2': {
    'algos': [
      ('PPM',
       ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
        'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte', 'ref_PPMd']
       ),
      ('Reference', ['ref_SCSU', 'ref_gzip', 'ref_bzip2', 'ref_cmix', 'ref_paq8hp12']),
    ],
    'files': STANDARD_CORPUS,
    'column_type': long_column_type,
    'padding': long_padding,
    'scale': (1.0, 6.0),
    'files_last': True,
  },
  'dcc_master': {
    'algos': [
      ('LZW', ['lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']),
      ('PPM', ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
               'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte', 'ref_PPMd'])
    ],
    'font': 'Computer Modern',
    'files': DCC_SMALL_CAPPED_CORPUS,
  }
  # 'new_polya': {
  #   'algos': [
  #     ('Polya', ['none_polya_token_broken', 'none_polya_token',
  #                'none_polya_stoken_uniform_token', 'none_polya_stoken_uniform_byte']),
  #     ('LZW', ['lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token_broken', 'lzw_polya_token',
  #              'lzw_polya_stoken_uniform_token', 'lzw_polya_stoken_uniform_byte']),
  #     ('PPM', ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
  #              'ppm_training_group_opt_polya_token_broken', 'ppm_training_group_opt_polya_token',
  #              'ppm_training_group_opt_polya_stoken_uniform_token',
  #              'ppm_training_group_optut_polya_stoken_uniform_byte',
  #              'ppm_training_group_opt_polya_stoken_uniform_byte']),
  #   ],
  #   'files': STANDARD_CORPUS,
  # },
}

def longtable(prefix, files):
  return {
    prefix + '_long1': {
      'algos': [
        ('Static', ['none_uniform_byte', 'none_uniform_token']),
        ('Adaptive', ['crp_uniform_byte', 'crp_uniform_token', 'none_polya_token']),
        ('LZW', ['ref_compress', 'none_lzw_byte', 'lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']),
      ],
      'files': files,
      'column_type': long_column_type,
      'padding': long_padding,
      'scale': (1.0, 6.0),
    },
    prefix + '_long2': {
      'algos': [
        ('PPM',
         ['ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
          'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte', 'ref_PPMd']
         ),
        ('Reference', ['ref_SCSU', 'ref_gzip', 'ref_bzip2', 'ref_cmix', 'ref_paq8hp12']),
      ],
      'files': files,
      'column_type': long_column_type,
      'padding': long_padding,
      'scale': (1.0, 6.0),
      'files_last': True,
    },
  }
SCORE_TABLES.update(longtable('dcc_small_combined', DCC_SMALL_COMBINED_CORPUS))

## Score tables for presentation (one column for each file)
CANTERBURY_TEXT = [
  'canterbury/canterbury/alice29.txt',
  'canterbury/canterbury/asyoulik.txt',
  'canterbury/canterbury/cp.html',
  # 'canterbury/canterbury/fields.c',
  # 'canterbury/canterbury/grammar.lsp',
  'canterbury/canterbury/lcet10.txt',
  'canterbury/canterbury/plrabn12.txt',
  'canterbury/canterbury/xargs.1'
]

PRESENTATION_LZW_ALGOS = ['lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token']
PRESENTATION_PPM_ALGOS = [
  'ppm_training_group_opt_uniform_byte',
  'ppm_training_group_opt_uniform_token',
  'ppm_training_group_opt_polya_token',
  'ref_PPMd', 'ref_cmix', 'ref_paq8hp12']

SCORE_PRESENTATION = {
  'lzw_presentation_canterbury': {
    'algos': PRESENTATION_LZW_ALGOS,
    'files': {'canterbury_text': CANTERBURY_TEXT}
  },
  'lzw_presentation_unicode': {
    'algos': PRESENTATION_LZW_ALGOS,
    'files': {'single_language': STANDARD_CORPUS['single_language']}
  },
  'ppm_presentation_canterbury': {
    'algos': PRESENTATION_PPM_ALGOS,
    'files': {'canterbury_text': CANTERBURY_TEXT}
  },
  'ppm_presentation_unicode': {
    'algos': PRESENTATION_PPM_ALGOS,
    'files': {'single_language': STANDARD_CORPUS['single_language']}
  },
}

## Score summaries

BEST_ALGOS = ['ppm_training_group_opt_uniform_byte',
              'ppm_training_group_opt_polya_token',
              'ref_PPMd',
              'ref_cmix',
              'ref_paq8hp12']
SCORE_SUMMARIES = {
  'lzw_summary': {
      'algos': ['none_lzw_byte', 'lzw_uniform_byte', 'lzw_polya_token', 'ref_gzip', 'ref_bzip2'],
      'files': STANDARD_CORPUS_GROUPED, 
  },
  'ppm_summary': {
    'algos': BEST_ALGOS,
    'files': STANDARD_CORPUS_GROUPED, 
    #'files': {k: STANDARD_CORPUS_GROUPED[k] for k in ['ASCII', 'Unicode']},
  },
  'dcc_lzw_summary': {
    'algos': ['none_lzw_byte', 'lzw_uniform_byte', 'lzw_polya_token', 'ref_gzip', 'ref_bzip2'],
    'files': DCC_CORPUS_GROUPED,
  },
  'dcc_ppm_summary': {
    'algos': BEST_ALGOS,
    'files': DCC_CORPUS_GROUPED,
  },
  'dcc_master_summary': {
    'algos': ['lzw_uniform_byte', 'lzw_uniform_token', 'lzw_polya_token',
              'ppm_training_group_opt_uniform_byte', 'ppm_training_group_opt_uniform_token',
              'ppm_training_group_opt_polya_token', 'ppm_training_group_5_uniform_byte',
              'ref_PPMd'],
    'files': DCC_SMALL_CAPPED_CORPUS_GROUPED,
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
  (SCORE_PRESENTATION, 'score_presentation'),
  (SCORE_SUMMARIES, 'score_summary'),
  (PARAMETER_TABLES, 'parameter_table'),
  (RESOURCE_TABLES, 'resource_table'),
  (RESOURCE_FIGURES, 'resource_figure'),
])
TESTS['score_bar'] = {
  'type': 'score_bar',
  'width': 0.7, # relative to textwidth
  'granularity': 100,
}
TESTS['score_bar_presentation'] = {
  'type': 'score_bar',
  'width': 0.4,
  'granularity': 100,
}
