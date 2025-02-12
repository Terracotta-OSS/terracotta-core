/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
package com.tc.test;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;

public class TestConfigUtil {

  public static String getTcBaseDirPath() {
    String baseDirProp = System.getProperty(TestConfigObject.TC_BASE_DIR);
    if (baseDirProp == null || baseDirProp.trim().equals("")) {
      baseDirProp = System.getProperty("user.dir") + File.separator + "target";
    }
    System.out.println("Using " + baseDirProp + " as Base Directory");
    return baseDirProp;
  }

  public static String jarFor(Class<?> c) {
    ProtectionDomain protectionDomain = c.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource != null) {
      URL url = codeSource.getLocation();
      String path = url.getPath();
      if (System.getProperty("os.name").contains("win") && path.startsWith("/")) {
        path = path.substring(1);
      }
      return URLDecoder.decode(path);
    } else {
      return jarFromClassResource(c);
    }
  }

  private static String jarFromClassResource(Class<?> c) {
    URL clsUrl = c.getResource(c.getSimpleName() + ".class");
    if (clsUrl != null) {
      try {
        URLConnection conn = clsUrl.openConnection();
        if (conn instanceof JarURLConnection) {
          JarURLConnection connection = (JarURLConnection) conn;
          return connection.getJarFileURL().getFile();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    throw new AssertionError("returning null for " + c.getName());
  }
}
