#!/bin/sh

[ $# -eq 1 ] || { echo Usage - $0 new_version >&2 ; exit 1 ; }

sed s/__JETTY_VERSION__/$1/g << "_EOF_" | patch -p0
Index: pom.xml
===================================================================
--- pom.xml	(revision 3630)
+++ pom.xml	(working copy)
@@ -9,7 +9,7 @@
   <artifactId>project</artifactId>
   <packaging>pom</packaging>
   <name>Jetty Server Project</name>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <url>http://jetty.mortbay.org</url>  
   <licenses>
     <license>
Index: extras/jboss/pom.xml
===================================================================
--- extras/jboss/pom.xml	(revision 3630)
+++ extras/jboss/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/gwt/pom.xml
===================================================================
--- extras/gwt/pom.xml	(revision 3630)
+++ extras/gwt/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/threadpool/pom.xml
===================================================================
--- extras/threadpool/pom.xml	(revision 3630)
+++ extras/threadpool/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/spring/pom.xml
===================================================================
--- extras/spring/pom.xml	(revision 3630)
+++ extras/spring/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/win32service/pom.xml
===================================================================
--- extras/win32service/pom.xml	(revision 3630)
+++ extras/win32service/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/sslengine/pom.xml
===================================================================
--- extras/sslengine/pom.xml	(revision 3630)
+++ extras/sslengine/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/servlet-tester/pom.xml
===================================================================
--- extras/servlet-tester/pom.xml	(revision 3630)
+++ extras/servlet-tester/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/ajp/pom.xml
===================================================================
--- extras/ajp/pom.xml	(revision 3630)
+++ extras/ajp/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/setuid/modules/java/pom.xml
===================================================================
--- extras/setuid/modules/java/pom.xml	(revision 3630)
+++ extras/setuid/modules/java/pom.xml	(working copy)
@@ -3,7 +3,7 @@
     <parent>
         <artifactId>jetty-setuid</artifactId>
         <groupId>org.mortbay.jetty</groupId>
-        <version>6.1-SNAPSHOT</version>
+        <version>__JETTY_VERSION__</version>
         <relativePath>../../pom.xml</relativePath>
     </parent>
     <modelVersion>4.0.0</modelVersion>
Index: extras/setuid/modules/native/pom.xml
===================================================================
--- extras/setuid/modules/native/pom.xml	(revision 3630)
+++ extras/setuid/modules/native/pom.xml	(working copy)
@@ -5,7 +5,7 @@
     <parent>
         <artifactId>jetty-setuid</artifactId>
         <groupId>org.mortbay.jetty</groupId>
-        <version>6.1-SNAPSHOT</version>
+        <version>__JETTY_VERSION__</version>
         <relativePath>../../pom.xml</relativePath>
     </parent>
     <modelVersion>4.0.0</modelVersion>
Index: extras/setuid/pom.xml
===================================================================
--- extras/setuid/pom.xml	(revision 3630)
+++ extras/setuid/pom.xml	(working copy)
@@ -3,7 +3,7 @@
     <parent>
         <artifactId>project</artifactId>
         <groupId>org.mortbay.jetty</groupId>
-        <version>6.1-SNAPSHOT</version>
+        <version>__JETTY_VERSION__</version>
         <relativePath>../../pom.xml</relativePath>
     </parent>
     <modelVersion>4.0.0</modelVersion>
Index: extras/xbean/pom.xml
===================================================================
--- extras/xbean/pom.xml	(revision 3630)
+++ extras/xbean/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: extras/client/pom.xml
===================================================================
--- extras/client/pom.xml	(revision 3630)
+++ extras/client/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </parent>
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
Index: project-website/project-site/src/site/site.xml
===================================================================
--- project-website/project-site/src/site/site.xml	(revision 3630)
+++ project-website/project-site/src/site/site.xml	(working copy)
@@ -5,7 +5,7 @@
   <skin>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>jetty-skin</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </skin>
   <bannerLeft>
     <name>Jetty</name>
Index: project-website/project-site/pom.xml
===================================================================
--- project-website/project-site/pom.xml	(revision 3630)
+++ project-website/project-site/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project-website</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
@@ -11,7 +11,7 @@
   <artifactId>project-site</artifactId>
   <packaging>pom</packaging>
   <name>Jetty Site</name>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <build>
     <plugins>    
       <plugin>
Index: project-website/project-skin/pom.xml
===================================================================
--- project-website/project-skin/pom.xml	(revision 3630)
+++ project-website/project-skin/pom.xml	(working copy)
@@ -7,7 +7,7 @@
   </parent>
   <groupId>org.mortbay.jetty</groupId>
   <artifactId>jetty-skin</artifactId>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <name>Maven Jetty Site Skin</name>
   <description>Maven Jetty Site Skin</description>
 </project>
Index: project-website/pom.xml
===================================================================
--- project-website/pom.xml	(revision 3630)
+++ project-website/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
@@ -11,7 +11,7 @@
   <artifactId>project-website</artifactId>
   <packaging>pom</packaging>
   <name>Jetty WebSite</name>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <modules>
     <module>project-skin</module>
     <module>project-site</module>
Index: modules/plus/pom.xml
===================================================================
--- modules/plus/pom.xml	(revision 3630)
+++ modules/plus/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jsp-2.0/pom.xml
===================================================================
--- modules/jsp-2.0/pom.xml	(revision 3630)
+++ modules/jsp-2.0/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jsp-2.1/pom.xml
===================================================================
--- modules/jsp-2.1/pom.xml	(revision 3630)
+++ modules/jsp-2.1/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/annotations/pom.xml
===================================================================
--- modules/annotations/pom.xml	(revision 3630)
+++ modules/annotations/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/servlet-api-2.5/pom.xml
===================================================================
--- modules/servlet-api-2.5/pom.xml	(revision 3630)
+++ modules/servlet-api-2.5/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/start/pom.xml
===================================================================
--- modules/start/pom.xml	(revision 3630)
+++ modules/start/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jetty/pom.xml
===================================================================
--- modules/jetty/pom.xml	(revision 3630)
+++ modules/jetty/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jspc-maven-plugin/pom.xml
===================================================================
--- modules/jspc-maven-plugin/pom.xml	(revision 3630)
+++ modules/jspc-maven-plugin/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jsp-api-2.0/pom.xml
===================================================================
--- modules/jsp-api-2.0/pom.xml	(revision 3630)
+++ modules/jsp-api-2.0/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/jsp-api-2.1/pom.xml
===================================================================
--- modules/jsp-api-2.1/pom.xml	(revision 3630)
+++ modules/jsp-api-2.1/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/maven-plugin/pom.xml
===================================================================
--- modules/maven-plugin/pom.xml	(revision 3630)
+++ modules/maven-plugin/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/util5/pom.xml
===================================================================
--- modules/util5/pom.xml	(revision 3630)
+++ modules/util5/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/html/pom.xml
===================================================================
--- modules/html/pom.xml	(revision 3630)
+++ modules/html/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/naming/pom.xml
===================================================================
--- modules/naming/pom.xml	(revision 3630)
+++ modules/naming/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/management/pom.xml
===================================================================
--- modules/management/pom.xml	(revision 3630)
+++ modules/management/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: modules/util/pom.xml
===================================================================
--- modules/util/pom.xml	(revision 3630)
+++ modules/util/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/tests/pom.xml
===================================================================
--- examples/tests/pom.xml	(revision 3630)
+++ examples/tests/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/spring-ebj3-demo/pom.xml
===================================================================
--- examples/spring-ebj3-demo/pom.xml	(revision 3630)
+++ examples/spring-ebj3-demo/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </parent>
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
Index: examples/test-jaas-webapp/pom.xml
===================================================================
--- examples/test-jaas-webapp/pom.xml	(revision 3630)
+++ examples/test-jaas-webapp/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/embedded/pom.xml
===================================================================
--- examples/embedded/pom.xml	(revision 3630)
+++ examples/embedded/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/test-annotations/pom.xml
===================================================================
--- examples/test-annotations/pom.xml	(revision 3630)
+++ examples/test-annotations/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/test-webapp/pom.xml
===================================================================
--- examples/test-webapp/pom.xml	(revision 3630)
+++ examples/test-webapp/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: examples/test-jndi-webapp/pom.xml
===================================================================
--- examples/test-jndi-webapp/pom.xml	(revision 3630)
+++ examples/test-jndi-webapp/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/jetty-ant/pom.xml
===================================================================
--- contrib/jetty-ant/pom.xml	(revision 1197)
+++ contrib/jetty-ant/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </parent>
 	<modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
Index: contrib/jetty-ant-demo/build.xml
===================================================================
--- contrib/jetty-ant-demo/build.xml	(revision 1197)
+++ contrib/jetty-ant-demo/build.xml	(working copy)
@@ -1,9 +1,9 @@
 <project name="Test application" basedir=".">
   <property name="project.outputDirectory" value="target" />
-  <property name="project.version" value="6.1-SNAPSHOT" />
+  <property name="project.version" value="__JETTY_VERSION__" />
   <property name="M2_REPO" value="${user.home}/.m2/repository/" />
 
-  <property name="jetty.version" value="6.1-SNAPSHOT" />
+  <property name="jetty.version" value="__JETTY_VERSION__" />
 
   <path id="jetty.plugin.classpath">
     <fileset dir="${M2_REPO}">
Index: contrib/debian/src/main/debian/changelog
===================================================================
--- contrib/debian/src/main/debian/changelog	(revision 1197)
+++ contrib/debian/src/main/debian/changelog	(working copy)
@@ -1,6 +1,6 @@
-jetty6 (6.1-SNAPSHOT-1) unstable; urgency=low
+jetty6 (__JETTY_VERSION__-1) unstable; urgency=low
 
-  * Initial Release for jetty 6
+  * Initial Release for jetty 6.1.12
 
  -- Greg Wilkins <gregw@mortbay.com>  Thu, 08 Nov 2007 18:23:39 +1100
 
Index: contrib/debian/pom.xml
===================================================================
--- contrib/debian/pom.xml	(revision 1197)
+++ contrib/debian/pom.xml	(working copy)
@@ -3,14 +3,14 @@
     <parent>
         <artifactId>project</artifactId>
         <groupId>org.mortbay.jetty</groupId>
-	<version>6.1-SNAPSHOT</version>
+	<version>__JETTY_VERSION__</version>
         <relativePath>../../pom.xml</relativePath>
     </parent>
 
     <modelVersion>4.0.0</modelVersion>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>jetty-deb</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <name>Jetty Deb Packages</name>
     <packaging>pom</packaging>
 
Index: contrib/wadi/pom.xml
===================================================================
--- contrib/wadi/pom.xml	(revision 1197)
+++ contrib/wadi/pom.xml	(working copy)
@@ -2,13 +2,13 @@
   <!--parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent-->
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
   <artifactId>jetty-wadi-session-manager</artifactId>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <name>Wadi - Jetty Session Cache</name>
   <url>http://jetty.mortbay.org</url>
   <licenses>
Index: contrib/jetty-deb/pom.xml
===================================================================
--- contrib/jetty-deb/pom.xml	(revision 1197)
+++ contrib/jetty-deb/pom.xml	(working copy)
@@ -3,14 +3,14 @@
     <parent>
         <artifactId>project</artifactId>
         <groupId>org.mortbay.jetty</groupId>
-        <version>6.1-SNAPSHOT</version>
+        <version>__JETTY_VERSION__</version>
         <relativePath>../../pom.xml</relativePath>
     </parent>
 
     <modelVersion>4.0.0</modelVersion>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>jetty-deb</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <name>Jetty Deb Packages</name>
     <packaging>pom</packaging>
 </project>
Index: contrib/terracotta/pom.xml
===================================================================
--- contrib/terracotta/pom.xml	(revision 1197)
+++ contrib/terracotta/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </parent>
 
   <modelVersion>4.0.0</modelVersion>
Index: contrib/j2se6/pom.xml
===================================================================
--- contrib/j2se6/pom.xml	(revision 1197)
+++ contrib/j2se6/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
   </parent>
 	<modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
Index: contrib/jetty-rewrite-handler/pom.xml
===================================================================
--- contrib/jetty-rewrite-handler/pom.xml	(revision 1197)
+++ contrib/jetty-rewrite-handler/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/grizzly/pom.xml
===================================================================
--- contrib/grizzly/pom.xml	(revision 1197)
+++ contrib/grizzly/pom.xml	(working copy)
@@ -3,7 +3,7 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/cometd/demo/pom.xml
===================================================================
--- contrib/cometd/demo/pom.xml	(revision 1197)
+++ contrib/cometd/demo/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>cometd-project</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/cometd/install/pom.xml
===================================================================
--- contrib/cometd/install/pom.xml	(revision 1197)
+++ contrib/cometd/install/pom.xml	(working copy)
@@ -2,13 +2,13 @@
   <parent>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>cometd-project</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
   <groupId>org.mortbay.jetty</groupId>
   <artifactId>cometd-install</artifactId>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <packaging>pom</packaging>
   <name>Cometd :: Jetty :: Install</name>
   <url>http://www.cometd.com</url>
Index: contrib/cometd/pom.xml
===================================================================
--- contrib/cometd/pom.xml	(revision 1197)
+++ contrib/cometd/pom.xml	(working copy)
@@ -3,12 +3,12 @@
   <parent>
     <artifactId>project</artifactId>
     <groupId>org.mortbay.jetty</groupId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../../pom.xml</relativePath>
   </parent>
   <groupId>org.mortbay.jetty</groupId>
   <artifactId>cometd-project</artifactId>
-  <version>6.1-SNAPSHOT</version>
+  <version>__JETTY_VERSION__</version>
   <packaging>pom</packaging>
   <name>Cometd :: Jetty</name>
   <url>http://www.cometd.com</url>
Index: contrib/cometd/bayeux/pom.xml
===================================================================
--- contrib/cometd/bayeux/pom.xml	(revision 1197)
+++ contrib/cometd/bayeux/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>cometd-project</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/cometd/client/bin/run.sh
===================================================================
--- contrib/cometd/client/bin/run.sh	(revision 1197)
+++ contrib/cometd/client/bin/run.sh	(working copy)
@@ -1,2 +1,2 @@
 #!/bin/sh
-java -Xmx2048M -cp target/classes:target/test-classes/:../bayeux/target/classes/:../../../lib/cometd/cometd-api-1.0-SNAPSHOT.jar:../../../lib/ext/jetty-client-6.1-SNAPSHOT.jar:../../../lib/ext/jetty-sslengine-6.1-SNAPSHOT.jar:../../../lib/jre1.5/jetty-util5-6.1-SNAPSHOT.jar:../../../lib/jetty-util-6.1-SNAPSHOT.jar:../../../lib/jetty-6.1-SNAPSHOT.jar:../../../lib/servlet-api-2.5-6.1-SNAPSHOT.jar org.mortbay.cometd.client.BayeuxLoadGenerator
+java -Xmx2048M -cp target/classes:target/test-classes/:../bayeux/target/classes/:../../../lib/cometd/cometd-api-1.0-SNAPSHOT.jar:../../../lib/ext/jetty-client-__JETTY_VERSION__.jar:../../../lib/ext/jetty-sslengine-__JETTY_VERSION__.jar:../../../lib/jre1.5/jetty-util5-__JETTY_VERSION__.jar:../../../lib/jetty-util-__JETTY_VERSION__.jar:../../../lib/jetty-__JETTY_VERSION__.jar:../../../lib/servlet-api-2.5-__JETTY_VERSION__.jar org.mortbay.cometd.client.BayeuxLoadGenerator
Index: contrib/cometd/client/pom.xml
===================================================================
--- contrib/cometd/client/pom.xml	(revision 1197)
+++ contrib/cometd/client/pom.xml	(working copy)
@@ -2,7 +2,7 @@
   <parent>
     <groupId>org.mortbay.jetty</groupId>
     <artifactId>cometd-project</artifactId>
-    <version>6.1-SNAPSHOT</version>
+    <version>__JETTY_VERSION__</version>
     <relativePath>../pom.xml</relativePath>
   </parent>
   <modelVersion>4.0.0</modelVersion>
Index: contrib/jetty-openspaces/pom.xml
===================================================================
--- contrib/jetty-openspaces/pom.xml	(revision 1197)
+++ contrib/jetty-openspaces/pom.xml	(working copy)
@@ -2,7 +2,7 @@
     <parent>
       <artifactId>project</artifactId>
       <groupId>org.mortbay.jetty</groupId>
-      <version>6.1-SNAPSHOT</version>
+      <version>__JETTY_VERSION__</version>
     </parent>
 
     <modelVersion>4.0.0</modelVersion>
_EOF_
