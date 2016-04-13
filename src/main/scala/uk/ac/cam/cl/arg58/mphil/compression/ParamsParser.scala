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