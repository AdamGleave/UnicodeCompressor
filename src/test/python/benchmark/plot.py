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

import hashlib, os

import matplotlib
matplotlib.use('PDF')
import matplotlib.pyplot as plt
from matplotlib.backends.backend_pdf import PdfPages

import benchmark.general as general
import benchmark.config_plot as config

def set_style(name):
  global style_name, style_rc
  style_name = name
  style_rc = config.STYLES[style_name]

def new_figure():
  matplotlib.rcdefaults() # reset style
  style_rc() # apply new style
  return plt.figure()

def save_figure(fig, output_dir, fnames):
  if len(fnames) == 1:
    fig_fname = general.sanitize_fname(fnames[0])
  else:
    m = hashlib.md5()
    for fname in fnames:
      m.update(fname.encode('utf8'))
    fig_fname = "group-" + m.hexdigest()
  fig_fname += "_" + style_name + ".pdf"
  fig_dir = os.path.join(config.FIGURE_DIR, output_dir)
  os.makedirs(fig_dir, exist_ok=True)

  fig_path = os.path.join(fig_dir, fig_fname)
  with PdfPages(fig_path) as out:
    print("Writing figure to " + fig_path)
    out.savefig(fig)
  return fig_path

style_name = None
style_rc = None
set_style(config.DEFAULT_STYLE)