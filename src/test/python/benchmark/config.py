import os

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

PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
EXT_COMPRESSORS = {
  'compress': (['compress', '-c'], [], ['-d']),
  'bzip2': (['bzip2', '-c', '--best'], ['-z'], ['-d']),
  'gzip': (['gzip', '-c'], [], ['-d']),
  'LZMA': (['lzma', '-c', '-9', '-e'], ['-z'], ['-d']),
  'PPMd': ([PPMd_EXECUTABLE], ['e'], ['d']),
}

TIMEOUTS = { 'warn': 5, 'backoff': 2, 'hard': 60 }
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