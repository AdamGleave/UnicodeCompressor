/* Copyright (C) 2016, Adam Gleave

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package uk.ac.cam.cl.arg58.mphil.compression

/**
  * Created by adam on 29/04/16.
  */
object MultiCompressor {
  def main(args: Array[String]): Unit = {
    if (args.length != 0) {
      System.err.println("MultiCompressor does not take any arguments on the CLI.")
    }

    for (ln <- io.Source.stdin.getLines) {
      val c = new Compressor()
      val args = ln.split(" ")
      c.parse(args)
    }
  }
}
