package com.terracotta.management;

import org.junit.Assert;
import org.junit.Test;
import sun.misc.URLClassPath;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.hamcrest.Matchers.equalTo;

/**
 * @author: Anthony Dahanne
 */
public class ApplicationTsaTest {
  private static final String[] PACKAGE_STARTS_WITH_FILTERS = new String[]{"java","javax","org.apache","com.sun","org.codehaus","org.hibernate"};
  private static final String[] PATH_TO_JAR_CONTAINS_FILTERS = new String[]{"repository"};

  @Test
  public void testGetClasses() throws Exception {

    ApplicationTsa applicationTsa = new ApplicationTsa();
    Set<Class<?>> applicationClasses = applicationTsa.getClasses();

    URLClassLoader classLoader = (URLClassLoader) ApplicationTsa.class.getClassLoader();
//    Field f = ClassLoader.class.getDeclaredField("classes");
//    f.setAccessible(true);
    Field declaredFieldUcp = URLClassLoader.class.getDeclaredField("ucp");
    declaredFieldUcp.setAccessible(true);
    URLClassPath ucp = (URLClassPath) declaredFieldUcp.get(classLoader);
    URL[] urLs = ucp.getURLs();
    Set<Class<?>> allClassesFound = new HashSet<Class<?>>();
    for (URL urL : urLs) {
      if(urL.toString().endsWith(".jar")) {
        if(pathOfJarNotFiltered(urL.toString())) {
          System.out.println("last scanned path : "+urL.toString());
          allClassesFound.addAll(getAllClassesFromJar(urL));
        }
      } else {
        System.out.println("last scanned path : "+urL.toString());
        allClassesFound.addAll(getAllClassesFromDirectory(urL));
      }
    }

    Set<Class<?>> annotatedClasses = new HashSet<Class<?>>();
    for (Class<?> aClass : allClassesFound) {
      if (aClass.isAnnotationPresent(javax.ws.rs.ext.Provider.class)
              || aClass.isAnnotationPresent(javax.ws.rs.Path.class)) {
        annotatedClasses.add(aClass);
      }
    }

    Assert.assertThat(annotatedClasses, equalTo(applicationClasses));

  }

  private Set<Class<?>> getAllClassesFromDirectory(URL urL) throws ClassNotFoundException {
    Set<Class<?>> classes =  new HashSet<Class<?>>();
    File rootPath = new File(urL.getFile());
    File[] files = rootPath.listFiles();
    for (File file : files) {
      findAndAddClassesFromRootPath(file, classes, rootPath);
    }
    return classes;
  }

  private Collection<? extends Class<?>> getAllClassesFromJar(URL urL) throws IOException {
    Set<Class<?>> classes =  new HashSet<Class<?>>();
    String decode = URLDecoder.decode(urL.getFile(), "UTF-8");
    JarFile jarFile = new JarFile(decode);
    Enumeration<JarEntry> entries = jarFile.entries();
    while(entries.hasMoreElements()) {
      JarEntry entry = entries.nextElement();
      String entryName = entry.getName();
      if(entryName.endsWith(".class")) {
        String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
        try {
          if(packageOfClassNotFiltered(className)) {
            classes.add(Class.forName(className));
          }
        }
        catch (Throwable e) {
          //not a problem
        }
      }
    }
    return classes;
  }

  private boolean packageOfClassNotFiltered(String className) {
    for (String filter : PACKAGE_STARTS_WITH_FILTERS){
      if(className.startsWith(filter)) {
        return false;
      }
    }
    return true;
  }

  private boolean pathOfJarNotFiltered(String jarPath) {
    for (String filter : PATH_TO_JAR_CONTAINS_FILTERS){
      if(jarPath.contains(filter)) {
        return true;
      }
    }
    return false;
  }

  private void findAndAddClassesFromRootPath(File file, Set<Class<?>> classes, File rootPath) throws ClassNotFoundException {
    if(file.isFile() && file.getAbsolutePath().endsWith(".class")) {
      String replace = file.getAbsolutePath().replace(rootPath.getAbsolutePath() + File.separator, "");
      String className = replace.replaceAll(File.separator, ".").substring(0,replace.length()-6);
      try {
        classes.add(Class.forName(className));
      }
      catch (Throwable e) {
        //not a problem
      }
    } else if (file.isDirectory()){
      File[] listOfFiles = file.listFiles();
      if(listOfFiles != null) {
        for (int i = 0; i < listOfFiles.length; i++)
          findAndAddClassesFromRootPath(listOfFiles[i], classes, rootPath);
      }
    }
  }

}
