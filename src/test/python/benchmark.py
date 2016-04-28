#!/usr/bin/env python3

import asciitable, argparse, csv, errno, filecmp, functools, os, pickle, re, sys, time
import celery

import benchmark.config_benchmark as config_benchmark
import benchmark.config as config
import benchmark.general

def find_compressors(patterns):
  patterns = list(map(re.compile, patterns))
  res = set()
  for k in config_benchmark.COMPRESSORS.keys():
    for p in patterns:
      if p.match(k):
        res.add(k)
        break
  return res

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
    return '{0:.4f}'.format(result / original * 8)
  elif table_type == 'per':
    return '{0:.1f}%'.format(result / original * 100)
  elif table_type == 'size':
    return human_readable_size(result)
  else:
    assert(False)

if __name__ == "__main__":
  description = 'Benchmark and test the correctness of compression algorithms.'
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
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
  run = (not args['invalidate']) or args['rerun']
  invalidate = args['invalidate'] or args['rerun']

  compressors = config_benchmark.COMPRESSORS.keys()
  if args['compressor']:
    compressors = find_compressors(args['compressor'])
  compressors = list(compressors)
  compressors.sort()

  files = benchmark.general.include_exclude_files(args['include'], args['exclude'])
  if verbose:
    print("Compressing files: " + str(files))

  results = {'Size': {}}
  for fname in files:
    input_fname = os.path.join(config.CORPUS_DIR, fname)
    results['Size'][fname] = os.path.getsize(input_fname)

  work = []
  for compressor_name in compressors:
    compressor, kwargs = config_benchmark.COMPRESSORS[compressor_name]
    for fname in files:
      work += [compressor.s(fname=fname, paranoia=True, **kwargs)]
  async_res = celery.group(work)()
  result_list = async_res.get()

  i = 0
  for compressor_name in compressors:
    results[compressor_name] = {}
    for fname in files:
      results[compressor_name][fname] = result_list[i]
      i += 1

  if run:
    fieldnames = ['File', 'Size'] + compressors

    if csv_fname:
      with open(csv_fname, 'w') as out:
        writer = csv.DictWriter(out, fieldnames=fieldnames)

        writer.writeheader()
        for fname in files:
          row = {'File': fname, 'Size': results['Size'][fname]}
          for compressor in compressors:
            row[compressor] = results[compressor][fname]
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
