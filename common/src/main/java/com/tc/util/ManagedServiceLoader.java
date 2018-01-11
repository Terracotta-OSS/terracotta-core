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
import com.tc.classloader.OverrideServiceType;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ManagedServiceLoader {

  private static final Logger LOG = LoggerFactory.getLogger(ManagedServiceLoader.class);

  private static final String METAINFCONST = "META-INF/services/";

  public static <T> Collection<T> loadServices(Class<T> serviceClass, ClassLoader loader) {
    return new ManagedServiceLoader().getImplementations(serviceClass, loader)
        .stream()
        .map((clazz) -> {
          try {
            return clazz.newInstance();
          } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Unable to resolve service implementations for " + serviceClass, e);
          }})
        .collect(toList());
  }
    
  public ManagedServiceLoader() {
  }

  /**
   * Discover all the jar resources which have implementation of particular service
   *
   * @param loader        classloader used to discover the resources
   * @param interfaceName service interface name to be queried
   * @return Map containing Implementation name and URL of resource to load it from
   */
  protected Collection<Class<?>> discoverImplementations(String interfaceName, ClassLoader loader) {
    try {
      Set<String> processed = new HashSet<>();
      HashMap<String, ClassWithLocation> urls = new HashMap<>();
      HashMap<String, String> overrides = new HashMap<>();
      Enumeration<URL> urlEnumeration = loader.getResources(
          METAINFCONST + interfaceName);
      StringBuilder sb = new StringBuilder();
      while (urlEnumeration.hasMoreElements()) {
        URL x = urlEnumeration.nextElement();
        String urlString = parseURLString(x, interfaceName);
        if (!processed.add(urlString)) {
          LOG.debug("already processed " + urlString);
          continue;
        } else {
          LOG.debug("reading " + urlString + " for " + interfaceName);
        }
        InputStream s = x.openStream();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(s, Charset.forName("UTF-8")));
        String split = reader.readLine();
        while (split != null) {
          LOG.debug(reader.getLineNumber() + ":processing " + split);
          String[] trim = split.trim().split("\\#");
          split = reader.readLine();
          for (int c=0;c<trim.length;c++) {
            trim[c] = trim[c].trim();
          }
          if(trim.length == 0 || trim[0].isEmpty()) {
            continue;
          }
  //  make sure the class is loadable         
          Class<?> type = loadClass(trim[0], urlString, loader);          
          if (type != null) {
            //  overrides in the manifest
            if (trim.length > 1) {
              String[] overridesClasses = checkForOverride(trim[1]);
              for (String override : overridesClasses) {
                LOG.debug("overriding class " + override + " with " + trim[0]);
                urls.remove(override);
                overrides.put(override, trim[0]);
              }
            }
            // overrides based on OverrideServices
            if (type.isAnnotationPresent(OverrideService.class)) {
              for (OverrideService override : type.getAnnotationsByType(OverrideService.class)) {
                LOG.debug("overriding class " + override.value() + " with annotation on " + trim[0]);
                String value = override.value();
                String[] types = override.types();
                if (value != null && value.length() > 0) {
                  urls.remove(value);
                  overrides.put(value, trim[0]);
                }
                for (String typeName : types) {
                  urls.remove(typeName);
                  overrides.put(typeName, trim[0]);
                }
              }
            }
            // overrides based on OverrideServiceTypes
            if (type.isAnnotationPresent(OverrideServiceType.class)) {
              for (OverrideServiceType override : type.getAnnotationsByType(OverrideServiceType.class)) {
                LOG.debug("overriding class " + override.value() + " with annotation on " + trim[0]);
                Class<?> value = override.value();
                if (value != null) {
                  urls.remove(value.getName());
                  overrides.put(value.getName(), trim[0]);
                }
              }
            }
            // only add the service if it is not part of the override graph
            if (!overrides.containsKey(trim[0])) {
              ClassWithLocation previous = urls.putIfAbsent(trim[0], new ClassWithLocation(type, urlString));
              if (previous != null) {
                LOG.info("MULTIPLE instances of " + trim[0] + " found, ignoring:" + urlString + " keeping:" + previous.location + " using classloader:" + type.getClassLoader());
              }
            }
          } else {
            LOG.info(trim[0] + " is not loadable from " + urlString + " skipping");
          }
        }
        sb.setLength(0);
      }
      if(LOG.isDebugEnabled()) {
        LOG.debug("implementations:" + urls.toString());
        LOG.debug("overrides:" + overrides.toString());
      }

      return urls.values().stream().map(cwl->cwl.impl).collect(Collectors.toList());
    } catch (IOException e) {
      LOG.warn("unable to load", e);
    }
    return null;
  }
  
  private static String parseURLString(URL src, String interfaceName) {
    String urlString = src.toExternalForm();
    if (urlString.startsWith("jar:")) {
      int resourcePart = urlString.indexOf("!/" + METAINFCONST + interfaceName);
//  strip the jar file notation from the URL, start index of 4 is for 'jar:'
      urlString = urlString.substring(4, resourcePart);
    } else {
//  strip the meta file information from the path
      int index = urlString.indexOf(METAINFCONST);
      urlString = urlString.substring(0, index);
    }
    return urlString;
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
    Collection<Class<?>> items = getImplementations(interfaceClass.getName(), loader);
    final Class<?> list[] = items.toArray(new Class<?>[items.size()]);
    return new AbstractList<Class<? extends T>>() {
      @Override
      public Class<? extends T> get(int index) {
        Class<?> got = list[index];
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
        return list.length;
      }
    };
  }

  /**
   * Create list of instance of implementation given interface each with an individual implementation having its own isolated classloader.
   *
   * @param interfaceName service type to be created
   * @param loader        parent classloader for all component to be loaded
   * @return list of implementation
   */
  private Collection<Class<?>> getImplementations(String interfaceName, final ClassLoader loader) {
    if(LOG.isDebugEnabled()) {
      LOG.debug("Discovering " + interfaceName + " with parent classloader " + loader.getClass().getName());
    }
    final Collection<Class<?>> urls = discoverImplementations(interfaceName, loader);
    if (null == urls || urls.isEmpty()) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("No implementations found for " + interfaceName);
      }
      return Collections.emptyList();
    }
    return urls;
  }
  
  protected Class<?> loadClass(String className, String location, ClassLoader loader) {
    try {
      return loader.loadClass(className);
    } catch (ClassNotFoundException c) {
      LOG.warn("No implementations found for " + className, c);
    }
    return null;
  }
  
  private static class ClassWithLocation {
    private final Class<?> impl;
    private final String location;

    public ClassWithLocation(Class<?> impl, String location) {
      this.impl = impl;
      this.location = location;
    }
    
    
  }
}