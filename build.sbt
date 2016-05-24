lazy val ppmii = taskKey[Unit]("Compile PPMII")
lazy val paq8hp12 = taskKey[Unit]("Compile paq8hp12")
lazy val zpaq = taskKey[Unit]("Compile zpaq")
lazy val cmix = taskKey[Unit]("Compile cmix")

lazy val root = (project in file(".")).
  settings(
    name := "mphil",
    version := "0.1",
    scalaVersion := "2.11.7",
    libraryDependencies += "org.scalactic" %% "scalactic" % "2.2.6",
    libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.6" % "test",
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.4.0",
    resolvers += Resolver.sonatypeRepo("public"),
    unmanagedSourceDirectories in Compile += baseDirectory.value / "steinruecken/java",
    unmanagedSourceDirectories in Compile += baseDirectory.value / "ext/scsu",
    ppmii := { Process("make" :: Nil, baseDirectory.value / "ext/ppmdj1") ! },
    paq8hp12 := { Process("make" :: Nil, baseDirectory.value / "ext/paq8hp12") ! },
    zpaq := { Process("make" :: Nil, baseDirectory.value / "ext/zpaq6.42") ! },
    cmix := { Process("make" :: Nil, baseDirectory.value / "ext/cmixv9") ! }
  )
  /* These were generating some errors in ScalaDoc, but not Javadoc! */
  sources in (Compile, doc) <<= sources in (Compile, doc) map { _.filterNot(_.getName startsWith "Urn") }
  compile in Compile <<= compile in Compile dependsOn (ppmii in Compile, paq8hp12 in Compile, 
                                                       zpaq in Compile, cmix in Compile)
