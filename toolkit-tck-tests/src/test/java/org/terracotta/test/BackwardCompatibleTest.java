/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.test;

import org.junit.Test;

import com.sun.tdk.signaturetest.ToolkitSignatureTest;
import com.sun.tdk.signaturetest.ToolkitSignatureTestConfig;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

public class BackwardCompatibleTest {
  private static String javaRuntime;
  private static File   baseDir;
  private static String jdkVersion = System.getProperty("java.specification.version");
  private static File   toolkitRuntimeJar;

  static {
    javaRuntime = System.getProperty("java.home") + "/lib/rt.jar";
    if (System.getProperty("os.name").contains("Mac")) {
      javaRuntime = System.getProperty("java.home") + "/../Classes/classes.jar";
    }

    try {
      baseDir = new File(System.getProperty("basedir", ".")).getCanonicalFile();
      toolkitRuntimeJar = new File(baseDir, "target" + File.separator + "terracotta-toolkit-runtime.jar");
      if (!toolkitRuntimeJar.exists()) { throw new AssertionError("Toolkit jar not found: " + toolkitRuntimeJar); }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ToolkitSignatureTestConfig setupConfig(String apiVersion, File signatureFile) {
    ToolkitSignatureTestConfig config = new ToolkitSignatureTestConfig();
    config
        .classpath(javaRuntime + File.pathSeparator + toolkitRuntimeJar + File.pathSeparator
                       + System.getProperty("java.class.path")).apiVersion(apiVersion).backwardCompatibleTest(true)
        .signatureFile(signatureFile.getAbsolutePath());
    return config;
  }

  @Test
  public void tckTest() throws Exception {
    File signatureDir = new File(baseDir, "src/signatures");
    File[] apiDirs = signatureDir.listFiles();
    for (File apiDir : apiDirs) {
      String apiVersion = apiDir.getName();
      File signatureFile = new File(apiDir, "reference-" + jdkVersion + ".sig");
      if (!signatureFile.exists()) {
    	  throw new AssertionError("Signature file not found: " + signatureFile);
      }
      ToolkitSignatureTestConfig config = setupConfig(apiVersion, signatureFile);
      config.packages("org.terracotta.toolkit");
      ToolkitSignatureTest tst = new ToolkitSignatureTest(config.getArguments());
      Assert.assertTrue(tst.run());
    }
  }
}
