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

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class ClusteredStateLoader extends SecureClassLoader {
  private static final boolean        DEBUG_TOOLKIT_CLASS_LOADING = false;
  protected final Map<String, byte[]> extraClasses = new ConcurrentHashMap<String, byte[]>();

  public ClusteredStateLoader(ClassLoader parent) {
    super(parent);
  }

  protected void addExtraClass(String name, byte[] classBytes) {
    extraClasses.put(name, classBytes);
  }

  protected Class<?> loadClassFromUrl(String name, URL url, CodeSource codeSource) {
    String packageName = name.substring(0, name.lastIndexOf('.'));
    if (getPackage(packageName) == null) {
      definePackage(packageName, null, null, null, null, null, null, null);
    }

    try {
      byte[] bytes = Util.extract(url.openStream());
      Class<?> clazz = defineClass(name, bytes, 0, bytes.length, codeSource);
      return clazz;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  // useful for debugging
  protected Class<?> returnAndLog(Class<?> c, String source) {
    if (DEBUG_TOOLKIT_CLASS_LOADING) {
      System.out.println("XXX loaded " + c + " from " + source);
    }
    return c;
  }
}
