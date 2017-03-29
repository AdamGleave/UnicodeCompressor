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
  rc('text.latex', preamble=r'\usepackage{abbreviations},\usepackage[binary-units=true]{siunitx}')
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

def set_rcs_onecol_square():
  set_rcs_common()
  rc('figure', figsize=(5.39, 5.39))

def set_rcs_twocol():
  set_rcs_common()
  rc('font', size=8)
  rc('legend', fontsize=6)
  # Slightly less than half of onecol so there's some whitespace between
  set_width(2.69)

def set_rcs_poster():
    set_rcs_common()
    set_width(9.5)
    rc('font', size=24)
    rc('legend', fontsize=24)

STYLES = {
  '1col': set_rcs_onecol,
  '1col_double': set_rcs_onecol_double,
  '1col_square': set_rcs_onecol_square,
  '2col': set_rcs_twocol,
  'poster': set_rcs_poster,
}

DEFAULT_STYLE = '1col'
