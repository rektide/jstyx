#!/bin/sh

# Sets the classpath and command-line options for running a program
# with JStyx

case "`uname`" in
	CYGWIN*) _s=';'
	;;
	*) _s=':'
esac

CP=../conf
for i in `find ../lib -name '*.jar'`; do
    CP="$CP$_s$i"
done
for i in `find ../target -name '*.jar'`; do
    CP="$CP$_s$i"
done

OPTS=-Djava.protocol.handler.pkgs=uk.ac.rdg.resc.jstyx.client.protocol

java $OPTS -cp $CP $*
