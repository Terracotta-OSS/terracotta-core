/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */

package com.tc.classloader;

import com.tc.config.Directories;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
import com.tc.util.ServiceUtil;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ServiceLocator {

  private static final TCLogger LOG = ServiceUtil.loadService(TCLoggingService.class).getLogger(ServiceLocator.class);

  private static final String METAINFCONST = "META-INF/services/";

  /**
   * Discover all the jar resources which have implementation of particular service
   *
   * @param loader        classloader used to discover the resources
   * @param interfaceName service interface name to be queried
   * @return Map containing Implementation name and URL of resource to load it from
   */
  public static Map<String, String> discoverImplementations(ClassLoader loader, String interfaceName) {
    try {
      HashMap<String, String> urls = new HashMap<String, String>();
      Enumeration<URL> urlEnumeration = loader.getResources(
          METAINFCONST + interfaceName);
      StringBuilder sb = new StringBuilder();
      while (urlEnumeration.hasMoreElements()) {
        URL x = urlEnumeration.nextElement();
        InputStream s = x.openStream();
        int i;
        while ((i = s.read()) != -1) {
          sb.append((char) i);
        }
        s.close();
        String[] splits = sb.toString().split("\n");
        for (String split : splits) {
          String trim = split.replaceAll("\n", "").trim();
          if(split.startsWith("#") || trim.isEmpty()) {
            continue;
          }
          urls.put(trim, x.getFile().split("!")[0]);
        }
        sb.setLength(0);
      }
      if(LOG.isDebugEnabled()) {
        LOG.debug("The list of implementation to URL to find them is " + urls.toString());
      }
      return urls;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * Create list of instance of implementation given interface each with an individual implementation having its own isolated classloader.
   *
   * @param interfaceName service type to be created
   * @param parent        parent classloader for all component to be loaded
   * @param <T>           concrete type of service/entity
   * @return list of implementation
   */
  public static <T> List<T> getImplementations(Class<T> interfaceName, ClassLoader parent) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Discovering " + interfaceName.getName() + " with parent classloader " + parent.getClass().getName());
    }
    Map<String, String> urls = discoverImplementations(parent, interfaceName.getName());
    CommonComponentChecker commonComponentChecker = createCommonComponentChecker();
    ArrayList<T> implementations = new ArrayList<T>();
    if (null == urls || urls.isEmpty()) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("No implementations found for " + interfaceName.getName());
      }
      return implementations;
    }
    for (Map.Entry<String, String> entry : urls.entrySet()) {
      try {
        ComponentURLClassLoader loader = new ComponentURLClassLoader(new URL[] {new URL(entry.getValue())}, parent, commonComponentChecker);
        implementations.add((T)
            Class.forName(entry.getKey(), false, loader).newInstance());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return implementations;
  }

  private static CommonComponentChecker createCommonComponentChecker() {

    if(System.getProperty("using-terracotta-kit") != null) {
      Set<String> commonClasses = new HashSet<String>();
      File pluginApiDir;
      try {
        pluginApiDir = Directories.getServerPluginsApiDir();
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
      String[] apiJars = pluginApiDir.list(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".jar");
        }
      });

      if (apiJars != null) {
        for (String jar : apiJars) {
          try {
            JarFile jarFile = new JarFile(pluginApiDir + File.separator + jar);
            Enumeration<JarEntry> entryEnumeration = jarFile.entries();
            while (entryEnumeration.hasMoreElements()) {
              JarEntry entry = entryEnumeration.nextElement();
              String entryName = entry.getName();
              if (!entry.isDirectory() && entryName.endsWith(".class")) {
                String className = entryName.replaceAll("/", ".").replace(".class", "");
                commonClasses.add(className);
              }
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }
      return new BasicCommonComponentChecker(commonClasses);
    } else {
      return new AnnotationBasedCommonComponentChecker();
    }
  }

}