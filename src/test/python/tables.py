#!/usr/bin/env python3

import argparse, csv, os

import benchmark.config_tables as config
import benchmark.general as general

def load_benchmark(fname):
  with open(fname) as dataf:
    reader = csv.DictReader(dataf)
    res = {}
    for row in reader:
      file = row['File']
      filesize = int(row['Size'])
      del row['File']
      del row['Size']
      res[file] = {k: float(size) / filesize * 8 for k, size in row.items()}
    return res

def load_resources(fname):
  def update(d, file, compressor, val):
    by_file = d.get(file, {})
    by_compressor = by_file.get(compressor, [])
    by_compressor.append(val)
    by_file[compressor] = by_compressor
    d[file] = by_file
  with open(fname) as dataf:
    reader = csv.DictReader(dataf)
    runtimes = {}
    memories = {}
    for row in reader:
      file = row['file']
      compressor = row['compressor']
      runtime = float(row['runtime'])
      memory = int(row['memory'])

      update(runtimes, file, compressor, runtime)
      update(memories, file, compressor, memory)
  return (runtimes, memories)

def process_score_settings(raw):
  res = dict(raw)
  res['groups'] = None
  if type(res['algos'][0]) == tuple:
    algo_groups = res['algos']
    flat_algos = []
    groups = []
    for group, algos in algo_groups:
      flat_algos += algos
      groups.append((group, len(algos)))
    res['algos'] = flat_algos
    res['groups'] = groups
  return res

def generate_row(row):
  return ' & '.join(row) + r"\\"

def autoscale(settings, data):
  subset = []
  for _group, files in settings['files']:
    for f in files:
      for a in settings['algos']:
        subset.append(data[f][a])
  return min(subset), max(subset)

def effectiveness_format(x, is_best, scale, leading, fg_cm, bg_cm):
  smallest, largest = scale
  p = (x - smallest) / (largest - smallest)
  if p < 0 or p > 1:
    print("WARNING: value {0} outside {1}".format(x, scale))
  bg = bg_cm(p)
  fg = fg_cm(p)
  val = '{0:.3f}'.format(x)
  pad = leading + 4 - len(val)
  val = '\hspace{0.5em}' * pad + val
  if is_best:
    val = r'\textbf{' + val + r'}'
  return r'{\kern-0.35em\colorbox[rgb]{' + '{0},{1},{2}'.format(*bg) + r'}{\textcolor[rgb]{' + \
         '{0},{1},{2}'.format(*fg) + r'}{' + val + '}}\kern-0.35em}'

def generate_table(settings, data):
  if 'scale' in settings:
    scale = settings['scale']
  else:
    scale = autoscale(settings, data)

  res = []
  res.append(r'\setlength{\tabcolsep}{1ex}')

  algos = settings['algos']
  algo_cols = ''
  for algo in algos:
    algo_cols += config.get_column_type(algo)
  res.append(r'\begin{tabular}{l' + algo_cols + '}')
  res.append(r'\toprule')

  if settings['groups']:
    group_row = ['']
    for group, num_algos in settings['groups']:
      group_row.append(r'\multicolumn{' + str(num_algos) + r'}{c}{' + group + r'}')
    res.append(generate_row(group_row))

  algo_row = [r'\textbf{File}']
  for algo in algos:
    abbrev = config.ALGO_ABBREVIATIONS[algo]
    left_padding, right_padding = config.get_padding(algo)
    abbrev = left_padding + abbrev + right_padding
    algo_row.append(abbrev)
  res.append(generate_row(algo_row))
  res.append(r'\midrule')

  for file_group, files in settings['files']:
    for file in files:
      filedata = data[file]
      file_abbrev = config.FILE_ABBREVIATIONS[file]
      row = [file_abbrev]
      best = min([filedata[a] for a in algos])
      for algo in algos:
        efficiency = filedata[algo]
        is_best = efficiency == best
        leading = config.get_leading(algo)
        val = effectiveness_format(efficiency, is_best, scale, leading, config.FG_COLORMAP, config.BG_COLORMAP)
        row.append(val)
      res.append(generate_row(row))
    res.append(r'\addlinespace[0em]\midrule\addlinespace[0.1em]')

  res[-1] = r'\bottomrule'
  res.append(r'\end{tabular}')
  return '\n'.join(res)

def generate_score_table(raw_settings, data):
  settings = process_score_settings(raw_settings)
  return generate_table(settings, data)

def confidence_interval(vals, alpha):
  import numpy as np
  import scipy.stats
  mean = np.mean(vals)
  sd = np.std(vals)

  df = len(vals) - 1
  t = scipy.stats.t.ppf(alpha, df)

  return (mean, t*sd)

def generate_resource_table(settings, data):
  runtimes, memories = data
  col = settings['col']

  if col == 'runtime':
    resources = runtimes
    format = lambda s : general.human_readable_time_latex(s * 1000)
  elif col == 'memory':
    resources = memories
    format = lambda s : general.human_readable_size_latex(s * 1024.0)
  else:
    assert(False)

  files = settings['files']
  algos = settings['algos']

  res = []
  res.append(r'\begin{tabular}{l' + 'l'*len(algos) + '}')
  res.append(r'\toprule')

  file_row = [r'\textbf{File}']
  for file in files:
    abbrev = config.FILE_ABBREVIATIONS[file]
    file_row.append(abbrev)
  res.append(generate_row(file_row))
  res.append(r'\midrule')

  for algo in algos:
    algo_abbrev = config.ALGO_ABBREVIATIONS[algo]
    row = [algo_abbrev]

    for file in files:
      vals = resources[file][algo]
      mean, uncertainty = confidence_interval(vals, config.RESOURCE_ALPHA)
      row.append("${0} \pm {1}$".format(format(mean), format(uncertainty)))
    res.append(generate_row(row))
  res.append(r'\bottomrule')
  res.append(r'\end{tabular}')

  return '\n'.join(res)

verbose = False

def main():
  description = "Produce LaTeX tables"
  parser = argparse.ArgumentParser(description=description)
  parser.add_argument('--verbose', dest='verbose', action='store_true',
                      help='produce detailed output showing work performed.')
  parser.add_argument('tables', nargs='*',
                      help='list of tables to generate')

  args = vars(parser.parse_args())
  global verbose
  verbose = args['verbose']

  tables = args['tables']
  if not tables:
    tables = config.TABLES.keys()

  score_data = load_benchmark(config.BENCHMARK_INPUT)
  resource_data = load_resources(config.RESOURCES_INPUT)

  for table in tables:
    if verbose:
      print("Generating " + table)
    settings = config.TABLES[table]
    type = settings['type']

    if type == 'score':
      result = generate_score_table(settings, score_data)
    elif type == 'resource':
      result = generate_resource_table(settings, resource_data)
    else:
      print("WARNING: unknown table type '{0}', skipping".format(type))
      continue

    os.makedirs(config.LATEX_OUTPUT_DIR, exist_ok=True)
    with open(os.path.join(config.LATEX_OUTPUT_DIR, table + '.tex'), 'w') as out:
      out.write(result)

if __name__ == "__main__":
  main()
