/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.express.loader.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * Here's a brief of whats going on:
 * <p/>
 * <li>ClusteredStateLoaderImpl created with a bunch of urls, and a handle to app loader</li>
 * <li>tries to load public types and "org.slf4j." classes with app loader</li>
 * <li>tries "extra classes" (which are bytes added to this loader) to define the class</li>
 * <li>tries to load the class from its urls at last</li>
 * <li>finally delegates to the app loader if class still not loaded.</li>
 * <p>
 * The point is:
 * <li>Load public types with app loader</li>
 * <li>Loads classes from its url - (which has urls of jars inside jars)</li>
 * <li>use app loader for everything else.</li>
 */
class ClusteredStateLoaderImpl extends ClusteredStateLoader {
  private static final boolean               USE_APP_JTA_CLASSES;
  private static final String                TOOLKIT_CONTENT_RESOURCE = "/toolkit-content.txt";
  private static final String                PRIVATE_CLASS_SUFFIX     = ".class_terracotta";

  private final ClassLoader                  appLoader;
  private final Map<String, HashSet<String>> internalResource;

  static {
    String prop = System.getProperty(ClusteredStateLoaderImpl.class.getName() + ".USE_APP_JTA_CLASSES", "true");
    prop = prop.trim();
    USE_APP_JTA_CLASSES = Boolean.valueOf(prop);
  }

  ClusteredStateLoaderImpl(AppClassLoader appLoader, boolean useEmbeddedEhcache) {
    super(null);
    this.appLoader = appLoader;
    internalResource = loadResourceIndex(useEmbeddedEhcache);
  }

  private Map<String, HashSet<String>> loadResourceIndex(boolean useEmbeddedEhcache) {
    InputStream in = ClusteredStateLoaderImpl.class.getResourceAsStream(TOOLKIT_CONTENT_RESOURCE);
    if (in == null) throw new RuntimeException("Couldn't load resource entries file at: " + TOOLKIT_CONTENT_RESOURCE);
    BufferedReader reader = null;
    try {
      Map<String, HashSet<String>> content = new HashMap<String, HashSet<String>>();
      reader = new BufferedReader(new InputStreamReader(in));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) {
          continue;
        }
        int secondSlash = line.indexOf("/", line.indexOf("/") + 1);
        String prefix = line.substring(0, secondSlash + 1);
        String resource = line.substring(secondSlash + 1);
        HashSet<String> prefixSet = content.get(prefix);
        if (prefixSet == null) {
          prefixSet = new HashSet<String>();
          content.put(prefix, prefixSet);
        }
        prefixSet.add(resource);
      }

      // filter out ehcache resources if not needed
      List<String> filteredPrefix = null;
      if (useEmbeddedEhcache) {
        filteredPrefix = new ArrayList<String>(content.keySet());
      } else {
        filteredPrefix = new ArrayList<String>();
        for (String prefix : content.keySet()) {
          if (!prefix.startsWith("ehcache/")) {
            filteredPrefix.add(prefix);
          }
        }
      }
      // we sort prefix so we can have a consistent lookup order
      Collections.sort(filteredPrefix);

