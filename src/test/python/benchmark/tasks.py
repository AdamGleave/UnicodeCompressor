import filecmp, itertools, tempfile, os, subprocess
from redis import Redis
import memoize.redis

from benchmark.celery import app
from benchmark.mode import CompressionMode
import benchmark.config as config

import numpy as np
from scipy import optimize

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
  print(base, algorithms)
  def run_compressor(in_file, out_file, mode):
    classpath = sbt_classpath + ':' + config.BIN_DIR
    class_qualified = 'uk.ac.cam.cl.arg58.mphil.compression.Compressor'
    starting_args = ['scala', '-J-Xms1024M', '-J-Xmx2048M',
                     '-classpath', classpath, class_qualified]
    ending_args = ['--base', base]
    if algorithms:
      print(algorithms)
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

# The below functions aren't memoized, as the individual values are memoized
@app.task
def optimise_brute(fname, paranoia, prior, depth, alpha_range, beta_range, granularity):
  print("alpha: {0}, beta: {1}".format(alpha_range, beta_range))
  alphas = create_range(alpha_range[0], alpha_range[1], granularity)
  betas = create_range(beta_range[0], beta_range[1], granularity)
  grid = filter(legal_parameters, itertools.product(alphas, betas))

  work = [(fname, paranoia, prior, ['ppm:d={0}:a={1}:b={2}'.format(int(depth),a,b)])
          for (a,b) in grid]
  async_res = my_compressor.chunks(work, 10).apply_async()
  res = itertools.chain(*async_res.get())

  results = np.empty((granularity, granularity))
  for i, a in enumerate(alphas):
    for j, b in enumerate(betas):
      if legal_parameters((a, b)):
        results[i][j] = res.__next__()
      else:
        results[i][j] = np.inf

  beta_grid, alpha_grid = np.meshgrid(betas, alphas)
  optimum_i = np.argmin(results)
  optimum_alpha = alpha_grid.flatten()[optimum_i]
  optimum_beta = beta_grid.flatten()[optimum_i]
  min_val = results.flatten()[optimum_i]

  optimum = (optimum_alpha, optimum_beta), min_val
  evals = (alpha_grid, beta_grid), results

  return optimum, evals

def create_range(start, stop, N):
  return start + np.arange(0, N) * (stop - start) / (N - 1)

def range_around(point, old_range, factor):
  old_width = old_range[1] - old_range[0]
  new_width = old_width / factor
  # clamp to end points
  new_start = max(point - new_width / 2, old_range[0])
  new_stop = min(point + new_width / 2, old_range[1])
  return (new_start, new_stop)

def legal_parameters(x):
  (a, b) = x
  return a + b >= 0.01 # mathematically legal if >0, but set 0.01 threshold for numerical stability

@app.task
# N.B. don't memoize as the atomic results are memoized
def grid_search(fname, paranoia, prior, depth, iterations,
                shrink_factor, alpha_range, beta_range, granularity):
  res = optimise_brute(fname, paranoia, prior, depth, alpha_range, beta_range, granularity)

  if iterations <= 1:
    return (res[0], [res[1]])
  else:
    argmin, min = res[0]
    evals = res[1]
    
    new_alpha_range = range_around(argmin[0], alpha_range, shrink_factor)
    new_beta_range = range_around(argmin[0], beta_range, shrink_factor)

    new_res = grid_search(fname, paranoia, prior, depth, iterations - 1, shrink_factor,
                          new_alpha_range, new_beta_range, granularity)
    new_optimum, new_evals = new_res

    return (new_optimum, [evals] + new_evals)