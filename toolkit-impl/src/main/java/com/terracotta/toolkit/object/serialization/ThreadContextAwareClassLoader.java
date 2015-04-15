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
package com.terracotta.toolkit.object.serialization;

public class ThreadContextAwareClassLoader extends ClassLoader {

  public ThreadContextAwareClassLoader(ClassLoader parent) {
    super(parent);
  }

  // Should it be findClass here ??
  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    // Check whether it's already loaded
    Class loadedClass = findLoadedClass(name);
    if (loadedClass != null) { return loadedClass; }

    // Try to load from thread context classloader, if it exists
    try {
      ClassLoader tccl = Thread.currentThread().getContextClassLoader();
      return Class.forName(name, false, tccl);
    } catch (ClassNotFoundException e) {
      // Swallow exception - does not exist in tccl
    }

    // If not found locally, use normal parent delegation
    return super.loadClass(name);
  }

}
