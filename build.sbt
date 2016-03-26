lazy val root = (project in file(".")).
  settings(
    name := "mphil",
    version := "0.1",
    scalaVersion := "2.11.7",
    libraryDependencies += "org.scalactic" %% "scalactic" % "2.2.6",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test"
  )
  /* These were generating some errors in ScalaDoc, but not Javadoc! */
  sources in (Compile, doc) <<= sources in (Compile, doc) map { _.filterNot(_.getName startsWith "Urn") }
