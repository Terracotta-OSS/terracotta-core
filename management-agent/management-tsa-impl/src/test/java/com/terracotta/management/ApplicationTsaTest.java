package com.terracotta.management;

import static org.hamcrest.Matchers.equalTo;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaTest {
  private static final String[] PACKAGE_STARTS_WITH_FILTERS  = new String[] { "java", "javax", "org.apache", "com.sun",
      "org.codehaus", "org.hibernate"                       };
  private static final String[] PATH_TO_JAR_CONTAINS_FILTERS = new String[] { "repository" };

  @Test
  public void testGetClasses() throws Exception {
    List<String> classpathElements = getClasspathElements();

    ApplicationTsa applicationTsa = new ApplicationTsa();
    Set<Class<?>> applicationClasses = applicationTsa.getClasses();

    Set<Class<?>> allClassesFound = new HashSet<Class<?>>();
    for (String cpElement : classpathElements) {
      if (cpElement.endsWith(".jar")) {
        if (pathOfJarNotFiltered(cpElement)) {
          System.out.println("last scanned path : " + cpElement);
          allClassesFound.addAll(getAllClassesFromJar(cpElement));
        }
      } else {
        System.out.println("last scanned path : " + cpElement);
        allClassesFound.addAll(getAllClassesFromDirectory(cpElement));
      }
    }

    Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
    for (Class<?> aClass : allClassesFound) {
      if (aClass.isAnnotationPresent(javax.ws.rs.ext.Provider.class)
          || aClass.isAnnotationPresent(javax.ws.rs.Path.class)) {
        annotatedClasses.add(aClass);
      }
    }

    Assert.assertThat(applicationClasses, equalTo(annotatedClasses));

  }

  private List<String> getClasspathElements() throws IOException {
    List<String> elements = new ArrayList<String>();
    String classpath = System.getProperty("java.class.path");
    System.out.println("XXX Current classpath: " + classpath);
    if (classpath.contains("surefirebooter")) {
      JarFile surefireBooter = new JarFile(classpath);
      Manifest manifest = surefireBooter.getManifest();
      classpath = manifest.getMainAttributes().getValue("Class-path");
      surefireBooter.close();
      for (String urlElement : classpath.split(" ")) {
        elements.add(FileUtils.toFile(new URL(urlElement)).getAbsolutePath());
      }
    } else {
      for (String path : classpath.split(File.pathSeparator)) {
        elements.add(path);
      }
    }
    return elements;
  }

  private Set<Class<?>> getAllClassesFromDirectory(String cpElement) throws ClassNotFoundException {
    Set<Class<?>> classes = new HashSet<Class<?>>();
    File rootPath = new File(cpElement);
    File[] files = rootPath.listFiles();
    for (File file : files) {
      findAndAddClassesFromRootPath(file, classes, rootPath);
    }
    return classes;
  }

  private Collection<? extends Class<?>> getAllClassesFromJar(String cpElement) throws IOException {
    Set<Class<?>> classes = new HashSet<Class<?>>();
    JarFile jarFile = new JarFile(cpElement);
    Enumeration<JarEntry> entries = jarFile.entries();
    while (entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      if (entryName.endsWith(".class")) {
        String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
        try {
          if (packageOfClassNotFiltered(className)) {
            classes.add(Class.forName(className));
          }
        } catch (Throwable e) {
          // not a problem
        }
      }
    }
    jarFile.close();
    return classes;
  }

  private boolean packageOfClassNotFiltered(String className) {
    for (String filter : PACKAGE_STARTS_WITH_FILTERS) {
      if (className.startsWith(filter)) { return false; }
    }
    return true;
  }

  private boolean pathOfJarNotFiltered(String jarPath) {
    for (String filter : PATH_TO_JAR_CONTAINS_FILTERS) {
      if (jarPath.contains(filter)) { return true; }
    }
    return false;
  }

  private void findAndAddClassesFromRootPath(File file, Set<Class<?>> classes, File rootPath)
      throws ClassNotFoundException {
    if (file.isFile() && file.getAbsolutePath().endsWith(".class")) {
      String replace = file.getAbsolutePath().replace(rootPath.getAbsolutePath() + File.separator, "");
      String className = replace.replace(File.separator, ".").substring(0, replace.length() - 6);
      try {
        classes.add(Class.forName(className));
      } catch (Throwable e) {
        // not a problem
      }
    } else if (file.isDirectory()) {
      File[] listOfFiles = file.listFiles();
      if (listOfFiles != null) {
        for (File listOfFile : listOfFiles)
          findAndAddClassesFromRootPath(listOfFile, classes, rootPath);
      }
    }
  }

}
