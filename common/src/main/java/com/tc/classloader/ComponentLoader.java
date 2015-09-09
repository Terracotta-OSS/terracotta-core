package com.tc.classloader;

import com.google.common.io.ByteStreams;

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
      return findClass(name);
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