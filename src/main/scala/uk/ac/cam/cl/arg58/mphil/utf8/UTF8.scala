package uk.ac.cam.cl.arg58.mphil.utf8

/**
  * Created by adam on 11/03/16.
  */
object UTF8 {
  val CodePoints = Array(    0x00 to     0x7f,
    0x0080 to   0x07ff,
    0x0800 to   0xffff,
    0x010000 to 0x10ffff)

  val SurrogateCodePoints = 0xd800 to 0xdfff
}
