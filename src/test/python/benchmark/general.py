import glob, os, re, sys, math
import benchmark.config_benchmark as config

def sanitize_fname(fname):
  return fname.replace('/', '_')

def human_readable_helper(units, last_unit, factor, format):
  def f(s):
    for unit in units:
      if s < factor:
        return format(s, unit)
      s /= factor
    return format(s, last_unit)
  return f

def plain_format(s, unit):
  def format(s, unit):
    return '{0:3.1f}{1}'.format(s, unit)

def latex_format(s, unit):
  return r'\SI{' + '{0:3.1f}'.format(s) + r'}{' + unit + r'}'

human_readable_size = human_readable_helper(['B', 'KB', 'MB', 'GB'], 'TB', 1024.0, format)

human_readable_size_latex = human_readable_helper(
  [r'\byte', r'\kibi\byte', r'\mebi\byte', r'\gibi\byte'],
  r'\tebi\byte', 1024.0, latex_format
)

human_readable_time_latex = human_readable_helper([r'\milli\second'], r'\second',
                                                  1000.0, latex_format)

def find_files(patterns):
  acc = set()
  for p in patterns:
    matches = glob.glob(os.path.join(config.CORPUS_DIR, p))
    matches = filter(os.path.isfile, matches)
    matches = map(lambda p: os.path.relpath(p, config.CORPUS_DIR), matches)
    acc = acc.union(matches)
  return acc

def find_all_files():
  acc = set()
  for path, subdirs, files in os.walk(config.CORPUS_DIR):
    for name in files:
      abspath = os.path.join(path, name)
      relpath = os.path.relpath(abspath, config.CORPUS_DIR)
      acc.add(relpath)
  return acc

def include_exclude_files(include, exclude):
  if include:
    include_files = find_files(include)
  else:
    include_files = find_all_files()

  if exclude:
    exclude_files = find_files(exclude)
  else:
    exclude_files = []

  files = list(include_files.difference(exclude_files))
  files.sort()

  if files == []:
    print("ERROR: " + str(include) + " - " + str(exclude) + " does not match any files.")
    sys.exit(1)

  return files


def find_compressors(patterns):
  if patterns:
    patterns = list(map(re.compile, patterns))
    res = set()
    for k in config.COMPRESSORS.keys():
      for p in patterns:
        if p.match(k):
          res.add(k)
          break
  else:
    res = config.COMPRESSORS.key()
  res = list(res)
  res.sort()
  return res