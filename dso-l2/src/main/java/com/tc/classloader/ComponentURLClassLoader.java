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
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class ComponentURLClassLoader extends URLClassLoader {

  private static final Logger LOGGER = LoggerFactory.getLogger(ComponentURLClassLoader.class);
  private final CommonComponentChecker commonComponentChecker;
  private static final boolean STRICT = Boolean.getBoolean("plugin.strict");
  private final ApiClassLoader apiLoader;
  private final String root;

  public ComponentURLClassLoader(String root, URL[] urls, ClassLoader parent, CommonComponentChecker commonComponentChecker) {
    super(urls, parent);
    this.root = root;
    this.commonComponentChecker = commonComponentChecker;
    this.apiLoader = findApiLoader();
  }
  
  private ApiClassLoader findApiLoader() {
    ClassLoader parent = getParent();
    while (parent != null) {
      if (parent instanceof ApiClassLoader) {
        return (ApiClassLoader)parent;
      } else {
        parent = parent.getParent();
      }
    }
    return null;
  }

  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {    
    Class<?> target = super.loadClass(name, resolve);
// if the class is not found, ClassNotFoundException will be thrown and that is fine, class is nowhere
    if (!commonComponentChecker.check(target) && target.getClassLoader() != this) {
//  not a common class as designated by annotation, see if the class is in this specific class loader for preference if it is
      try {
        synchronized (getClassLoadingLock(name)) {
          target = findClass(name);
          if (resolve) {
            this.resolveClass(target);
          }
        }
      } catch (ClassNotFoundException notfound) {
        if (STRICT && target.getClassLoader() != apiLoader) {
// in strict mode, throw the exception if the class was not loaded by the apiLoader
          throw notfound;
        } else {
//  it's not here in this loader, revert back to the common (already set)          
        }
      }
    } else {
//  this is a designated common component, return it no matter where it came from 
//  (default implementation always uses the parent classloader if the class is available there)
    }

    return target;
  }
  
  @Override
  public String toString() {
    return "ComponentURLClassLoader{root:" + root + " from "+ Arrays.toString(this.getURLs()) + '}';
  }
}
