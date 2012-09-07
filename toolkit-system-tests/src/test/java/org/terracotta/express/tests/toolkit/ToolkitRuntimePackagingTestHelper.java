/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.express.tests.toolkit.FilesDFSWalker.FileVisitor;
import org.terracotta.toolkit.ToolkitFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * https://jira.terracotta.org/jira/browse/DEV-6979 <br>
 * Disallow following inside toolkit-runtime jar:
 * <ul>
 * <li>No internal jars with names having both 'toolkit' and 'api'</li>
 * <li>No internal jars containing class files with package 'org.terracotta'. Some exceptions below:
 * <ul>
 * <li>{@link #ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR_IGNORED_JARS}, (some jars are ignored, like terracotta-license jar)
 * </li>
 * </ul>
 * </li>
 * </ul>
 */
public class ToolkitRuntimePackagingTestHelper {

  private static final Pattern       ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR = Pattern.compile(".*org.terracotta.*");
  private static final List<String>  ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR_IGNORED_JARS;
  private static final List<Pattern> ILLEGAL_INTERNAL_JAR_NAMES;
  static {
    List<Pattern> tmp = new ArrayList<Pattern>();
    tmp.add(Pattern.compile(".*toolkit.*api.*.jar$"));
    tmp.add(Pattern.compile(".*api.*toolkit.*.jar$"));
    ILLEGAL_INTERNAL_JAR_NAMES = tmp;
  }

  static {
    // list of jar files inside runtime jar that can have "org.terracotta." classes
    List<String> tmp = new ArrayList<String>();
    tmp.add("terracotta-license"); // org/terracotta/license/
    tmp.add("tcconfig"); // org/terracotta/groupConfigForL1/
    tmp.add("terracotta-l1-ee"); // org/terracotta/modules/configuration
    tmp.add("management-core"); // org/terracotta/management/
    tmp.add("ehcache"); // org/terracotta/modules/ehcache
    tmp.add("deploy"); // org/terracotta/modules/configuration
    ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR_IGNORED_JARS = tmp;
  }

  public static void doTest(File tempDir) throws Exception {
    final File explodedRuntimeJar = unpackRuntimeJar(tempDir);
    System.out.println("Analyzing exploded dir at: " + explodedRuntimeJar.getAbsolutePath());
    FilesDFSWalker.newWalker(explodedRuntimeJar).accept(new FileVisitor() {

      @Override
      public void visitFile(File file) {
        try {
          String jarEntryName = getRelativeName(explodedRuntimeJar, file);
          if (file.isFile() && jarEntryName.endsWith(".jar")) {
            checkJarWithinJar(file, jarEntryName);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      private void checkJarWithinJar(File file, String jarWithinJarName) throws IOException {
        System.out.println("Checking jar-within-jar: " + jarWithinJarName);
        checkIllegalInternalJarName(jarWithinJarName);
        checkIllegalContentsOfInternalJar(file, jarWithinJarName);
      }

      private void checkIllegalContentsOfInternalJar(File file, String jarWithinJarName) throws IOException,
          AssertionError {
        for (String ignore : ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR_IGNORED_JARS) {
          if (jarWithinJarName.contains(ignore)) {
            // skip checking as marked ignored
            return;
          }
        }
        JarFile jar = new JarFile(file);
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          String entryName = entry.getName();
          if (entryName.endsWith(".class")) {
            // only check class files
            checkIllegalClassNameInsideInternalJar(jarWithinJarName, entryName);
          }
        }
      }

      private void checkIllegalClassNameInsideInternalJar(String jarWithinJarName, String entryName)
          throws AssertionError {
        if (ILLEGAL_CLASSNAME_INSIDE_INTERNAL_JAR.matcher(entryName).matches()) { throw new AssertionError(
                                                                                                           "Found illegal content inside jar-within-runtime-jar: "
                                                                                                               + jarWithinJarName
                                                                                                               + ", illegal content: "
                                                                                                               + entryName); }
      }

      private void checkIllegalInternalJarName(String jarWithinJarName) {
        for (Pattern p : ILLEGAL_INTERNAL_JAR_NAMES) {
          if (p.matcher(jarWithinJarName).matches()) { throw new AssertionError(
                                                                                "Found illegal name for jar-within-runtime-jar: "
                                                                                    + jarWithinJarName
                                                                                    + ", should not contain \"api\" and \"toolkit\" in the name"); }
        }
      }

    });

  }

  private static String getRelativeName(File base, File file) {
    return file.getAbsolutePath().substring(base.getAbsolutePath().length());
  }

  // returns path to exploded runtime jar file
  private static File unpackRuntimeJar(File tempDir) throws Exception {
    JarFile runtimeJar = getRuntimeJarFile();
    File exploded = new File(tempDir + File.separator + "exploded-runtime-jar-" + currentTime());
    if (exploded.exists()) { throw new AssertionError("File already exists - " + exploded.getAbsolutePath()); }
    exploded.mkdir();
    extract(runtimeJar, exploded);
    return exploded;
  }

  private static String currentTime() {
    return new SimpleDateFormat("yyyyMMdd.HH.mm.ss.SSS").format(new Date());
  }

  private static void extract(JarFile jar, File destDir) throws IOException {
    System.out.println("Extracting jarfile: " + jar.getName() + " at " + destDir.getAbsolutePath());
    Enumeration<JarEntry> entries = jar.entries();
    while (entries.hasMoreElements()) {
      JarEntry file = entries.nextElement();
      File f = new File(destDir + File.separator + file.getName());
      if (file.isDirectory()) {
        f.mkdir();
        continue;
      }
      InputStream is = jar.getInputStream(file);
      FileOutputStream fos = new FileOutputStream(f);
      while (is.available() > 0) {
        fos.write(is.read());
      }
      fos.close();
      is.close();
    }
    System.out.println("Done extracting " + jar.getName());
  }

  private static JarFile getRuntimeJarFile() throws Exception {
    ProtectionDomain protectionDomain = ToolkitFactory.class.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource != null) {
      URL url = codeSource.getLocation();
      return new JarFile(new File(url.toURI()));
    }
    throw new Exception("Can't locate jar file for toolkit runtime :(");
  }
}
