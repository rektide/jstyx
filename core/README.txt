JStyx library: Pure-Java implementation of the Styx protocol
http://jstyx.sourceforge.net

Primary author: Jon Blower
Licensed under the BSD license, Copyright University of Reading 2006

Getting started
===============

The best place to get started is the project website (http://jstyx.sf.net).
You can get a local copy of this website by downloading the docs zip file
(jstyx-x.x.x-doc.zip).

Getting help
============

The best places to get help are the project website (http://jstyx.sf.net) and
the jstyx-users mailing list (http://jstyx.sf.net/mail-lists.html).

Build instructions
==================

This distribution includes a pre-built jar file (called
jstyx-x.x.x.jar (where x.x.x is the version number) in the root directory of the
distribution.  If you want to build your own version of the library,
run "ant jar".  Note that you need Apache Ant to build the source using this method.

The current version of JStyx requires Java 1.4.2.  It is anticipated that future
releases will require Java 1.5 to allow use of the new security classes..

To build the Javadoc documentation, enter "ant javadoc".

To build the website and docs, enter "maven site" (you need Maven 1.0.2 for this).

To build the PDF documentation, enter "maven pdf" (you need to run "maven site" first).