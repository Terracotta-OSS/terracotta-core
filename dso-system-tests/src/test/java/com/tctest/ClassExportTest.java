/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassReader;
import com.tc.asm.ClassWriter;
import com.tc.asm.commons.RemappingClassAdapter;
import com.tc.asm.commons.SimpleRemapper;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.Assert;
import com.tctest.runner.AbstractErrorCatchingTransparentApp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ClassExportTest extends TransparentTestBase {

  @Override
  protected Class getApplicationClass() {
    return ClassExportTestApp.class;
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(1);
    t.initializeTestRunner();
  }

  public static class ClassExportTestApp extends AbstractErrorCatchingTransparentApp {

    public ClassExportTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
      super(appId, cfg, listenerProvider);
    }

    public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
      if (config instanceof StandardDSOClientConfigHelper) {
        StandardDSOClientConfigHelper standardConfig = (StandardDSOClientConfigHelper) config;
        
        try {
          ClassWriter cw = new ClassWriter(0);
          ClassAdapter rename = new RemappingClassAdapter(cw, new SimpleRemapper("com/tctest/ExportedClass", "com/tctest/ExportedClassButNot"));
            new ClassReader(getResourceURL(ExportedClass.class).openStream()).accept(rename, 0);
          
          File temp = File.createTempFile("terracotta_class_export_test", ".class");
          temp.deleteOnExit();
          OutputStream out = new FileOutputStream(temp);
          try {
            IOUtils.write(cw.toByteArray(), out);
          } finally {
            out.close();
          }
          standardConfig.addClassResource("com.tctest.ExportedClassButNot", temp.toURL(), false);
        } catch (IOException e) {
          throw new AssertionError(e);
        }
      } else {
        throw new AssertionError();
      }
    }

    @Override
    public void runTest() throws ClassNotFoundException, InterruptedException, ExecutionException {
      Thread.currentThread().getContextClassLoader().loadClass("com.tctest.ExportedClassButNot");
      this.getClass().getClassLoader().loadClass("com.tctest.ExportedClassButNot");
      
      final ClassLoader broken = new ClassLoader(null) {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          if (name.equals("java.lang.Object")) {
            return super.loadClass(name);
          } else {
            throw new AssertionError(name);
          }
        }
      };
      ClassProcessorHelper.setContext(broken, ClassProcessorHelper.getContext(this.getClass().getClassLoader()));

      ExecutorService executor = Executors.newFixedThreadPool(16);

      List<Future<Class>> futures = executor.<Class> invokeAll(Collections
          .<Callable<Class>> nCopies(16, new Callable<Class>() {
            public Class call() throws Exception {
              return broken.loadClass("com.tctest.ExportedClassButNot");
            }
          }));

      Set<Class> defined = new HashSet<Class>();
      for (Future<Class> f : futures) {
        Assert.assertNotNull(f.get());
        defined.add(f.get());
      }
      Assert.assertEquals(1, defined.size());
    }
  }

  private static URL getResourceURL(Class clazz) {
    return clazz.getResource(clazz.getSimpleName() + ".class");
  }
}
