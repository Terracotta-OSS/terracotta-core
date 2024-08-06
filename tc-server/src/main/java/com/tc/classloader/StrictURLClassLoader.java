/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
