/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.demos;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.terracotta.testing.common.Assert;


public class TestHelpers {
  // NOTE:  This is based on TestBaseUtil.jarFromClassResource() and can be used directly since these class path entries
  // MUST be from a JAR since we need to install them in the kit.
  public static String jarContainingClass(Class<?> clazz) {
    String jarPath = null;
    URL classUrl = clazz.getResource(clazz.getSimpleName() + ".class");
    Assert.assertNotNull(classUrl);
    try {
      URLConnection conn = classUrl.openConnection();
      if (conn instanceof JarURLConnection) {
        JarURLConnection connection = (JarURLConnection) conn;
        jarPath = connection.getJarFileURL().getFile();
        //trim leading "/" from maven jar path on windows
        if (isWindows() && jarPath.startsWith("/")){
          jarPath = jarPath.substring(1);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return jarPath;
  }

  public static boolean isWindows(){
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }
}
