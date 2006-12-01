#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

java -Xbootclasspath/p:$JAVA_HOME/lib/dsojre.jar -classpath $TC_CLASSPATH -Dtc.home=/Users/dferguson/main/tc -Djava.system.class.loader=com.tc.object.bytecode.DSOClassLoader dso.concurrency.ConcurrencyTester 100 1 2