      // reconstruct the map with sorted order of prefixes
      Map<String, HashSet<String>> sortedContent = new LinkedHashMap<String, HashSet<String>>();
      for (String prefix : filteredPrefix) {
        sortedContent.put(prefix, content.get(prefix));
      }
      return sortedContent;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    } finally {
      Util.closeQuietly(in);
    }
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    URL resource = findResourceWithPrefix(name);
    if (resource != null) {
      try {
        return resource.openStream();
      } catch (IOException e) {
        // ignore
      }
    }
    InputStream in = super.getResourceAsStream(name);
    if (in != null) return in;
    return appLoader.getResourceAsStream(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> resources = findResourcesWithPrefix(name);
    if (resources != null && resources.hasMoreElements()) { return resources; }

    resources = super.getResources(name);
    if (resources != null && resources.hasMoreElements()) { return resources; }

    return appLoader.getResources(name);
  }

  @Override
  public URL getResource(String name) {
    URL resource = findResourceWithPrefix(name);
    if (resource != null) { return resource; }
    resource = super.getResource(name);
    if (resource != null) { return resource; }
    return appLoader.getResource(name);
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class rv = loadClass(name);
    if (resolve) {
      resolveClass(rv);
    }

    return rv;
  }

  @Override
  public synchronized Class<?> loadClass(String name) throws ClassNotFoundException {
    Class<?> rv = findLoadedClass(name);
    if (rv != null) { return rv; }

    byte[] extra = extraClasses.remove(name);
    if (extra != null) { return returnAndLog(defineClass(name, extra, 0, extra.length), "extra"); }

    // special case jta types to allow consistent loading with the app
    if (USE_APP_JTA_CLASSES && name.startsWith("javax.transaction.")) { return returnAndLog(appLoader.loadClass(name),
                                                                                            "appLoader"); }

    // special case slf4j too. If the app already has it don't use the one that might have been included for embedded
    // ehcache (since the reward is a loader contstraint violation later down the road)
    if (name.startsWith("org.slf4j")) {
      try {
        return returnAndLog(appLoader.loadClass(name), "appLoader");
      } catch (ClassNotFoundException cnfe) {
        //
      }
    }

    URL url = findClassWithPrefix(name);
    if (url != null) { return returnAndLog(loadClassFromUrl(name, url, appLoader.getClass().getProtectionDomain()
                                               .getCodeSource()), "embedded resource"); }

    // last path is to delegate to the app loader and finally to the thread context loader
    // A case where final fallback to the thread context is relevant is when the
    // toolkit-runtime is in a shared classloader (eg. tomcat/lib). In that case things like
    // ehcache-core or terracotta-ehcache are not present in the "appLoader" but likely
    // are in the thread context loader
    try {
      return returnAndLog(appLoader.loadClass(name), "appLoader");
    } catch (ClassNotFoundException cnfe) {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if (contextClassLoader != this && contextClassLoader != appLoader && contextClassLoader != getParent()) {
        //
        return returnAndLog(contextClassLoader.loadClass(name), contextClassLoader.toString());
      }
      throw cnfe;
    }
  }

  private String findInternalPrefix(String resource) {
    for (Map.Entry<String, HashSet<String>> entry : internalResource.entrySet()) {
      String prefix = entry.getKey();
      HashSet<String> resources = entry.getValue();
      if (resources.contains(resource)) { return prefix; }
    }
    return null;
  }

  private URL findClassWithPrefix(String name) {
    String resource = name.replace('.', '/').concat(PRIVATE_CLASS_SUFFIX);
    String prefix = findInternalPrefix(resource);
    return prefix != null ? appLoader.getResource(prefix + resource) : null;
  }

  private URL findResourceWithPrefix(String name) {
    String resource = name.endsWith(".class") ? name.substring(0, name.lastIndexOf(".class")) + PRIVATE_CLASS_SUFFIX
        : name;
    String prefix = findInternalPrefix(resource);
    return prefix != null ? appLoader.getResource(prefix + resource) : null;
  }

  private Enumeration<URL> findResourcesWithPrefix(String name) throws IOException {
    String resource = name.endsWith(".class") ? name.substring(0, name.lastIndexOf(".class")) + PRIVATE_CLASS_SUFFIX
        : name;

    Vector<URL> urls = new Vector<URL>();
    String prefix = findInternalPrefix(resource);
    if (prefix != null) {
      Enumeration<URL> e = appLoader.getResources(prefix + resource);
      if (e != null) {
        while (e.hasMoreElements()) {
          urls.add(e.nextElement());
        }
      }
    }

    if (urls.size() > 0) { return urls.elements(); }

    return null;
  }
}
