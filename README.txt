Now, I'm warning you here: this project contains a hand-written GUI in Java's swing framework. It ain't pretty. Best you not worry what's in that one monolithic main class.

Just delete the following from the pom.xml file:

<parent>
  <version>1.0.0</version>
  <groupId>uk.ac.susx.tag</groupId>
  <artifactId>tag-dist</artifactId>
  <relativePath>../tag-dist</relativePath>
</parent>

Then in the main directory, in a terminal session, type:

mvn clean compile assembly:single

A directory called "target" will be created, and in that directory a jar file called "interactive-dependency-parser-x.x.x-jar-with-dependencies" will be created (where the X's depict the version number)

To run the interactive parser, simply double click that jar file! (or however you like to run your jar files).

IMPORTANT NOTE

Ensure that you have the correct version of the tag classification framework that the pom.xml file requests (the classification framework is available at the same github page). Install the classification framework with a "mvn install" command (see instructions with that project).