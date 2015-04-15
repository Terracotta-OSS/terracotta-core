#!/bin/sh
# 
# The contents of this file are subject to the Terracotta Public License Version
# 2.0 (the "License"); You may not use this file except in compliance with the
# License. You may obtain a copy of the License at 
# 
#      http://terracotta.org/legal/terracotta-public-license.
# 
# Software distributed under the License is distributed on an "AS IS" basis,
# WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
# the specific language governing rights and limitations under the License.
# 
# The Covered Software is Terracotta Platform.
# 
# The Initial Developer of the Covered Software is 
#     Terracotta, Inc., a Software AG company
#

if [ ! -d "$JAVA_HOME" ]; then
   echo "ERROR: JAVA_HOME must point to Java installation. Please see code-samples/README.txt for more information."
   echo "    $JAVA_HOME"
fi

# You May Need To Change this to your BigMemory installation root
workdir=`dirname $0`
workdir=`cd ${workdir} && pwd`
BIGMEMORY=${workdir}/../..



BIGMEMORY_CP=""



for jarfile in "$BIGMEMORY"/common/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done

for jarfile in "$BIGMEMORY"/code-samples/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done

for jarfile in "$BIGMEMORY"/apis/ehcache/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done


for jarfile in "$BIGMEMORY"/apis/toolkit/lib/*.jar; do
  BIGMEMORY_CP="$BIGMEMORY_CP":$jarfile
done


BIGMEMORY_CP="$BIGMEMORY_CP":

# Convert to Windows path if cygwin detected
# This allows users to use .sh scripts in cygwin
if [ `uname | grep CYGWIN` ]; then
  BIGMEMORY_CP=`cygpath -w -p "$BIGMEMORY_CP"`
fi

