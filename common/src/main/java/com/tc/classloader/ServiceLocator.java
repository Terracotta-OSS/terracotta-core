package com.tc.classloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Top level service locator class used to identify and isolate service dependencies in its own classloader.
 */
public class ServiceLocator {

  private static final String METAINFCONST = "META-INF/services/";

  /**
   * Discover all the jar resources which have implementation of particular service
   *
   * @param loader        classloader used to discover the resources
   * @param interfaceName service interface name to be queried
   * @return Map containing Implementation name and URL of resource to load it from
   */
  public static Map<String, String> initalize(ClassLoader loader, String interfaceName) {
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
          urls.put(split, x.getFile().split("!")[0]);
        }
        sb.setLength(0);
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
    Map<String, String> urls = initalize(parent, interfaceName.getName());
    ArrayList<T> implementations = new ArrayList<T>();
    if (null == urls || urls.isEmpty()) {
      return implementations;
    }
    for (Map.Entry<String, String> entry : urls.entrySet()) {
      try {
        ComponentLoader loader = new ComponentLoader(new URL(entry.getValue()), parent);
        implementations.add((T)
            Class.forName(entry.getKey(), false, loader).newInstance());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return implementations;
  }

}