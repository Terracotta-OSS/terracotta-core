/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.express.tests.toolkit;

import org.terracotta.express.tests.base.AbstractToolkitTestBase;
import org.terracotta.test.util.TestBaseUtil;
import org.terracotta.tests.base.AbstractClientBase;
import org.terracotta.toolkit.ToolkitFactory;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.Assert;

public class TwoToolkitRuntimeTest extends AbstractToolkitTestBase {

  public TwoToolkitRuntimeTest(TestConfig testConfig) {
    super(testConfig);
    testConfig.getClientConfig().setClientClasses(MyClient.class, 1);
  }

  @Override
  protected List<String> getExtraJars() {
    List<String> list = new ArrayList<String>();
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    JarOutputStream target;
    try {
      String path = super.getTempDirectory() + File.separator + "TestClassPath.jar";
      String expressRuntime = TestBaseUtil.jarFor(ToolkitFactory.class);
      target = new JarOutputStream(new FileOutputStream(path), manifest);
      File inputDirectory = new File("META-INF");
      inputDirectory.mkdir();
      File services = new File("META-INF" + File.separator + "services");
      services.mkdir();
      File someFile = new File("META-INF" + File.separator + "services" + File.separator
                               + "org.terracotta.toolkit.api.ToolkitFactoryService");
      someFile.createNewFile();
      Writer writer = new FileWriter(someFile);

      writer.write("org.terracotta.express.tests.toolkit.TestToolkitFactoryService");
      writer.flush();
      writer.close();
      add(services, target);
      target.close();
      list.add(path);
      list.add(expressRuntime);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return list;
  }

  public static class MyClient extends AbstractClientBase {

    public MyClient(String[] args) {
      super(args);
    }

    @Override
    protected void doTest() throws Throwable {
      try {
        ToolkitFactory.createToolkit("toolkit:terracotta://" + getTerracottaUrl());
        Assert.fail("Unexpected behaviour");
      } catch (Exception e) {
        System.err.println("Expected error came!!");
        Assert.assertTrue(e.getMessage().contains("Multiple Toolkit implementation found "));
      }
    }
  }

  private void add(File source, JarOutputStream target) throws IOException {
    BufferedInputStream in = null;
    try {
      if (source.isDirectory()) {
        String name = source.getPath().replace("\\", "/");
        if (!name.isEmpty()) {
          if (!name.endsWith("/")) name += "/";
          JarEntry entry = new JarEntry(name);
          entry.setTime(source.lastModified());
          target.putNextEntry(entry);
          target.closeEntry();
        }
        for (File nestedFile : source.listFiles())
          add(nestedFile, target);
        return;
      }

      JarEntry entry = new JarEntry(source.getPath().replace("\\", "/"));
      entry.setTime(source.lastModified());
      target.putNextEntry(entry);
      in = new BufferedInputStream(new FileInputStream(source));

      byte[] buffer = new byte[1024];
      while (true) {
        int count = in.read(buffer);
        if (count == -1) break;
        target.write(buffer, 0, count);
      }
      target.closeEntry();
    } finally {
      if (in != null) in.close();
    }
  }
}
