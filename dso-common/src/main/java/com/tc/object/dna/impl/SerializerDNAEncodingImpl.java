/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  @Override
  protected boolean useStringEnumRead(final byte type) {
    return (type == TYPE_ID_ENUM);
  }

  @Override
  protected boolean useClassProvider(final byte type, final byte typeToCheck) {
    return (type == typeToCheck);
  }

  @Override
  protected boolean useUTF8String(final byte type) {
    return (type == TYPE_ID_STRING);
  }

  private static class LocalClassProvider implements ClassProvider {

    private static final String            LOADER_ID   = LocalClassProvider.class.getName() + "::CLASSPROVIDER";
    private static final LoaderDescription LOADER_DESC = new LoaderDescription(null, LOADER_ID);

    // This method assumes the Class is visible in this VM and can be loaded by the same class loader as this
    // object.
    public Class getClassFor(final String className, final LoaderDescription desc) {
      Assert.assertEquals(LOADER_DESC, desc);
      try {
        return Class.forName(className);
      } catch (final ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

    public LoaderDescription getLoaderDescriptionFor(final Class clazz) {
      return LOADER_DESC;
    }

    public ClassLoader getClassLoader(final LoaderDescription loaderDesc) {
      Assert.assertEquals(LOADER_DESC, loaderDesc);
      return ClassLoader.getSystemClassLoader();
    }

    public LoaderDescription getLoaderDescriptionFor(final ClassLoader loader) {
      return LOADER_DESC;
    }

    public void registerNamedLoader(final NamedClassLoader loader, final String appGroup) {
      // do nothing
    }
  }
}
