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

sbt_classpath = None

def my_compressor(algorithm):
  def run_compressor(inFile, outFile, mode):
    global sbt_classpath
    if sbt_classpath == None:
      cwd = os.getcwd()
      os.chdir(PROJECT_DIR)
      res = subprocess.check_output(['sbt', 'export compile:dependencyClasspath'])
      os.chdir(cwd)

      sbt_classpath = res.splitlines()[-1].decode("utf-8")
      print("Found classpath: {0}".format(sbt_classpath))
    classpath = sbt_classpath + ':' + BIN_DIR
    class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.Compressor'
    compressor = build_compressor(['scala', '-J-Xms1024M', '-J-Xmx2048M',
                                   '-classpath', classpath, class_qualified, algorithm],
                                  ['compress'], ['decompress'])
    compressor(inFile, outFile, mode)
  return run_compressor

COMPRESSORS = {}
COMPRESSORS['gzip'] = build_compressor(['gzip', '-c'], [], ['-d'])
COMPRESSORS['bzip2'] = build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d'])
for x in ['uniform_token', 'categorical_token', 'crp_uniform_token', 'crp_categorical_token',
          'ppm_uniform_byte', 'ppm_uniform_token']:
  COMPRESSORS[x] = my_compressor(x)