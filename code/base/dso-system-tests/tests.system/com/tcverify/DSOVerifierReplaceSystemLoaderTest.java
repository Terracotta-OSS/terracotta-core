/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import java.util.Arrays;
import java.util.Collection;

public class DSOVerifierReplaceSystemLoaderTest extends DSOVerifierTest {

  @Override
  protected String getMainClass() {
    return Client.class.getName();
  }

  @Override
  protected Collection<String> getExtraJvmArgs() {
    return Arrays.asList(new String[] { "-Djava.system.class.loader=" + SystemLoader.class.getName() });
  }

  public static class SystemLoader extends ClassLoader {
    public SystemLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
      // this method here just so it will get the class export hook
      return super.loadClass(name);
    }

  }

  public static class Client {
    public static void main(String[] args) {
      Class<SystemLoader> expectedType = SystemLoader.class;
      Class<? extends ClassLoader> actualType = ClassLoader.getSystemClassLoader().getClass();

      if (expectedType != actualType) {
        //
        throw new RuntimeException("System loader not replaced, expected " + expectedType + ", but was " + actualType);
      }

      try {
        Class<?> c = ClassLoader.getSystemClassLoader().loadClass("org.terracotta.modules.DummyExportedClass");
        if (c.getClassLoader() != ClassLoader.getSystemClassLoader()) {
          //
          throw new AssertionError("replaced loader should have defined exported class, was: " + c.getClassLoader());
        }
      } catch (ClassNotFoundException cnfe) {
        throw new RuntimeException(cnfe);
      }

      DSOVerifier.main(args);
    }
  }

}
