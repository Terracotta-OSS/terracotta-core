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

package com.tc.util;

import com.tc.classloader.OverrideService;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ManagedServiceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ManagedServiceLoader.class);

  private static final String METAINFCONST = "META-INF/services/";
    
  public ManagedServiceLoader() {
  }

  /**
   * Discover all the jar resources which have implementation of particular service
   *
   * @param loader        classloader used to discover the resources
   * @param interfaceName service interface name to be queried
   * @return Map containing Implementation name and URL of resource to load it from
   */
  protected Map<String, String> discoverImplementations(String interfaceName, ClassLoader loader) {
    try {
      HashMap<String, String> urls = new HashMap<String, String>();
      HashMap<String, String> overrides = new HashMap<String, String>();
      Enumeration<URL> urlEnumeration = loader.getResources(
          METAINFCONST + interfaceName);
      StringBuilder sb = new StringBuilder();
      while (urlEnumeration.hasMoreElements()) {
        URL x = urlEnumeration.nextElement();
        InputStream s = x.openStream();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(s, Charset.forName("UTF-8")));
        String split = reader.readLine();
        while (split != null) {
          String[] trim = split.trim().split("\\#");
          split = reader.readLine();
          for (int c=0;c<trim.length;c++) {
            trim[c] = trim[c].trim();
          }
          if(trim.length == 0 || trim[0].isEmpty()) {
            continue;
          }
          if (trim.length > 1) {
            String[] overridesClasses = checkForOverride(trim[1]);
            for (String override : overridesClasses) {
              LOG.debug("overriding class " + override + " with " + trim[0]);
              urls.remove(override);
              overrides.put(override, trim[0]);
            }
          }
          String urlString = x.toExternalForm();
          if (urlString.startsWith("jar:")) {
      //  strip the jar file notation from the URL, start index of 4 is for 'jar:'
            urlString = urlString.substring(4, urlString.indexOf("!"));
          } else {
      //  strip the meta file information from the path
            urlString = urlString.substring(0, urlString.indexOf(METAINFCONST));
          }
          Class<?> type = loadClass(trim[0], urlString, loader);
          if (type.isAnnotationPresent(OverrideService.class)) {
            OverrideService os = type.getAnnotation(OverrideService.class);
            for (String override : os.types()) {
              LOG.debug("overriding class " + override + " with annotation on " + trim[0]);
              urls.remove(override);
              overrides.put(override, trim[0]);
            }
          }
          if (!overrides.containsKey(trim[0])) {
            String previous = urls.put(trim[0], urlString);
            if (previous != null) {
              LOG.info("MULTIPLE instances of " + trim[0] + " found, ignoring:" + urlString + " keeping:" + previous);
            }
          }
        }
        sb.setLength(0);
      }
      if(LOG.isDebugEnabled()) {
        LOG.debug("implementations:" + urls.toString());
        LOG.debug("overrides:" + overrides.toString());
      }

      return urls;
    } catch (IOException e) {
      LOG.warn("unable to load", e);
    }
    return null;
  }
  
  private static String[] checkForOverride(String value) {
    int index = value.indexOf("overrides ");
    if (index >= 0) {
      String[] list = value.substring(index + 10).trim().split(",");
      for (int x=0;x<list.length;x++) {
        list[x] = list[x].trim();
      }
      return list;
    }
    return new String[0];
  }
    
  public <T> List<Class<? extends T>> getImplementations(final Class<T> interfaceClass, ClassLoader loader) {
    Assert.assertNotNull(loader);
    final List<Class<?>> items = (List<Class<?>>)getImplementations(interfaceClass.getName(), loader);
    return new AbstractList<Class<? extends T>>() {
      @Override
      public Class<? extends T> get(int index) {
        Class<?> got = items.get(index);
        try {
          return got.asSubclass(interfaceClass);
        } catch (ClassCastException cast) {
          ClassLoader loader = interfaceClass.getClassLoader();
          ClassLoader sub = got.getInterfaces()[0].getClassLoader();
          LOG.warn("There has been a class cast exception.  This is usually an indication that a service has been improperly packaged with system dependencies included.  Offending class is " + interfaceClass.getName());
          throw cast;
        }
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
  private List<Class<?>> getImplementations(String interfaceName, final ClassLoader loader) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Discovering " + interfaceName + " with parent classloader " + loader.getClass().getName());
    }
    final Map<String, String> urls = discoverImplementations(interfaceName, loader);
    if (null == urls || urls.isEmpty()) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("No implementations found for " + interfaceName);
      }
      return Collections.emptyList();
    }
    final List<String> keys = new ArrayList(urls.keySet());
    return new AbstractList<Class<?>>() {
      @Override
      public Class<?> get(int index) {
        String className = keys.get(index);
        return loadClass(className, urls.get(className), loader);
      }

      @Override
      public int size() {
        return keys.size();
      }
    };
  }
  
  protected Class<?> loadClass(String className, String location, ClassLoader loader) {
    try {
      return loader.loadClass(className);
    } catch (ClassNotFoundException c) {
      LOG.warn("No implementations found for " + className, c);
    }
    return null;
  }
}