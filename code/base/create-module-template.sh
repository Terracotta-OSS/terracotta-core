#!/bin/sh
if [ "$1" = "" ]; then
  echo "usage: $0 [module-name]"
  exit 0
fi

MODULE_NAME=$1

# directory structure
mkdir -p ${MODULE_NAME}
mkdir -p ${MODULE_NAME}/build.eclipse/ECLIPSE.IS.MESSEDUP
mkdir -p ${MODULE_NAME}/META-INF
mkdir -p ${MODULE_NAME}/src.resources
mkdir -p ${MODULE_NAME}/tests.base
mkdir -p ${MODULE_NAME}/tests.base.resources
mkdir -p ${MODULE_NAME}/tests.system

# build.xml
(cat <<'EOF'
<project name="*** MODULE NAME ***" xmlns:ivy="antlib:fr.jayasoft.ivy.ant">
  <!-- ================================= 
  target: resolve              
  ================================= -->
  <target name="resolve" description="--> retrieve dependencies with ivy">
        <ivy:resolve file="${basedir}/ivy.xml"/>
    <ivy:retrieve pattern="${ivy.dependencies.dir}/[artifact]-[revision].[ext]"/>
  </target>
</project>
EOF
) > ${MODULE_NAME}/build.xml

# ivy.xml
(cat <<'EOF'
<?xml version="1.0" encoding="ISO-8859-1"?>
<ivy-module version="1.0">
  <info organisation="ibiblio" module="*** MODULE NAME ***"/>
  <dependencies>
    <dependency name="*** DEPENDENCY NAME ***" rev="*** DEPENDENCY REVISION ***" org="terracotta"/>
  </dependencies>
</ivy-module>
EOF
) > ${MODULE_NAME}/ivy.xml

# build.properties
(cat <<'EOF'
source.. = src.resources/
bin.includes = META-INF/,\
               .,\
               src.resources/
src.includes = src.resources/,\
               META-INF/
EOF
) > ${MODULE_NAME}/build.properties

# tests.system.buildconfig
(cat <<'EOF'
include-dso-boot-jar=true
timeout=90
EOF
) > ${MODULE_NAME}/tests.system.buildconfig

# manifest file for bundle
(cat <<'EOF'
Manifest-Version: 1.0
Bundle-Category: terracotta
Bundle-Copyright: Terracotta, Inc. (c) 2003-2007
Bundle-Description: *** MODULE DESCRIPTION ***
Bundle-ManifestVersion: 2
Bundle-Name: *** MODULE NAME ***
Bundle-RequiredExecutionEnvironment: J2SE-1.5
Bundle-SymbolicName: *** MODULE SYMBOLIC NAME ***
Bundle-Vendor: Terracotta, Inc.
Bundle-Version: *** MODULE VERSION ***
EOF
) > ${MODULE_NAME}/META-INF/MANIFEST.MF

# terracotta.xml (tc-config.xml fragment)
(cat <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<!--

  All content copyright (c) 2007 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved

-->
<xml-fragment>
<!--

        Instrumentation config goes here.

-->
</xml-fragment>
EOF
) > ${MODULE_NAME}/src.resources/terracotta.xml

# pom.xml
(cat <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <parent>
    <artifactId>parent</artifactId>
    <groupId>org.terracotta</groupId>
    <version>2.3-SNAPSHOT</version>
  </parent>
  <modelVersion>4.0.0</modelVersion>
  <artifactId>*** MODULE NAME ***</artifactId>
  <dependencies>
    <dependency>
      <groupId>org.terracotta</groupId>
      <artifactId>legacy-test-tree</artifactId>
      <version>${tcVersion}</version>
      <type>test-jar</type>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.terracotta</groupId>
      <artifactId>simulator</artifactId>
      <version>${tcVersion}</version>
      <type>test-jar</type>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.terracotta</groupId>
      <artifactId>common</artifactId>
      <version>${tcVersion}</version>
      <type>test-jar</type>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.terracotta</groupId>
      <artifactId>dso-l1</artifactId>
    </dependency>
    <dependency>
      <groupId>*** OTHER MODULE DEPENDENCY ***</groupId>
      <artifactId>*** OTHER MODULE DEPENDENCY NAME ***</artifactId>
    </dependency>
  </dependencies>
</project>
EOF
) > ${MODULE_NAME}/pom.xml
