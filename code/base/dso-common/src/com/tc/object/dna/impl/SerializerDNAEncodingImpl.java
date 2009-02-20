/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;
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
    private static final LoaderDescription LOADER_DESC = new LoaderDescription(null, LOADER_ID);

    // This method assumes the Class is visible in this VM and can be loaded by the same class loader as this
    // object. 
    public Class getClassFor(String className, LoaderDescription desc) {
      Assert.assertEquals(LOADER_DESC, desc);
      try {
        return Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    public LoaderDescription getLoaderDescriptionFor(Class clazz) {
      return LOADER_DESC;
    }

    public ClassLoader getClassLoader(LoaderDescription loaderDesc) {
      Assert.assertEquals(LOADER_DESC, loaderDesc);
      return ClassLoader.getSystemClassLoader();
    }

    public LoaderDescription getLoaderDescriptionFor(ClassLoader loader) {
      return LOADER_DESC;
    }

    public void registerNamedLoader(NamedClassLoader loader) {
      // do nothing
    }

    public void registerNamedLoader(NamedClassLoader loader, String appGroup) {
      // do nothing
    }
  }
}
