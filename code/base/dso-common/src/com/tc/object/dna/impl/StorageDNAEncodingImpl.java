/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;

public class StorageDNAEncodingImpl extends BaseDNAEncodingImpl {

  private static final ClassProvider FAILURE_PROVIDER                     = new FailureClassProvider();

  public StorageDNAEncodingImpl() {
    super(FAILURE_PROVIDER);
  }
  
  protected boolean useStringEnumRead(byte type) {
    return false;
  }
  
  protected boolean useClassProvider(byte type, byte typeToCheck) {
    return false;
  }

  protected boolean useUTF8String(byte type) {
    return false;
  }
  
  private static class FailureClassProvider implements ClassProvider {

    public Class getClassFor(String className, String loaderDesc) {
      throw new AssertionError();
    }

    public String getLoaderDescriptionFor(Class clazz) {
      throw new AssertionError();
    }

    public ClassLoader getClassLoader(String loaderDesc) {
      throw new AssertionError();
    }

    public String getLoaderDescriptionFor(ClassLoader loader) {
      throw new AssertionError();
    }

    public void registerNamedLoader(NamedClassLoader loader) {
      throw new AssertionError();
    }
  }

}
