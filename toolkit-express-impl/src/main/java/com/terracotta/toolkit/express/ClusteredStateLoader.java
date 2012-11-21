/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import com.terracotta.toolkit.express.loader.Util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Here's a brief of whats going on:
 * <p/>
 * <li>ClusteredStateLoader created with a bunch of urls, and a handle to app loader</li>
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
class ClusteredStateLoader extends SecureClassLoader {
  private static final boolean      USE_APP_JTA_CLASSES;
  private static final String       PRIVATE_CLASS_SUFFIX     = ".class_terracotta";
  private static final boolean      DEV_MODE                 = Boolean.getBoolean("com.tc.test.toolkit.devmode");

  private final ClassLoader         appLoader;
  private final Map<String, byte[]> extraClasses             = new ConcurrentHashMap<String, byte[]>();
  private final List<String>        embeddedResourcePrefixes;
  private final ClassLoader         devModeUrlClassLoader;

  static {
    String prop = System.getProperty(ClusteredStateLoader.class.getName() + ".USE_APP_JTA_CLASSES", "true");
    prop = prop.trim();
    USE_APP_JTA_CLASSES = Boolean.valueOf(prop);
  }

  ClusteredStateLoader(List<String> prefixes, AppClassLoader appLoader) {
    super(null);
    this.appLoader = appLoader;
    this.embeddedResourcePrefixes = prefixes;
    if (DEV_MODE) {
      System.out.println("XXX TOOLKIT DEVMODE CLASS LOADING IS APPLIED.");
      devModeUrlClassLoader = URLClassLoader.newInstance(generateDevModeUrls(), null);
    } else {
      devModeUrlClassLoader = null;
    }
  }

  void addExtraClass(String name, byte[] classBytes) {
    extraClasses.put(name, classBytes);
  }

  private URL loadDevModeClassAsResource(String name) {
    if (name.startsWith("java") || name.startsWith("sun")) { return null; }
    String resource = name.replace('.', '/') + ".class";
    return devModeUrlClassLoader.getResource(resource);
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    URL resource = DEV_MODE ? devModeUrlClassLoader.getResource(name) : findResourceWithPrefix(name);
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
  public URL getResource(String name) {
    URL resource = DEV_MODE ? devModeUrlClassLoader.getResource(name) : findResourceWithPrefix(name);
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
    if (extra != null) { return defineClass(name, extra); }

    // special case jta types to allow consistent loading with the app
    if (USE_APP_JTA_CLASSES && name.startsWith("javax.transaction.")) { return appLoader.loadClass(name); }

    // special case slf4j too. If the app already has it don't use the one that might have been included for embedded
    // ehcache (since the reward is a loader contstraint violation later down the road)
    if (name.startsWith("org.slf4j")) {
      try {
        return appLoader.loadClass(name);
      } catch (ClassNotFoundException cnfe) {
        //
      }
    }

    URL url = DEV_MODE ? loadDevModeClassAsResource(name) : findClassWithPrefix(name);
    if (url != null) { return loadClassFromUrl(name, url); }

    // last path is to delegate to the app loader and finally to the thread context loader
    // A case where final fallback to the thread context is relevant is when the
    // toolkit-runtime is in a shared classloader (eg. tomcat/lib). In that case things like
    // ehcache-core or terracotta-ehcache are not present in the "appLoader" but likely
    // are in the thread context loader
    try {
      return appLoader.loadClass(name);
    } catch (ClassNotFoundException cnfe) {
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      if (contextClassLoader != this && contextClassLoader != appLoader && contextClassLoader != getParent()) {
        //
        return contextClassLoader.loadClass(name);
      }
      throw cnfe;
    }
  }

  private URL findClassWithPrefix(String name) {
    String resource = name.replace('.', '/').concat(PRIVATE_CLASS_SUFFIX);
    for (String prefix : embeddedResourcePrefixes) {
      URL url = appLoader.getResource(prefix + resource);
      if (url != null) { return url; }
    }
    return null;
  }

  private URL findResourceWithPrefix(String name) {
    String resource = name.endsWith(".class") ? name.substring(0, name.lastIndexOf(".class")) + PRIVATE_CLASS_SUFFIX
        : name;
    for (String prefix : embeddedResourcePrefixes) {
      URL url = appLoader.getResource(prefix + resource);
      if (url != null) { return url; }
    }
    return null;
  }

  private Class<?> loadClassFromUrl(String name, URL url) {
    String packageName = name.substring(0, name.lastIndexOf('.'));
    if (getPackage(packageName) == null) {
      definePackage(packageName, null, null, null, null, null, null, null);
    }

    try {
      byte[] bytes = Util.extract(url.openStream());
      Class<?> clazz = defineClass(name, bytes, 0, bytes.length, appLoader.getClass().getProtectionDomain()
          .getCodeSource());
      return clazz;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError {
    return defineClass(name, bytes, 0, bytes.length);
  }

  private static URL[] generateDevModeUrls() {
    List<URL> urlList = new ArrayList<URL>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(
                                                        ClusteredStateLoader.class
                                                            .getResourceAsStream("/devmode-embedded-classpath.txt")));
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) continue;
        for (String part : line.split(File.pathSeparator)) {
          String path = part.trim();
          if (path.length() == 0) continue;
          File file = new File(path);
          urlList.add(file.toURI().toURL());
        }
      }
      System.out.println("XXX devmode classpath: " + urlList);
      return urlList.toArray(new URL[0]);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      Util.closeQuietly(reader);
    }
  }
}
