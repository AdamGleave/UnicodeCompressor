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

#!/usr/bin/env python3

import asciitable, argparse, csv, errno, filecmp, functools, os, pickle, sys
import celery

import benchmark.config_benchmark as config
import benchmark.general as general

def table_convert(original, result, table_type):
  if type(result) == str:
    # an error message, not a number
    return result

  if table_type == 'bits':
    return '{0:.4f}'.format(result / original * 8)
  elif table_type == 'per':
    return '{0:.1f}%'.format(result / original * 100)
  elif table_type == 'size':
    return general.human_readable_size(result)
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
  parser.add_argument('--rerun-errors', dest='rerun', action='store_true',
                      help='rerun compressors if it ended in an error')
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

  compressors = general.find_compressors(args['compressor'])
  if verbose:
    print("Using compressors: " + str(compressors))

  files = general.include_exclude_files(args['include'], args['exclude'])
  if verbose:
    print("Compressing files: " + str(files))

  results = {'Size': {}}
  for fname in files:
    input_fname = os.path.join(config.CORPUS_DIR, fname)
    results['Size'][fname] = os.path.getsize(input_fname)

  work = []
  for compressor_name in compressors:
    compressor, kwargs = config.COMPRESSORS[compressor_name]
    for fname in files:
      kwargs.update({'fname': fname, 'paranoia': True})
      work += [compressor.s(**kwargs)]

      if args['rerun']:
        import benchmark.tasks
        memoised = benchmark.tasks.memo(compressor.__wrapped__.func)
        if memoised.exists(kwargs=kwargs):
          cached = memoised.get(kwargs=kwargs)
          if type(cached) != int:
            print("Detected error '{0}' on {1} over {2}: rerunning"
                  .format(cached, compressor_name, fname))
            memoised.delete(kwargs=kwargs)

  async_res = celery.group(work)()
  result_list = async_res.get()

  i = 0
  for compressor_name in compressors:
    results[compressor_name] = {}
    for fname in files:
      results[compressor_name][fname] = result_list[i]
      i += 1

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
    tableResults['Size'] = [general.human_readable_size(results['Size'][fname]) for fname in files]

    for compressor in compressors:
      tableResults[compressor] = []
      for fname in files:
        res = table_convert(results['Size'][fname], results[compressor][fname], table_type)
        tableResults[compressor].append(res)
    asciitable.write(tableResults, sys.stdout, names=fieldnames, Writer=asciitable.FixedWidth)
