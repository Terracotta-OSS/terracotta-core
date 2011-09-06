/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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

    makeJar(tempDir, null, "tim-A", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.0;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, null, "tim-B", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-2.0;bundle-version:=[1.0.0,1.0.0]");

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

  public void testNewRefAfterFreeze() throws Exception {
    File tempDir = getTempDirectory();

    makeJar(tempDir, null, "tim-A", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.1;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, null, "tim-B", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.2;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, "org.terracotta.toolkit", "terracotta-toolkit-1.1", "1.0.0", null);

    Resolver resolver = new Resolver(new String[] { tempDir.getAbsolutePath() }, false, tcVersion, apiVersion/* , repos */);

    Module A = Module.Factory.newInstance();
    A.setName("tim-A");
    A.setVersion("1.0.0");

    resolver.resolve(A);
    URL url = resolver.attemptToolkitFreeze();
    Assert.assertNotNull(url);
    url = resolver.attemptToolkitFreeze();
    Assert.assertNull(url);

    ToolkitVersion tookitVersion = resolver.getMaxToolkitVersion();
    assertEquals(1, tookitVersion.getMajor());
    assertEquals(1, tookitVersion.getMinor());

    Module B = Module.Factory.newInstance();
    B.setName("tim-B");
    B.setVersion("1.0.0");
    try {
      resolver.resolve(B);
      fail();
    } catch (ConflictingModuleException e) {
      // expected
      System.err.println(e.getMessage());
    }
  }

  public void testTopLevelConflict() throws Exception {
    File tempDir = getTempDirectory();

    makeJar(tempDir, "org.terracotta.toolkit", "terracotta-toolkit-1.1", "1.0.0", null);
    makeJar(tempDir, null, "tim-A", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.5;bundle-version:=[1.0.0,1.0.0]");

    Resolver resolver = new Resolver(new String[] { tempDir.getAbsolutePath() }, false, tcVersion, apiVersion/* , repos */);

    Module tk = Module.Factory.newInstance();
    tk.setGroupId("org.terracotta.toolkit");
    tk.setName("terracotta-toolkit-1.1");
    tk.setVersion("1.0.0");

    resolver.resolve(tk);
    URL url = resolver.attemptToolkitFreeze();
    Assert.assertNull(url);

    ToolkitVersion tookitVersion = resolver.getMaxToolkitVersion();
    assertEquals(1, tookitVersion.getMajor());
    assertEquals(1, tookitVersion.getMinor());

    Module A = Module.Factory.newInstance();
    A.setName("tim-A");
    A.setVersion("1.0.0");
    try {
      resolver.resolve(A);
      fail();
    } catch (ConflictingModuleException e) {
      // expected
      System.err.println(e.getMessage());
    }
  }

  public void testMinorVersion() throws Exception {
    File tempDir = getTempDirectory();

    makeJar(tempDir, null, "tim-A", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.0;bundle-version:=[1.0.0,1.0.0]");
    makeJar(tempDir, null, "tim-B", "1.0.0",
            "org.terracotta.toolkit.terracotta-toolkit-1.40;bundle-version:=[1.0.0,1.0.0]");

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

  private File makeJar(File tmpDir, String groupId, String name, String version, String requires) throws IOException {
    File out = new File(tmpDir, name + "-" + version + ".jar");

    if (groupId == null) {
      groupId = "org.terracotta.modules";
    }

    ZipOutputStream jar = null;
    try {
      FileOutputStream fout = new FileOutputStream(out);
      jar = new ZipOutputStream(fout);
      jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      jar.write(("Manifest-Version: 1.0\r\n").getBytes());
      jar.write(("Bundle-SymbolicName: " + groupId + "." + name + "\r\n").getBytes());
      jar.write(("Bundle-Version: " + version + "\r\n").getBytes());
      jar.write(("Terracotta-RequireVersion: " + ProductInfo.getInstance().version() + "\r\n").getBytes());

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
