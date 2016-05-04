import os

### Celery
REDIS_HOST = 'localhost'
REDIS_PORT = 6379
CELERY = {
  'BROKER_URL': 'amqp://guest@localhost:5672//',
  'CELERY_TRACK_STARTED': True,
}

### Directories

THIS_DIR = os.path.dirname(os.path.abspath(__file__)) # src/test/python/benchmark
PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(THIS_DIR))))
BIN_DIR = os.path.join(PROJECT_DIR, 'target', 'scala-2.11', 'classes')
CORPUS_DIR = os.path.join(PROJECT_DIR, 'src', 'test', 'resources', 'corpora')
OUTPUT_DIR = os.path.join(PROJECT_DIR, 'experiments')

### Local overrides (optional)
if os.path.exists(os.path.join(THIS_DIR, 'config_local.py')):
  CELERY_OVERRIDES = {}
  from benchmark.config_local import *
  CELERY.update(CELERY_OVERRIDES)

#CELERY['CELERY_RESULT_BACKEND'] = 'redis://{0}:{1}'.format(REDIS_HOST, REDIS_PORT)
CELERY['CELERY_RESULT_BACKEND'] = CELERY['BROKER_URL'].replace('amqp', 'rpc')

### Compression algorithms

PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
EXT_COMPRESSORS = {
  'gzip': (['gzip', '-c'], [], ['-d']),
  'bzip2': (['bzip2', '-c', '--best'], ['-z'], ['-d']),
  'PPMd': ([PPMd_EXECUTABLE], ['e'], ['d']),
}
