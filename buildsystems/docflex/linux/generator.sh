#!/bin/sh

#----------------------------------------------------------------------
# Specify the location of Sun JRE/JDK 1.5.x or 1.4.x here
#----------------------------------------------------------------------
JRE=/usr/java/jdk1.5.0_05

#----------------------------------------------------------------------
# -Xmx option sets the maximum heap size allocated by JVM.
#
# Check this option when you need to process large quantities of data!
#
# Note: DocFlex/XML generator may be especially hungry for memory
# as it stores lots of temporary data in hash-tables in order to boost
# performance. However, according to our tests, even when processing
# such a heavy template application as XSDDoc on the Eclipse UML2 schemas,
# the largest we've seen so far, the 512 MB memory heap was quite enough.
#----------------------------------------------------------------------
JAVA_OPTIONS="-Xms256m -Xmx512m"

#----------------------------------------------------------------------
# DocFlex/XML Kit class path
#----------------------------------------------------------------------
JAVA_OPTIONS="${JAVA_OPTIONS} -cp lib/xml-apis.jar:lib/xercesImpl.jar:lib/docflex-xml-kit.jar"

#----------------------------------------------------------------------
# Running generator with all settings taken from generator.config file
#----------------------------------------------------------------------
${JRE}/bin/java ${JAVA_OPTIONS} com.docflex.xml.Generator
