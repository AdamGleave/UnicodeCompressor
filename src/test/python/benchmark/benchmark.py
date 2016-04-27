#!/usr/bin/env python3

import asciitable, argparse, csv, errno, filecmp, functools, os, pickle, re, sys, time
from multiprocessing import Pool

from mode import CompressionMode
import config_benchmark as config
import general

def find_compressors(patterns):
  patterns = list(map(re.compile, patterns))
  res = set()
  for k in config.COMPRESSORS.keys():
    for p in patterns:
      if p.match(k):
        res.add(k)
        break
  return res

def check_file(original, output_prefix):
  decompressed_fname = output_prefix + ".decompressed"
  if os.path.exists(decompressed_fname):
    if filecmp.cmp(original, decompressed_fname):
      os.unlink(decompressed_fname) # cleanup
    else:
      return "Error: decompressed file differs from original."
  with open(output_prefix + '.done', 'r+b') as cached_file:
    compression_time, decompression_time = pickle.load(cached_file)
    return (os.path.getsize(output_prefix + ".compressed"), compression_time, decompression_time)

def execute_compressor(compressor_name, input_fname, output_prefix):
  if verbose:
    print("Running {0} on {1}".format(compressor_name, input_fname))
    sys.stdout.flush()

  compressor = config.COMPRESSORS[compressor_name]
  compressed_fname = output_prefix + ".compressed"
  decompressed_fname = output_prefix + ".decompressed"
  cached_fname = output_prefix + ".done"

  with open(input_fname, 'r') as inFile:
    with open(compressed_fname, 'w') as outFile:
      start_time = time.time()
      compressor(inFile, outFile, CompressionMode.compress)
      end_time = time.time()
      compression_time = end_time - start_time

  with open(compressed_fname, 'r') as inFile:
    with open(decompressed_fname, 'w') as outFile:
      start_time = time.time()
      compressor(inFile, outFile, CompressionMode.decompress)
      end_time = time.time()
      decompression_time = end_time - start_time

  with open(cached_fname, 'w+b') as cachedFile:
    pickle.dump((compression_time, decompression_time), cachedFile)

  return check_file(input_fname, output_prefix)

def cleanup(prefix):
  try:
    os.unlink(prefix + ".compressed")
  except FileNotFoundError: # ignore error if removing files that don't exist
    pass
  try:
    os.unlink(prefix + ".decompressed")
  except FileNotFoundError:
    pass
  try:
    os.unlink(prefix + ".done")
  except FileNotFoundError:
    pass

def run_test(pool, results, compressor_name, fname):
  input_fname = os.path.join(config.CORPUS_DIR, fname)
  output_prefix = os.path.join(config.COMPRESSED_DIR, compressor_name, fname)
  output_dir = os.path.dirname(output_prefix)

  os.makedirs(output_dir, exist_ok=True)

  if invalidate:
    cleanup(output_prefix)

  if run:
    cached = os.path.exists(output_prefix + ".done")
    if cached:
      if verbose:
        print("{0} on {1}: cached".format(compressor_name, fname))

      results[compressor_name][fname] = check_file(input_fname, output_prefix)
    else:
      if verbose:
        print("{0} on {1}: testing".format(compressor_name, fname))
        sys.stdout.flush()

      def callback(compressor_name, fname, size):
        results[compressor_name][fname] = size
      def error_callback(compressor_name, fname, e):
        print("Error in {0} on {1}: {2}".format(compressor_name, fname, e), file=sys.stderr)
        results[compressor_name][fname] = "Exception: " + str(e)

      pool.apply_async(execute_compressor,
                       (compressor_name, input_fname, output_prefix),
                       callback=functools.partial(callback, compressor_name, fname),
                       error_callback=functools.partial(error_callback, compressor_name, fname))

def human_readable_size(s):
  for unit in ['B', 'KB', 'MB', 'GB']:
    if s < 1024.0:
      return '{0:3.1f}{1}'.format(s, unit)
    s /= 1024.0
  return '{0:3.1f}TB'.format(s)

