import os, subprocess, re

from mode import CompressionMode

### General
NUM_THREADS = 2

### Directories

THIS_DIR = os.path.dirname(os.path.abspath(__file__)) # src/test/python/benchmark
PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(THIS_DIR))))
BIN_DIR = os.path.join(PROJECT_DIR, 'target', 'scala-2.11', 'classes')
CORPUS_DIR = os.path.join(PROJECT_DIR, 'src', 'test', 'resources', 'corpora')
OUTPUT_DIR = os.path.join(PROJECT_DIR, 'compressed')

### Local overrides (optional)
if os.path.exists(os.path.join(THIS_DIR, 'config_local.py')):
  from config_local import *

### Compression algorithms

def build_compressor(standard_args, compress_args, decompress_args):
  def run_compressor(inFile, outFile, mode):
    args = standard_args.copy()
    if mode == CompressionMode.compress:
      args += compress_args
    else:
      args += decompress_args
    subprocess.check_call(args, stdin=inFile, stdout=outFile)
  return run_compressor

def find_sbt_classpath():
  classpath_cache = os.path.join(OUTPUT_DIR, 'classpath.cached')

  sbt_classpath = None
  if os.path.exists(classpath_cache):
    with open(classpath_cache, 'r') as f:
      sbt_classpath = f.read().strip()
  else:
    cwd = os.getcwd()
    os.chdir(PROJECT_DIR)
    res = subprocess.check_output(['sbt', 'export compile:dependencyClasspath'])
    os.chdir(cwd)

    sbt_classpath = res.splitlines()[-1].decode("utf-8")

    with open(classpath_cache, 'w') as f:
      f.write(sbt_classpath)

  return sbt_classpath

sbt_classpath = find_sbt_classpath()

def my_compressor(base, algorithms=None):
  def run_compressor(inFile, outFile, mode):
    classpath = sbt_classpath + ':' + BIN_DIR
    class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.Compressor'
    starting_args = ['scala', '-J-Xms1024M', '-J-Xmx2048M',
                     '-classpath', classpath, class_qualified]
    ending_args = ['--base', base]
    if algorithms:
      ending_args += ['--model'] + algorithms
    compressor = build_compressor(starting_args,
                                  ['compress'] + ending_args,
                                  ['decompress'] + ending_args)
    compressor(inFile, outFile, mode)
  return run_compressor

COMPRESSORS = {}
COMPRESSORS['ref_gzip'] = build_compressor(['gzip', '-c'], [], ['-d'])
COMPRESSORS['ref_bzip2'] = build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d'])

PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
COMPRESSORS['ref_PPMd'] = build_compressor([PPMd_EXECUTABLE], ['e'], ['d'])

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
      COMPRESSORS[name] = my_compressor(prior_name, algo_config)
