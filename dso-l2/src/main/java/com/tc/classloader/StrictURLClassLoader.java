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

import java.net.URL;
import java.net.URLClassLoader;

/**
 *
 */
public class StrictURLClassLoader extends URLClassLoader {

  private final CommonComponentChecker checker;
  ThreadLocal<String> topname = new ThreadLocal<>();

  public StrictURLClassLoader(URL[] urls, ClassLoader cl, CommonComponentChecker checker) {
    super(urls, cl);
    this.checker = checker;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> target = null;

    String top = topname.get();

    if (top == null) {
      topname.set(name);
    }
    
    try {
      target = super.loadClass(name, resolve);
      if (target != null) {
        boolean thisLoader = target.getClassLoader() == this;
        if (thisLoader) {
          boolean common = top == null ? checker.check(target) : true;
          if (!common) {
            target = null;
          }
        }
      }
    } catch (NoClassDefFoundError err) {
      target = null;
    } finally {
      if (top == null) {
        topname.remove();
      }
    }

    if (target == null) {
      throw new ClassNotFoundException(name);
    }

    return target;
  }

  @Override
  protected Package getPackage(String name) {
    return super.getPackage(name);
  }

  @Override
  protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
    return null;
  }


}
