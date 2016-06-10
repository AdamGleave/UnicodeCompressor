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

package uk.ac.cam.cl.arg58.mphil.utf8

object UTF8 {
  final val CodePoints = Array(
    0x00 to     0x7f,
    0x0080 to   0x07ff,
    0x0800 to   0xffff,
    0x010000 to 0x10ffff
  )

  final val SurrogateCodePoints = 0xd800 to 0xdfff
}
