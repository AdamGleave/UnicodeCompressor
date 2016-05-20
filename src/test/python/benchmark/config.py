import os, shutil, subprocess, tempfile

from benchmark.mode import CompressionMode

### Celery
REDIS_HOST = 'localhost'
REDIS_PORT = 6379
CELERY = {
  'BROKER_URL': 'amqp://guest@localhost:5672//',
  'CELERY_TRACK_STARTED': True,
  'CELERYD_PREFETCH_MULTIPLIER': 1,
  'CELERY_ACKS_LATE': True,
}

### Directories

THIS_DIR = os.path.dirname(os.path.abspath(__file__)) # src/test/python/benchmark
PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(THIS_DIR))))
BIN_DIR = os.path.join(PROJECT_DIR, 'target', 'scala-2.11', 'classes')
CORPUS_DIR = os.path.join(PROJECT_DIR, 'src', 'test', 'resources', 'corpora')
OUTPUT_DIR = os.path.join(PROJECT_DIR, 'experiments')
TABLE_DIR = os.path.join(OUTPUT_DIR, 'tables')

### Local overrides (optional)
if os.path.exists(os.path.join(THIS_DIR, 'config_local.py')):
  CELERY_OVERRIDES = {}
  from benchmark.config_local import *
  CELERY.update(CELERY_OVERRIDES)

CELERY['CELERY_RESULT_BACKEND'] = 'redis://{0}:{1}'.format(REDIS_HOST, REDIS_PORT)

### Compression algorithms

# Helper functions
def build_compressor(standard_args, compress_args, decompress_args):
  def run_compressor(in_fname, out_fname, mode):
    args = standard_args.copy()
    if mode == CompressionMode.compress:
      args += compress_args
    else:
      args += decompress_args
    with open(in_fname, 'rb') as in_file:
      with open(out_fname, 'wb') as out_file:
        return subprocess.Popen(args, stdin=in_file, stdout=out_file)
  return run_compressor

archive_temp_names = {}
def build_compressor_from_archive(compress_args, decompress_args):
  def run_compressor(in_fname, out_fname, mode):
    # Since an archive is being created, can't just compress to/from stdout (annoying).
    # To workaround this, will symlink a temporary filename to the input.
    # The output will then be decompressed to the same temporary filename.
    if mode == CompressionMode.compress: # compress
      temp_input = tempfile.mktemp(prefix='archive_in')
      os.symlink(in_fname, temp_input)
      os.unlink(out_fname)

      args = compress_args(in_fname, temp_input, out_fname)
      archive_temp_names[out_fname] = temp_input
    else: # decompress
      decompressed_fname = archive_temp_names.pop(in_fname)

      # HACK: make a symbolic link to where the output will be extracted!
      os.unlink(out_fname)
      os.unlink(decompressed_fname)
      os.symlink(decompressed_fname, out_fname)

      # now decompress
      args = decompress_args(in_fname)
    return subprocess.Popen(args)
  return run_compressor

# paq8hp12
PAQ8HP12_DIR = os.path.join(PROJECT_DIR, 'ext', 'paq8hp12')
PAQ8HP12_EXECUTABLE = os.path.join(PAQ8HP12_DIR, 'paq8hp12')
def paq8hp12_compress_args(in_fname, temp_input, out_fname):
  return [PAQ8HP12_EXECUTABLE, '-8', out_fname, temp_input]
def paq8hp12_decompress_args(in_fname):
  return [PAQ8HP12_EXECUTABLE, in_fname]
paq8hp12_helper = build_compressor_from_archive(paq8hp12_compress_args, paq8hp12_decompress_args)
def paq8hp12(in_fname, out_fname, mode):
  # have to run paq8hp12 from its source directory, as it contains data files there (this is crazy!)
  os.chdir(PAQ8HP12_DIR)
  return paq8hp12_helper(in_fname, out_fname, mode)

# zpaq
ZPAQ_DIR = os.path.join(PROJECT_DIR, 'ext', 'zpaq6.42')
ZPAQ_EXECUTABLE = os.path.join(ZPAQ_DIR, 'zpaq')
def zpaq(in_fname, out_fname, mode):
  if mode == CompressionMode.compress:
    os.symlink(out_fname, out_fname + ".zpaq")
    archive_temp_names[out_fname] = in_fname

    config_file = os.path.join(ZPAQ_DIR, 'max6')
    args = [ZPAQ_EXECUTABLE, '-m', 's10.0.5f'+config_file, 'a', out_fname, in_fname]
  else:
    original_fname = archive_temp_names.pop(in_fname)
    os.unlink(out_fname) # ZPAQ won't clobber existing files
    args = [ZPAQ_EXECUTABLE, 'x', in_fname, original_fname, '-to', out_fname]
  return subprocess.Popen(args)

# All compressors
CMIX_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'cmixv9', 'cmix')
PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
EXT_COMPRESSORS = {
  'bzip2': build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d']),
  'cmix': build_compressor([CMIX_EXECUTABLE], ['-c'], ['-d']),
  'compress': build_compressor(['compress', '-c'], [], ['-d']),
  'gzip': build_compressor(['gzip', '-c'], [], ['-d']),
  'LZMA': build_compressor(['lzma', '-c', '-9', '-e'], ['-z'], ['-d']),
  'paq8hp12': paq8hp12,
  'PPMd': build_compressor([PPMd_EXECUTABLE], ['e'], ['d']),
  'zpaq': zpaq,
}

# Timeouts
TIMEOUTS = { 'warn': 5, 'backoff': 2, 'hard': 600 }
def timeouts():
  waited_for = 0
  current_timeout = TIMEOUTS['warn']
  while waited_for < TIMEOUTS['hard']:
    waited_for += current_timeout
    next_timeout = min(current_timeout * TIMEOUTS['backoff'], TIMEOUTS['hard'] - waited_for)
    if waited_for == TIMEOUTS['hard']:
      yield (current_timeout, "waited for {0}s: timed out".format(waited_for))
    else:
      yield (current_timeout, "waited for {0}s, sleeping for {1}s more".format(waited_for, next_timeout))
      current_timeout = next_timeout