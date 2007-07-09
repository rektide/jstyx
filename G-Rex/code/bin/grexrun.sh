#!/bin/sh

# ${0%/*} holds the directory containing this (grexrun.sh) script: see 
# http://www.codecomments.com/archive287-2004-5-194770.html

# Sets the classpath

case "`uname`" in
	CYGWIN*) _s=';'
	;;
	*) _s=':'
esac

CP=${0%/*}$_s${0%/*}/../build/web/WEB-INF/classes
for i in `find ${0%/*}/../build/web/WEB-INF/lib -name '*.jar'`; do
    CP="$CP$_s$i"
done

java -cp $CP uk.ac.rdg.resc.grex.client.cli.GRexRun $*