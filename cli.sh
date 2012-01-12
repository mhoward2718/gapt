#!/usr/bin/env bash

# searches the actual $PATH, local directory and its development subpath for 
# the CLI jar package and runs the scala interpreter
# (preference is given to the development version)

export JARNAME="cli-1.0-SNAPSHOT-jar-with-dependencies.jar"
export SCP=""
export POSSIBLE_PATHS=`echo $PATH | sed s/:/\\ /g`

for I in ${POSSIBLE_PATHS} .; do
    if test -f $I/${JARNAME};
    then
	export SCP=$I
	break
    fi
done

for I in ${POSSIBLE_PATHS} .; do
    if test -f $I/cli/target/${JARNAME};
    then
	export SCP="$I/cli/target"
	break
    fi
done

if test _${SCP}_ = __ ; then 
    echo "Could not find ${JARNAME}!"
else
    echo found ${JARNAME} in ${SCP}!
    export JAVA_OPTS="-Xss2m -Xmx2g"
    #scala -classpath ${SCP}/${JARNAME} -i cli-script.scala
    $JAVA_HOME/bin/java -jar ${SCP}/${JARNAME}
    # workaround because jline somehow mixes up the terminal
    reset
fi