resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Build Version Plugins
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.5.0")
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.5.0")
addSbtPlugin("org.scoverage" %% "sbt-coveralls" % "1.1.0")