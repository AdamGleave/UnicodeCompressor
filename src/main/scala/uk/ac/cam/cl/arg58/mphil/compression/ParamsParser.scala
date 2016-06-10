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

class ParamsParser(keys: Array[String]) {
  def parse(params: String): Map[String, String] = params match {
    case "" => Map()
    case params => params
      .split(":")
      .map(s =>
        s.split("=") match {
          case Array(key, value) =>
            if (keys.contains(key)) {
              Some(key, value)
            } else {
              System.err.println("WARNING: Unrecognised key '" + key + "' in '" + s + "'")
              None
            }
          case _ =>
            System.err.println("WARNING: Illegal key-value pair '" + s + "'")
            None
        }
      )
      .collect { case Some(x) => x }
      .toMap
  }
}