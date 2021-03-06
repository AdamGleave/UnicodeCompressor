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
DISSERTATION_DIR = os.path.join(PROJECT_DIR, '..', 'dissertation')
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
sbt_classpath_cache = None
def find_sbt_classpath():
  global sbt_classpath_cache
  if not sbt_classpath_cache:
    classpath_cache = os.path.join(OUTPUT_DIR, 'classpath.cached')

    if os.path.exists(classpath_cache):
      with open(classpath_cache, 'r') as f:
        sbt_classpath_cache = f.read().strip()
    else:
      cwd = os.getcwd()
      os.chdir(PROJECT_DIR)
      res = subprocess.check_output(['sbt', 'export compile:fullClasspath'])
      os.chdir(cwd)

      sbt_classpath_cache = res.splitlines()[-1].decode("utf-8")

      with open(classpath_cache, 'w') as f:
        f.write(sbt_classpath_cache)
  return sbt_classpath_cache

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

# cmix
# Helper functions
CMIX_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'cmixv9', 'cmix')
def cmix(in_fname, out_fname, mode):
  args = [CMIX_EXECUTABLE]
  if mode == CompressionMode.compress:
    args.append('-c')
  else:
    args.append('-d')
  args += [in_fname, out_fname]
  return subprocess.Popen(args)

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
PPMd_EXECUTABLE = os.path.join(PROJECT_DIR, 'ext', 'ppmdj1', 'PPMd')
SCSU_ARGS = ['java', '-classpath', find_sbt_classpath(), 'uts6.SimpleCompressMain']
EXT_COMPRESSORS = {
  'bzip2': build_compressor(['bzip2', '-c', '--best'], ['-z'], ['-d']),
  'cmix': cmix,
  'compress': build_compressor(['compress', '-c'], [], ['-d']),
  'gzip': build_compressor(['gzip', '-c'], [], ['-d']),
  'LZMA': build_compressor(['lzma', '-c', '-9', '-e'], ['-z'], ['-d']),
  'paq8hp12': paq8hp12,
  'PPMd': build_compressor([PPMd_EXECUTABLE], ['e'], ['d']),
  'SCSU': build_compressor(SCSU_ARGS, ['compress'], ['decompress']),
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