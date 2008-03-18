/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.util.Assert;

public class SerializerDNAEncodingImpl extends BaseDNAEncodingImpl {

  private static final ClassProvider LOCAL_PROVIDER = new LocalClassProvider();

  public SerializerDNAEncodingImpl() {
    super(LOCAL_PROVIDER);
  }
  
  protected boolean useStringEnumRead(byte type) {
    return (type == TYPE_ID_ENUM);
  }
  
  protected boolean useClassProvider(byte type, byte typeToCheck) {
    return (type == typeToCheck);
  }

  protected boolean useUTF8String(byte type) {
    return (type == TYPE_ID_STRING);
  }
  
  private static class LocalClassProvider implements ClassProvider {

    private static final String LOADER_ID = LocalClassProvider.class.getName() + "::CLASSPROVIDER";

    // This method assumes the Class is visible in this VM and can be loaded by the same class loader as this
    // object. 
    public Class getClassFor(String className, String loaderDesc) {
      Assert.assertEquals(LOADER_ID, loaderDesc);
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    public String getLoaderDescriptionFor(Class clazz) {
      return LOADER_ID;
    }

    public ClassLoader getClassLoader(String loaderDesc) {
      Assert.assertEquals(LOADER_ID, loaderDesc);
      return ClassLoader.getSystemClassLoader();
    }

    public String getLoaderDescriptionFor(ClassLoader loader) {
      return LOADER_ID;
    }

    public void registerNamedLoader(NamedClassLoader loader) {
      // do nothing
    }
  }
}
