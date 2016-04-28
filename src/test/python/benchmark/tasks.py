import filecmp, tempfile, os, subprocess
from redis import Redis
import memoize.redis

from benchmark.celery import app
from benchmark.mode import CompressionMode
import benchmark.config as config

db = Redis(host=config.REDIS_HOST, port=config.REDIS_PORT)
store = memoize.redis.wrap(db)
memo = memoize.Memoizer(store)

def build_compressor(standard_args, compress_args, decompress_args):
  def run_compressor(in_fname, out_fname, mode):
    args = standard_args.copy()
    if mode == CompressionMode.compress:
      args += compress_args
    else:
      args += decompress_args
    with open(in_fname, 'rb') as in_file:
      with open(out_fname, 'wb') as out_file:
        subprocess.check_call(args, stdin=in_file, stdout=out_file)
  return run_compressor

def find_sbt_classpath():
  classpath_cache = os.path.join(config.OUTPUT_DIR, 'classpath.cached')

  if os.path.exists(classpath_cache):
    with open(classpath_cache, 'r') as f:
      sbt_classpath = f.read().strip()
  else:
    cwd = os.getcwd()
    os.chdir(config.PROJECT_DIR)
    res = subprocess.check_output(['sbt', 'export compile:dependencyClasspath'])
    os.chdir(cwd)

    sbt_classpath = res.splitlines()[-1].decode("utf-8")

    with open(classpath_cache, 'w') as f:
      f.write(sbt_classpath)

  return sbt_classpath
sbt_classpath = find_sbt_classpath()

def build_my_compressor(base, algorithms=None):
  def run_compressor(in_file, out_file, mode):
    classpath = sbt_classpath + ':' + config.BIN_DIR
    class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.Compressor'
    starting_args = ['scala', '-J-Xms1024M', '-J-Xmx2048M',
                     '-classpath', classpath, class_qualified]
    ending_args = ['--base', base]
    if algorithms:
      ending_args += ['--model'] + algorithms
    compressor = build_compressor(starting_args,
                                  ['compress'] + ending_args,
                                  ['decompress'] + ending_args)
    return compressor(in_file, out_file, mode)
  return run_compressor

def compressed_filesize(compressor, input_fname, paranoia):
  with tempfile.NamedTemporaryFile(prefix='compression_en') as compressed:
    input_fname = os.path.join(config.CORPUS_DIR, input_fname)
    compressor(input_fname, compressed.name, CompressionMode.compress)
    if paranoia:
      with tempfile.NamedTemporaryFile(prefix='compression_de') as decompressed:
        compressor(compressed.name, decompressed.name, CompressionMode.decompress)
        if not filecmp.cmp(input_fname, decompressed.name):
          return "ERROR: decompressed file differs from original"
        else:
          return os.path.getsize(compressed.name)

#SOMEDAY: if result is in cache, quicker to hit DB locally rather than farming it out via Celery.
@app.task
@memo
def my_compressor(fname, paranoia, base, algorithms):
  compressor = build_my_compressor(base, algorithms)
  return compressed_filesize(compressor, fname, paranoia)

@app.task
@memo
def ext_compressor(fname, paranoia, name):
  standard_args, compressor_args, decompressor_args = config.EXT_COMPRESSORS[name]
  compressor = build_compressor(standard_args, compressor_args, decompressor_args)
  return compressed_filesize(compressor, fname, paranoia)
