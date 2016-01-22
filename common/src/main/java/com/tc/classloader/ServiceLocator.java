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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


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
          String urlString = x.toExternalForm();
          if (urlString.startsWith("jar:")) {
      //  strip the jar file notation from the URL, start index of 4 is for 'jar:'
            urlString = urlString.substring(4, urlString.indexOf("!"));
          } else {
      //  strip the meta file information from the path
            urlString = urlString.substring(0, urlString.indexOf(METAINFCONST));
          }
          urls.put(trim, urlString);
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

  public static <T> List<T> getImplementations(Class<T> interfaceName, ClassLoader parent) {
    final List<?> items = getImplementations(interfaceName.getName(), parent);
    return new AbstractList<T>() {
      @Override
      public T get(int index) {
        return (T)items.get(index);
      }

      @Override
      public int size() {
        return items.size();
      }
    };
  }

  /**
   * Create list of instance of implementation given interface each with an individual implementation having its own isolated classloader.
   *
   * @param interfaceName service type to be created
   * @param parent        parent classloader for all component to be loaded
   * @param <T>           concrete type of service/entity
   * @return list of implementation
   */
  public static List<?> getImplementations(String interfaceName, ClassLoader parent) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Discovering " + interfaceName + " with parent classloader " + parent.getClass().getName());
    }
    ClassLoader apiLoader = getApiClassLoader(parent);
    Map<String, String> urls = discoverImplementations(apiLoader, interfaceName);
    ArrayList<Object> implementations = new ArrayList<Object>();
    if (null == urls || urls.isEmpty()) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("No implementations found for " + interfaceName);
      }
      return implementations;
    }
    for (Map.Entry<String, String> entry : urls.entrySet()) {
      try {
        ComponentURLClassLoader loader = new ComponentURLClassLoader(new URL[] {new URL(entry.getValue())}, getApiClassLoader(parent), new AnnotationOrDirectoryStrategyChecker());
        implementations.add(Class.forName(entry.getKey(), false, loader).newInstance());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return implementations;
  }
  
  private static final Map<ClassLoader, ApiClassLoader> API_LOADERS = new HashMap<ClassLoader, ApiClassLoader>();
  
  private static synchronized ApiClassLoader getApiClassLoader(ClassLoader parent) {
    ApiClassLoader loader = API_LOADERS.get(parent);
    if (loader == null) {
      loader = createApiClassLoader(parent);
      API_LOADERS.put(parent, loader);
    }
    return loader;
  }

  private static ApiClassLoader createApiClassLoader(ClassLoader parent) {
    File pluginApiDir;
    try {
      pluginApiDir = Directories.getServerPluginsApiDir();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    File[] apiJars = pluginApiDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar");
      }
    });

    URL[] apiJarUrls = new URL[0];
    if(apiJars != null) {
      apiJarUrls = new URL[apiJars.length];
      for (int i = 0; i < apiJars.length; i++) {
        try {
          apiJarUrls[i] = apiJars[i].toURI().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }

    }
    return new ApiClassLoader(apiJarUrls, parent);
  }
}