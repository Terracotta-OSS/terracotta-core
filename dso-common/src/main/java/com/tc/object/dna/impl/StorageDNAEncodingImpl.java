/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.loaders.NamedClassLoader;

public class StorageDNAEncodingImpl extends BaseDNAEncodingImpl {

  private static final ClassProvider FAILURE_PROVIDER = new FailureClassProvider();

  public StorageDNAEncodingImpl() {
    super(FAILURE_PROVIDER);
  }

  @Override
  protected boolean useStringEnumRead(final byte type) {
    return false;
  }

  @Override
  protected boolean useClassProvider(final byte type, final byte typeToCheck) {
    return false;
  }

  @Override
  protected boolean useUTF8String(final byte type) {
    return false;
  }

  private static class FailureClassProvider implements ClassProvider {

    public LoaderDescription getLoaderDescriptionFor(final Class clazz) {
      throw new AssertionError();
    }

    public ClassLoader getClassLoader(final LoaderDescription loaderDesc) {
      throw new AssertionError();
    }

    public LoaderDescription getLoaderDescriptionFor(final ClassLoader loader) {
      throw new AssertionError();
    }

    public Class getClassFor(final String className, final LoaderDescription desc) throws ClassNotFoundException {
      throw new ClassNotFoundException();
    }

    public void registerNamedLoader(final NamedClassLoader loader, final String appGroup) {
      throw new AssertionError();
    }
  }

}
