import os, subprocess

from mode import CompressionMode

### General
NUM_THREADS = 4

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

def my_compressor(classname):
  class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.' + classname
  return build_compressor(['scala', '-cp', BIN_DIR, class_qualified], ['COMPRESS'], ['DECOMPRESS'])

COMPRESSORS = {}
COMPRESSORS['gzip'] = build_compressor(['gzip', '-c'], [], ['-d'])
COMPRESSORS['bzip2'] = build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d'])
COMPRESSORS['crputf8'] = my_compressor('CRPUTF8Compressor')