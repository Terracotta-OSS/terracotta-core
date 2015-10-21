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

import com.google.common.io.ByteStreams;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLoggingService;
import com.tc.util.ServiceUtil;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Top level component loader class which loads content within a single jar URL.
 * It does not delegate any loading to its parent unless in case of exceptions, which is inverse of
 * what most of the JVM classloader do (i.e. Ask parent first before loading anything).
 */
public class ComponentLoader extends ClassLoader {
  private static final TCLogger LOG = ServiceUtil.loadService(TCLoggingService.class).getLogger(ComponentLoader.class);

  //URL which the classloader looksup to load class definitions
  private final URL url;
  private final ConcurrentHashMap<String, String> contents;
  private final ConcurrentHashMap<String, WeakReference<Class<?>>> loadedClasses;

  public ComponentLoader(URL url, ClassLoader parent) {
    super(parent);
    this.url = url;
    this.contents = new ConcurrentHashMap<String, String>();
    this.loadedClasses = new ConcurrentHashMap<String, WeakReference<Class<?>>>();
    populateContents();
  }

  /**
   * These are classes which we should load within this particular component loader.
   */
  private void populateContents() {
    File f = new File(url.getFile());
    try {
      JarFile jf = new JarFile(f);
      Enumeration<JarEntry> entries = jf.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        if (name.endsWith(".class")) {
          String x = "jar:" + url.toString() + "!/" + name;
          String entryName = name.replace(".class", "").replace("/", ".");
          contents.put(entryName, x);
        }
      }
    } catch (IOException e) {
      //Exception occurred while reading jar file contents, should we throw runtime exception?
      contents.clear();
    }
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    //check if we need to load this class
    if (contents.get(name) != null) {
      if(LOG.isDebugEnabled()) {
        LOG.debug("Loading " + name + " within an isloated container alongwith depedencies");
      }
      return findClass(name);
    }
    if(LOG.isDebugEnabled()) {
      LOG.debug("Loading " + name + " at parent level");
    }
    //ask our parent to load the class
    return getParent().loadClass(name);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    WeakReference<Class<?>> classWeakReference = loadedClasses.get(name);
    if (classWeakReference != null) {
      Class<?> klass = classWeakReference.get();
      if (klass != null) {
        return klass;
      }
    }
    //Should we lock on interned rep of String to make sure class is only loaded once?
    String spec = contents.get(name);
    if (spec == null) {
      return getParent().loadClass(name);
    }
    URL url;
    try {
      url = new URL(spec);
    } catch (MalformedURLException e) {
      return getParent().loadClass(name);
    }
    Class<?> klass = loadClassFromUrl(name, url, new CodeSource(url, new CodeSigner[0]));
    loadedClasses.put(name, new WeakReference<Class<?>>(klass));
    return klass;
  }

  private Class<?> loadClassFromUrl(String name, URL url, CodeSource codeSource) {
    String packageName = name.substring(0, name.lastIndexOf('.'));
    if (getPackage(packageName) == null) {
      definePackage(packageName, null, null, null, null, null, null, null);
    }

    try {
      byte[] bytes = ByteStreams.toByteArray(url.openStream());
      return defineClass(name, bytes, 0, bytes.length, new ProtectionDomain(codeSource, null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}