/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tcverify;

import java.util.Arrays;
import java.util.Collection;

public class DSOVerifierReplaceSystemLoaderTest extends DSOVerifierTest {

  protected String getMainClass() {
    return Client.class.getName();
  }

  protected Collection<String> getExtraJvmArgs() {
    return Arrays.asList(new String[] { "-Djava.system.class.loader=" + SystemLoader.class.getName() /*
                                                                                                      * ,
                                                                                                      * "-XX:+TraceClassLoading"
                                                                                                      */});
  }

  public static class SystemLoader extends ClassLoader {
    public SystemLoader(ClassLoader parent) {
      super(parent);
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

      DSOVerifier.main(args);
    }
  }

}
