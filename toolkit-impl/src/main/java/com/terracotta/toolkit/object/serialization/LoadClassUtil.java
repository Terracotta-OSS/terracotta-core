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

public class LoadClassUtil {

  /**
   * The purpose of this method is to call loadClass on the relevant classloader but deal with possibility of class
   * names like "[Ljava.lang.String;" or "[[I" which are what you get from String[].class.getName() and
   * int[][].class.getName(). Normally Class.forName() just takes care of this for you but that method unfortunately
   * caches results in an undesired way
   */
  public static Class<?> loadClass(String name, ClassLoader loader) throws ClassNotFoundException {
    if (loader == null) { return Class.forName(name, false, null); }

    if (name.charAt(0) != '[') { return loader.loadClass(name); }

    int dimensions = 1;
    while (name.charAt(dimensions) == '[') {
      dimensions++;
    }

    String componentType = name.substring(dimensions);

    if (componentType.charAt(0) == 'L') {
      Class componentClass = loader.loadClass(componentType.substring(1, componentType.length() - 1));
      return Class.forName(name, false, componentClass.getClassLoader());
    } else {
      return Class.forName(name, false, null);
    }
  }
}
