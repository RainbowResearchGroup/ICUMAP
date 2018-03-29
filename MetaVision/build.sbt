name := "PapworthDatabase"

version := "1.0"

scalaVersion := "2.12.4"

resolvers ++= Seq(
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
    "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
    "clojars.org" at "http://clojars.org/repo"
)

// For accessing SQL-server databases
libraryDependencies ++= Seq(
    "org.scalikejdbc" %% "scalikejdbc"       % "3.0.0",
    "com.h2database"  %  "h2"                % "1.4.192",
    "ch.qos.logback"  %  "logback-classic"   % "1.1.7",
    "org.scalikejdbc" %% "scalikejdbc-config"  % "3.0.0"
)

libraryDependencies += "com.microsoft.sqlserver" % "mssql-jdbc" % "6.2.2.jre8"

// Matrix data structure
libraryDependencies ++= Seq(
    "org.scalanlp" %% "breeze" % "0.13.1",
    "org.scalanlp" %% "breeze-natives" % "0.13.1"
)

// For retrieving and parsing ICNARC's ICM condition codes.
libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "2.1.0"

// For exporting to json
libraryDependencies +=  "com.typesafe.play" %% "play-json" % "2.6.0-RC1"

classpathTypes += "dll"

mainClass in (Compile, run) := Some("uk.ac.cam.cl.as2388.icumap.database.extractors.ExtractEverything")