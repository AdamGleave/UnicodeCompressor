import os, subprocess, sys

from mode import CompressionMode

### General
NUM_THREADS = 2

### Directories

THIS_DIR = os.path.dirname(os.path.abspath(__file__)) # src/test/python/benchmark
PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(THIS_DIR))))
BIN_DIR = os.path.join(PROJECT_DIR, 'target', 'scala-2.11', 'classes')
CORPUS_DIR = os.path.join(PROJECT_DIR, 'src', 'test', 'resources', 'corpora')
OUTPUT_DIR = os.path.join(PROJECT_DIR, 'compressed')

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

def my_compressor(algorithm, params=None):
  def run_compressor(inFile, outFile, mode):
    classpath = sbt_classpath + ':' + BIN_DIR
    class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.Compressor'
    args = ['scala', '-J-Xms1024M', '-J-Xmx2048M',
            '-classpath', classpath, class_qualified, algorithm]
    if params:
      args += ['--params', params]
    compressor = build_compressor(args, ['compress'], ['decompress'])
    compressor(inFile, outFile, mode)
  return run_compressor

COMPRESSORS = {}
COMPRESSORS['ref_gzip'] = build_compressor(['gzip', '-c'], [], ['-d'])
COMPRESSORS['ref_bzip2'] = build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d'])
for x in ['none_uniform_token', 'none_categorical_token', 'none_polya_token',
          'crp_uniform_token', 'crp_categorical_token', 'crp_polya_token']:
  COMPRESSORS[x] = my_compressor(x)

for d in [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]:
  name = 'ppm{0}'.format(d)
  for suffix in ['uniform_token', 'uniform_byte', 'polya_token']:
    COMPRESSORS[name + '_' + suffix] = my_compressor('ppm_' + suffix, 'd={0}'.format(d))