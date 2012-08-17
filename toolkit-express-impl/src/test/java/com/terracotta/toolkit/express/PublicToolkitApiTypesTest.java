/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.express;

import org.terracotta.agent.repkg.de.schlichtherle.io.FileInputStream;
import org.terracotta.toolkit.Toolkit;

import com.tc.util.runtime.Os;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import junit.framework.TestCase;

public class PublicToolkitApiTypesTest extends TestCase {

  public void testAllPublicTypesAdded() throws Exception {
    String toolkitApiJar = jarFor(Toolkit.class);
    System.out.println("Found toolkit api classes in jar: " + toolkitApiJar);
    ZipInputStream jar = new ZipInputStream(new FileInputStream(new File(toolkitApiJar)));
    ZipEntry entry = null;
    Set<String> missingTypes = new HashSet<String>();
    while ((entry = jar.getNextEntry()) != null) {
      String name = entry.getName().replace("/", ".");
      if (isPublicToolkitApiClass(name)) {
        name = name.replace(".class", "");
        if (!PublicToolkitApiTypes.isClassPublicToolkitApiType(name)) {
          missingTypes.add(name);
          System.out.print(" MISSING : ");
        } else {
          System.out.print("   OK    : ");
        }
        System.out.println(name);
      }
    }
    if (missingTypes.size() > 0) {
      System.out.println("------------------------------------------");
      System.out.println("Following types are missing:");
      System.out.println("------------------------------------------");
      for (String type : missingTypes) {
        System.out.println(type);
      }
      System.out.println("\n\n------------------------------------------");
      System.out.println("Add following in PublicToolkitApiTypes.java:");
      System.out.println("------------------------------------------");
      for (String type : missingTypes) {
        System.out.println("tmpSet.add(" + type.substring(type.lastIndexOf(".") + 1) + ".class.getName());");
      }
      System.out.println("\n\n------------------------------------------");
      fail("Test failed");
    }
    System.out.println("Test passed successfully");

  }

  private boolean isPublicToolkitApiClass(String name) {
    return name != null && name.endsWith(".class") && name.startsWith("org.terracotta.");
  }

  public static String jarFor(Class c) {
    ProtectionDomain protectionDomain = c.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource != null) {
      URL url = codeSource.getLocation();
      String path = url.getPath();
      if (Os.isWindows() && path.startsWith("/")) {
        path = path.substring(1);
      }
      return URLDecoder.decode(path);
    } else {
      return jarFromClassResource(c);
    }
  }

  private static String jarFromClassResource(Class c) {
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
