/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import com.tc.test.TCTestCase;
import com.tc.util.ProductInfo;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ToolkitModuleTest extends TCTestCase {
  private final String apiVersion;
  private final String tcVersion;

  public ToolkitModuleTest() {
    ProductInfo info = ProductInfo.getInstance();
    apiVersion = info.timApiVersion();
    tcVersion = info.version();
  }

  public void testMajorVersionConflict() throws Exception {
    File tempDir = getTempDirectory();

    makeJar(tempDir, "tim-A", "1.0.0", "org.terracotta.terracotta-toolkit-1.0;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, "tim-B", "1.0.0", "org.terracotta.terracotta-toolkit-2.0;bundle-version:=[1.0.0,1.0.0]");

    Resolver resolver = new Resolver(new String[] { tempDir.getAbsolutePath() }, false, tcVersion, apiVersion);

    Module A = Module.Factory.newInstance();
    A.setName("tim-A");
    A.setVersion("1.0.0");

    Module B = Module.Factory.newInstance();
    B.setName("tim-B");
    B.setVersion("1.0.0");

    resolver.resolve(A);

    try {
      resolver.resolve(B);
      fail();
    } catch (ConflictingModuleException e) {
      // expected
      System.err.println(e.getMessage());
    }
  }

  public void testMinorVersion() throws Exception {
    File tempDir = getTempDirectory();

    makeJar(tempDir, "tim-A", "1.0.0", "org.terracotta.terracotta-toolkit-1.0;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, "tim-B", "1.0.0", "org.terracotta.terracotta-toolkit-1.40;bundle-version:=[1.0.0,1.0.0]");

    Resolver resolver = new Resolver(new String[] { tempDir.getAbsolutePath() }, false, tcVersion, apiVersion);

    Module A = Module.Factory.newInstance();
    A.setName("tim-A");
    A.setVersion("1.0.0");

    Module B = Module.Factory.newInstance();
    B.setName("tim-B");
    B.setVersion("1.0.0");

    resolver.resolve(A);
    resolver.resolve(B);

    ToolkitVersion tookitVersion = resolver.getMaxToolkitVersion();
    assertEquals(1, tookitVersion.getMajor());
    assertEquals(40, tookitVersion.getMinor());
  }

  private File makeJar(File tmpDir, String symName, String version, String requires) throws IOException {
    File out = new File(tmpDir, symName + "-" + version + ".jar");

    ZipOutputStream jar = null;
    try {
      FileOutputStream fout = new FileOutputStream(out);
      jar = new ZipOutputStream(fout);
      jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      jar.write(("Manifest-Version: 1.0\r\n").getBytes());
      jar.write(("Bundle-SymbolicName: org.terracotta.modules." + symName + "\r\n").getBytes());
      jar.write(("Bundle-Version: " + version + "\r\n").getBytes());
      if (requires != null) {
        jar.write(("Require-Bundle: " + requires + "\r\n").getBytes());
      }

      jar.close();
      return out;
    } finally {
      if (jar != null) {
        try {
          jar.close();
        } catch (IOException ioe) {
          //
        }
      }
    }

  }
}
