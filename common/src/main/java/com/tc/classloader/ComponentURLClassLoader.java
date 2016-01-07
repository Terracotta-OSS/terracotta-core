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
public class ComponentURLClassLoader extends URLClassLoader {

  private final CommonComponentChecker commonComponentChecker;

  public ComponentURLClassLoader(URL[] urls, ClassLoader parent, CommonComponentChecker commonComponentChecker) {
    super(urls, parent);
    this.commonComponentChecker = commonComponentChecker;
  }
  
  public ComponentURLClassLoader(URL urls, ClassLoader parent, CommonComponentChecker commonComponentChecker) {
    this(new URL[] {urls} , parent, commonComponentChecker);
  }
  
  @Override
  protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    Class<?> target = findLoadedClass(name);
//  if it's already loaded in this loader, return it, decision has already been made about where 
//  to load in previous iteration
    if (target == null) {
      target = super.loadClass(name, resolve);
// if the class is not found, ClassNotFoundException will be thrown and that is fine, class is nowhere
      if (!commonComponentChecker.check(target)) {
//  not a common class as designated by annotation, see if the class is in this specific class loader for preference if it is
        try {
          target = findClass(name);
        } catch (ClassNotFoundException notfound) {
//  it's not here in this loader, revert back to the common (already set)
        }
      } else {
  //  this is a designated common component, return it no matter where it came from 
  //  (default implementation always uses the parent classloader if the class is available there)
      }
    }
    
    if (resolve) {
      this.resolveClass(target);
    }
    return target;
  }
}
