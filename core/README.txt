JStyx library: Pure-Java implementation of the Styx protocol
http://jstyx.sourceforge.net

Primary author: Jon Blower
Licensed under the BSD license, Copyright University of Reading 2005

Build instructions
==================

This distribution should have come with a pre-built jar file (called
jstyx-x.xx.jar (where x.xx is the version number) in the dist directory of the
distribution.  If not, or if you want to build your own version of the library
(perhaps for your own version of Java; the supplied jar was build with J2SDK
1.4.2), change into the bin directory and enter "build" (should work on most
platforms).  Note that you need Apache Ant to build the source using this method.

To build the Javadoc documentation, change into the bin directory and enter
"build javadoc".

To clean up any class files, the jar library and javadoc, enter "build clean".

Note that the build.xml file is in the ant directory of the distribution. This
was placed here to avoid clashes with the build.xml file that NetBeans
automatically generates on my system in the root of the distribution.

The best place to get started is docs/tutorial.html.