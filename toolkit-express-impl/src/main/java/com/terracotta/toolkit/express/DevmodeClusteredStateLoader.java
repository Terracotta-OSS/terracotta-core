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
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Classloader used for devmode, substitutes for ClusteredStateLoader
 * 
 * @author hhuynh
 */
class DevmodeClusteredStateLoader extends ClusteredStateLoader {
  private static final boolean USE_APP_JTA_CLASSES;
  private static final String  DEVMODE_OS_DEPENDENCIES_RESOURCE = "/META-INF/devmode/org.terracotta/terracotta-toolkit-runtime/embedded-dependencies.txt";
  private static final String  DEVMODE_EE_DEPENDENCIES_RESOURCE = "/META-INF/devmode/org.terracotta/terracotta-toolkit-runtime-ee/embedded-dependencies.txt";

  private final URLClassLoader urlClassLoader;
  private final ClassLoader    appLoader;

  static {
    String prop = System.getProperty(ClusteredStateLoaderImpl.class.getName() + ".USE_APP_JTA_CLASSES", "true");
    prop = prop.trim();
    USE_APP_JTA_CLASSES = Boolean.valueOf(prop);
  }

  public DevmodeClusteredStateLoader(URL depsReource, ClassLoader appLoader, boolean useEmbeddedEhcache) {
    super(null);
    this.appLoader = appLoader;
    urlClassLoader = initUrlClassLoader(depsReource, useEmbeddedEhcache);
  }

  /**
   * returns either EE or OS resource file that contains rest agent dependencies null if not found
   * 
   * @return
   */
  public static URL devModeResource() {
    URL url = DevmodeClusteredStateLoader.class.getResource(DEVMODE_EE_DEPENDENCIES_RESOURCE);
    if (url != null) { return url; }
    url = DevmodeClusteredStateLoader.class.getResource(DEVMODE_OS_DEPENDENCIES_RESOURCE);
    if (url != null) { return url; }
    return null;
  }

  private URLClassLoader initUrlClassLoader(URL depsReource, boolean useEmbeddedEhcache) {
    List<URL> urlList = new ArrayList<URL>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(depsReource.openStream()));
      String line = null;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) continue;
        // assume ehcache deps listed last
        if (line.startsWith("#ehcache") && !useEmbeddedEhcache) {
          break;
        }
        if (line.startsWith("#")) continue;
        URL url = new URL(line);
        urlList.add(url);
      }
      // System.out.println("XXX: toolkit devmode embedded jars " + urlList);
      return new URLClassLoader(urlList.toArray(new URL[0]), null);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      Util.closeQuietly(reader);
    }
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class rv = loadClass(name);
    if (resolve) {
      resolveClass(rv);
    }

    return rv;
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    Class<?> rv = findLoadedClass(name);
    if (rv != null) { return rv; }

    byte[] extra = extraClasses.remove(name);
    if (extra != null) {
      return returnAndLog(defineClass(name, extra), "extra");
    }

    // special case jta types to allow consistent loading with the app
    if (USE_APP_JTA_CLASSES && name.startsWith("javax.transaction.")) { return returnAndLog(appLoader.loadClass(name), "appLoader"); }

    // special case slf4j too. If the app already has it don't use the one that might have been included for embedded
    // ehcache (since the reward is a loader contstraint violation later down the road)
    if (name.startsWith("org.slf4j")) {
      try {
        return returnAndLog(appLoader.loadClass(name), "appLoader");
      } catch (ClassNotFoundException cnfe) {
        //
      }
    }

    // it's important to load an isolated class as resource
    // so we don't fall into the class loading hierarchy problem with javax.* classes
    URL url = urlClassLoader.findResource(name.replace('.', '/') + ".class");
    if (url != null) { return returnAndLog(loadClassFromUrl(name, url, appLoader.getClass().getProtectionDomain().getCodeSource()), "embedded jars"); }

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

  private Class<?> defineClass(String name, byte[] bytes) throws ClassFormatError {
    return defineClass(name, bytes, 0, bytes.length);
  }

  @Override
  public URL getResource(String name) {
    URL resource = urlClassLoader.getResource(name);
    if (resource != null) { return resource; }
    resource = super.getResource(name);
    if (resource != null) { return resource; }
    return appLoader.getResource(name);
  }

  @Override
  public Enumeration<URL> getResources(String name) throws IOException {
    Enumeration<URL> resources = urlClassLoader.getResources(name);
    if (resources != null && resources.hasMoreElements()) { return resources; }

    resources = super.getResources(name);
    if (resources != null && resources.hasMoreElements()) { return resources; }

    return appLoader.getResources(name);
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    URL resource = urlClassLoader.getResource(name);
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
}
