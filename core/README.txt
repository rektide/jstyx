JStyx library: Pure-Java implementation of the Styx protocol
http://jstyx.sourceforge.net

Primary author: Jon Blower
Licensed under the BSD license, Copyright University of Reading 2005

Build instructions
==================

THESE INSTRUCTIONS ARE SERIOUSLY OUT OF DATE - MODIFY!

This distribution should have come with a pre-built jar file (called
jstyx-x.xx.jar (where x.xx is the version number) in the target directory of the
distribution.  If not, or if you want to build your own version of the library,
change into the bin directory and enter "build" (should work on most
platforms).  Note that you need Apache Ant to build the source using this method.

You will need at least Java 5 (i.e. J2SDK 1.5) to build JStyx, as it uses
features only available from this version of Java (e.g. SSLEngine, also new 
language features).

To build the Javadoc documentation, change into the bin directory and enter
"build javadoc".

To clean up any class files, the jar library and javadoc, enter "build clean".

Note that the build.xml file is in the ant directory of the distribution. This
was placed here to avoid clashes with the build.xml file that NetBeans
automatically generates on my system in the root of the distribution.

The best place to get started is docs/tutorial.html.