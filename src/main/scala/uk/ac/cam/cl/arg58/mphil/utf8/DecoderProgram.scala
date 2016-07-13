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

import java.io.{FileInputStream, InputStream}

object DecoderProgram {
  private def decode(in : InputStream, encode_after_decode : Boolean) = {
    val decoder = new DetailedUTF8Decoder(in)
    for (token <- decoder)
      if (encode_after_decode) {
        System.out.write(DetailedUTF8Encoder.tokenToBytes(token))
      } else {
        System.out.print(token)
      }
  }

  private def usage(): Unit = {
    System.err.println("USAGE: <IDENTITY|INFO> [fname1] [fname2] ...")
    System.exit(1)
  }

  def main(args: Array[String]) {
    if (args.length == 0) {
      usage()
    } else {
      var encode_after_decode : Boolean = args(0) match {
        case "IDENTITY" => true
        case "INFO" => false
        case _ => usage(); assert(false); false
      }
      val fnames = args.drop(1)
      if (fnames.length == 0) {
        decode(System.in, encode_after_decode)
      } else {
        for (fname <- fnames) {
          val in: InputStream = new FileInputStream(fname)
          decode(in, encode_after_decode)
        }
      }
    }
  }
}
