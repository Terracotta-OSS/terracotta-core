/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

class AppClassLoader extends ClassLoader {

  private static final Enumeration<URL>          EMPTY_URL_ENUMERATION = new EmptyEnumeration<URL>();

  private final List<WeakReference<ClassLoader>> loaders               = new CopyOnWriteArrayList<WeakReference<ClassLoader>>();
  private final ReferenceQueue<ClassLoader>      refQueue              = new ReferenceQueue<ClassLoader>();

  AppClassLoader(ClassLoader parent) {
    super(parent);
  }

  @Override
  protected Class<?> findClass(String name) throws ClassNotFoundException {
    for (ClassLoader loader : loaders()) {
      try {
        return loader.loadClass(name);
      } catch (ClassNotFoundException cnfe) {
        //
      }
    }

    return super.findClass(name);
  }

  private Iterable<ClassLoader> loaders() {
    reap();

    final Iterator<WeakReference<ClassLoader>> iter = loaders.iterator();

    return new Iterable<ClassLoader>() {
      @Override
      public Iterator<ClassLoader> iterator() {
        return new Iterator<ClassLoader>() {
          private ClassLoader loader;

          {
            advance();
          }

          @Override
          public boolean hasNext() {
            return loader != null;
          }

          private void advance() {
            loader = null;

            while (iter.hasNext()) {
              ClassLoader cl = iter.next().get();
              if (cl != null) {
                loader = cl;
                return;
              }
            }
          }

          @Override
          public ClassLoader next() {
            final ClassLoader rv = loader;
            if (rv == null) { throw new NoSuchElementException(); }
            advance();
            return rv;
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }

  private void reap() {
    Reference<? extends ClassLoader> ref;

    while ((ref = refQueue.poll()) != null) {
      this.loaders.remove(ref);
    }
  }

  @Override
  protected URL findResource(String name) {
    for (ClassLoader loader : loaders()) {
      URL resource = loader.getResource(name);
      if (resource != null) { return resource; }
    }

    return null;
  }

  @Override
  protected Enumeration<URL> findResources(String name) throws IOException {
    for (ClassLoader loader : loaders()) {
      Enumeration<URL> resources = loader.getResources(name);
      if (resources.hasMoreElements()) { return resources; }
    }

    return EMPTY_URL_ENUMERATION;
  }

  void addLoader(ClassLoader loader) {
    if (loader == null) { throw new NullPointerException("null loader"); }

    synchronized (this) {
      // don't add duplicates
      for (ClassLoader cl : loaders()) {
        if (cl == loader) { return; }
      }

      loaders.add(new WeakReference<ClassLoader>(loader, refQueue));
    }
  }

  void clear() {
    loaders.clear();
  }

  private static class EmptyEnumeration<T> implements Enumeration<T> {

    @Override
    public boolean hasMoreElements() {
      return false;
    }

    @Override
    public T nextElement() {
      throw new NoSuchElementException();
    }
  }

}
