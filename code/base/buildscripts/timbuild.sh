#!/bin/bash

if [ $# != 3 ]; then
    echo "usage $0 <tim list> <checkout dir> <dest dir>"
    echo ""
    echo "      <tim list>     - Either text file with list of TIMs or a tc-config.xml"
    echo "      <checkout dir> - Location of checked out forge projects"
    echo "      <dest dir>     - Destination location for TIMs"
    exit 2
fi

IN="$1"
CHECKOUT="$2"
DEST="$3"

if [ `uname | grep CYGWIN` ]; then
  IN=`cygpath -u $IN`
  CHECKOUT=`cygpath -u $CHECKOUT`
  DEST=`cygpath -u $DEST`
fi

echo "IN: $IN"
echo "CHECKOUT: $CHECKOUT"
echo "DEST: $DEST"

if [ ! -f "$IN" ]; then
    echo "ERROR: input file [$IN] does not exist"
    exit 1
fi

if [ ! -d "$CHECKOUT" ]; then
    echo "ERROR: checkout dir [$CHECKOUT] does not exist"
    exit 1
fi

if [ ! -e "$DEST" ]; then
    mkdir -p "$DEST"
fi 

if [ ! -d "$DEST" ]; then
    echo "ERROR: destination dir [$DEST] is not a directory"
    exit 1
fi

cd "$DEST"
DEST=`pwd`

if [ "${IN#*.}" == "xml" ]; then
    TIMS=`grep '<module name' $IN | sed -e 's/^.*<module name="//' | sed -e 's/".*//'`
else
    TIMS=`cat $IN | sed 's/^M$//'`
fi

for tim in ${TIMS[@]}; do
  echo "compiling $tim"

  # attempt to compensate for version number in TIM name
  if [ ! -d "$CHECKOUT/$tim" ]; then
      parts=( `echo $tim | awk -F'-' '{for(i=1; i<=NF; i++){print $i}}'` );
      trimmed="${parts[0]}"
      for (( i = 1 ; i < ${#parts[@]} - 1 ; i++ )); do
          trimmed="${trimmed}-${parts[$i]}"
      done 
      
      if [ -d "$CHECKOUT/$trimmed" ]; then
          tim=$trimmed
      fi
  fi
 
  if [ ! -d "$CHECKOUT/$tim" ]; then
      echo "ERROR: checkout dir [$CHECKOUT/$tim] does not exist"
      exit 1
  fi

  cd "$CHECKOUT/$tim"

  mvn clean install -DskipTests
  if [ $? -ne 0 ]; then
    echo "ERROR: error compiling $tim"
    exit 1
  fi

  echo "copying snapshots to dest dir"
  find . -type f -name *.jar -a ! -name '*-sources.jar' -a ! -name '*-tests.jar' -exec cp {} $DEST \;
  if [ $? -ne 0 ]; then
    echo "ERROR: error copying snapshots to $DEST"
    exit 1
  fi
done

exit 0
