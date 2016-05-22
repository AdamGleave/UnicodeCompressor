import os, subprocess
from matplotlib import rc

from benchmark.config import *

FIGURE_DIR = os.path.join(OUTPUT_DIR, 'figures')

## Appearance
def set_rcs_common():
  rc('font',**{'family':'serif', 'serif':['Palatino']})
  rc('text', usetex=True)

  texinputs = subprocess.check_output(['kpsexpand', '$TEXINPUTS'])
  texinputs = DISSERTATION_DIR + ':' + texinputs.decode('utf-8').strip()
  os.putenv('TEXINPUTS', texinputs)
  rc('text.latex', preamble=r'\usepackage{algoabbrev},\usepackage[binary-units=true]{siunitx}')
  rc('figure', autolayout=True)

  rc('font', size=10)
  rc('legend', fontsize=8)

def set_width(width, aspect_ratio=4/3.0):
  rc('figure', figsize=(width, width/aspect_ratio))

def set_rcs_onecol():
  set_rcs_common()
  # My textwidth is 137.06772mm, or just over 5.39 in
  set_width(5.39)

# A little smaller, so they can comfortably fit stacked
def set_rcs_onecol_double():
  set_rcs_common()
  set_width(4.5)

def set_rcs_twocol():
  set_rcs_common()
  rc('font', size=8)
  rc('legend', fontsize=6)
  # Slightly less than half of onecol so there's some whitespace between
  set_width(2.69)

STYLES = {
  '1col': set_rcs_onecol,
  '1col_double': set_rcs_onecol_double,
  '2col': set_rcs_twocol,
}

DEFAULT_STYLE = '1col'