#!/usr/bin/env python3.4

import asciitable, argparse, csv, errno, filecmp, glob, functools, os, re, sys
from multiprocessing import Pool

from mode import CompressionMode
import config

def find_compressors(patterns):
  patterns = map(re.compile, patterns)
  res = set()
  for k in config.COMPRESSORS.keys:
    for p in patterns:
      if p.match(k):
        res.add(k)
        break
  return res

def find_files(patterns):
  acc = set()
  for p in patterns:
    matches = glob.glob(os.path.join(config.CORPUS_DIR, p))
    matches = filter(os.path.isfile, matches)
    matches = map(lambda p: os.path.relpath(p, config.CORPUS_DIR), matches)
    acc = acc.union(matches)
  return acc

def check_file(original, compressed, decompressed):
  if filecmp.cmp(original, decompressed):
    return os.path.getsize(compressed)
  else:
    return "Error: decompressed file differs from original."

def execute_compressor(compressor_name, input_fname, compressed_fname, decompressed_fname):
  compressor = config.COMPRESSORS[compressor_name]

  with open(input_fname, 'r') as inFile:
    with open(compressed_fname, 'w') as outFile:
      compressor(inFile, outFile, CompressionMode.compress)

  with open(compressed_fname, 'r') as inFile:
    with open(decompressed_fname, 'w') as outFile:
      compressor(inFile, outFile, CompressionMode.decompress)

  return check_file(input_fname, compressed_fname, decompressed_fname)

def run_test(pool, results, compressor_name, fname):
  input_fname = os.path.join(config.CORPUS_DIR, fname)
  output_prefix = os.path.join(config.OUTPUT_DIR, compressor_name, fname)
  output_dir = os.path.dirname(output_prefix)

  try:
    os.makedirs(output_dir)
  except OSError as e: # ignore error due to recreating directory
    if e.errno != errno.EEXIST:
      raise # if the error is due to something else, raise

  compressed_fname = output_prefix + ".compressed"
  decompressed_fname = output_prefix + ".decompressed"

  if invalidate:
    try:
      os.unlink(compressed_fname)
    except FileNotFoundError: # ignore error if removing files that don't exist
      pass
    try:
      os.unlink(decompressed_fname)
    except FileNotFoundError:
      pass

  if run:
    compressed_exists = os.path.exists(compressed_fname)
    decompressed_exists = os.path.exists(decompressed_fname)
    if compressed_exists and decompressed_exists:
      if verbose:
        print("{0} on {1}: cached".format(compressor_name, fname))

      results[compressor_name][fname] = check_file(input_fname,
                                                   compressed_fname, decompressed_fname)
    else:
      if compressed_exists != decompressed_exists:
        print("{0} on {1}: WARNING: previous run must have aborted, rerunning"
              .format(compressor_name, fname))
      else:
        if verbose:
          print("{0} on {1}: testing".format(compressor_name, fname))

      def callback(compressor_name, fname, size):
        results[compressor_name][fname] = size
      def error_callback(compressor_name, fname, e):
        print("Error in {0} on {1}: {2}".format(compressor_name, fname, e), file=sys.stderr)
        results[compressor_name][fname] = "Exception: " + str(e)

      pool.apply_async(execute_compressor,
                       (compressor_name, input_fname, compressed_fname, decompressed_fname),
                       callback=functools.partial(callback, compressor_name, fname),
                       error_callback=functools.partial(error_callback, compressor_name, fname))

    return os.path.getsize(compressed_fname)

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
  parser.add_argument('--invalidate', dest='invalidate', action='store_true',
                      help='remove any cached results matching the pattern.')
  parser.add_argument('--rerun', dest='rerun', action='store_true',
                      help='run the test, even if there is a cached result; ' +
                           'equivalent to invalidating and then rerunning the program.')
  parser.add_argument('--path', dest='path', nargs='+',
                      help='regex of paths to match; if unspecified, defaults to *.')
  parser.add_argument('compressor', nargs='*',
                      help='regex of compression algorithms to match; if unspecified, defaults to *.')

  args = vars(parser.parse_args())

  verbose = args['verbose']
  num_threads = args['threads']
  run = (not args['invalidate']) or args['rerun']
  invalidate = args['invalidate'] or args['rerun']

  compressors = config.COMPRESSORS.keys()
  if args['compressor']:
    compressors = find_compressors(args['compressor'])

  path_patterns = args['path'] if args['path'] else ['**/*']
  files = find_files(path_patterns)

  # TODO: summarise results in some nice way?

  results = {'Original': {}}
  for fname in files:
    input_fname = os.path.join(config.CORPUS_DIR, fname)
    results['Original'][fname] = os.path.getsize(input_fname)

  with Pool(num_threads) as p:
    if verbose:
      print("Splitting work across {0} processes".format(num_threads))
    for col in compressors:
      results[col] = {}
      for fname in files:
        run_test(p, results, col, fname)

    p.close()
    p.join()

  if run:
    cols = ['Original'] + list(compressors)
    fieldnames = ['File'] + cols

    if args['csv_fname']:
      with open(args['csv_fname'], 'w') as out:
        writer = csv.DictWriter(out, fieldnames=fieldnames)

        writer.writeheader()
        for fname in files:
          row = {'File': fname}
          for col in cols:
            row[col] = results[col][fname]
          writer.writerow(row)
    else:
      tableResults = {}
      tableResults['File'] = list(files)
      for col in cols:
        tableResults[col] = []
        for fname in files:
          tableResults[col].append(str(results[col][fname]))
      asciitable.write(tableResults, sys.stdout, names=fieldnames, Writer=asciitable.FixedWidth)