def table_convert(original, result, table_type):
  if type(result) == str:
    # an error message, not a number
    return result

  if table_type == 'bits':
    compressed, _, _ = result
    return '{0:.4f}'.format(compressed / original * 8)
  elif table_type == 'per':
    compressed, _, _ = result
    return '{0:.1f}%'.format(compressed / original * 100)
  elif table_type == 'size':
    compressed, _, _ = result
    return human_readable_size(compressed)
  elif table_type == 'time':
    _, compression_time, decompression_time = result
    return "{0:.3f} ; {1:.3f}".format(compression_time, decompression_time)
  else:
    assert(False)

if __name__ == "__main__":
  description = 'Benchmark and test the correctness of compression algorithms.'
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('--threads', dest='threads', type=int, default=config.NUM_THREADS,
                      help='number of compression algorithms to run concurrently ' +
                           '(default: {0}).'.format(config.NUM_THREADS))
  parser.add_argument('--csv', dest='csv_fname', metavar='PATH',
                      help='write the results to a CSV file (default: ASCII table).')
  parser.add_argument('--table', dest='table_type', metavar='UNIT',
                      help='write the resutlts to stdout in an ASCII table in specified units, ' +
                      'one of bits (bits output/byte input), per (percentage output/input), ' +
                      'size (output file size), time (runtime in seconds) (default: bits).')
  parser.add_argument('--invalidate', dest='invalidate', action='store_true',
                      help='remove any cached results matching the pattern.')
  parser.add_argument('--rerun', dest='rerun', action='store_true',
                      help='run the test, even if there is a cached result; ' +
                           'equivalent to invalidating and then rerunning the program.')
  parser.add_argument('--include', dest='include', nargs='+',
                      help='paths which match the specified regex are included; ' +
                           'if unspecified, defaults to *.')
  parser.add_argument('--exclude', dest='exclude', nargs='+',
                      help='paths which match the specified regex are excluded.')
  parser.add_argument('compressor', nargs='*',
                      help='regex of compression algorithms to match; if unspecified, defaults to *.')

  args = vars(parser.parse_args())

  table_type = 'bits'
  csv_fname = None
  if args['csv_fname']:
    table_type = ''
    csv_fname = args['csv_fname']
  if args['table_type']:
    table_type = args['table_type']
    if table_type not in {"bits", "per", "size", "time"}:
      parser.error("Unrecognised table type: " + table_type)

  verbose = args['verbose']
  num_threads = args['threads']
  run = (not args['invalidate']) or args['rerun']
  invalidate = args['invalidate'] or args['rerun']

  compressors = config.COMPRESSORS.keys()
  if args['compressor']:
    compressors = find_compressors(args['compressor'])
  compressors = list(compressors)
  compressors.sort()

  files = general.include_exclude_files(args['include'], args['exclude'])
  if verbose:
    print("Compressing files: " + str(files))

  results = {'Size': {}}
  for fname in files:
    input_fname = os.path.join(config.CORPUS_DIR, fname)
    results['Size'][fname] = os.path.getsize(input_fname)

  with Pool(num_threads) as p:
    if verbose:
      print("Splitting work across {0} processes".format(num_threads))
    for compressor in compressors:
      results[compressor] = {}
      for fname in files:
        run_test(p, results, compressor, fname)

    p.close()
    p.join()

  if run:
    fieldnames = ['File', 'Size'] + compressors

    if csv_fname:
      with open(csv_fname, 'w') as out:
        writer = csv.DictWriter(out, fieldnames=fieldnames)

        writer.writeheader()
        for fname in files:
          row = {'File': fname, 'Size': results['Size'][fname]}
          for compressor in compressors:
            size, compression_time, decompression_time = results[compressor][fname]
            row[compressor + "_size"] = size
            row[compressor + "_ctime"] = compression_time
            row[compressor + "_dtime"] = decompression_time
          writer.writerow(row)
    if table_type:
      tableResults = {}
      tableResults['File'] = files
      tableResults['Size'] = [human_readable_size(results['Size'][fname]) for fname in files]

      for compressor in compressors:
        tableResults[compressor] = []
        for fname in files:
          res = table_convert(results['Size'][fname], results[compressor][fname], table_type)
          tableResults[compressor].append(res)
      asciitable.write(tableResults, sys.stdout, names=fieldnames, Writer=asciitable.FixedWidth)
