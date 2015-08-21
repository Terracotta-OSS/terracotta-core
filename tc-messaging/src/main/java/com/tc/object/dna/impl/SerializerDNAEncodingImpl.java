/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.dna.impl;

import com.tc.object.loaders.ClassProvider;

public class SerializerDNAEncodingImpl extends BaseDNAEncodingImpl {

  private static final ClassProvider LOCAL_PROVIDER = new LocalClassProvider();

  public SerializerDNAEncodingImpl() {
    super(LOCAL_PROVIDER);
  }

  @Override
  protected boolean useStringEnumRead(byte type) {
    return (type == TYPE_ID_ENUM);
  }

  @Override
  protected boolean useClassProvider(byte type, byte typeToCheck) {
    return (type == typeToCheck);
  }

  @Override
  protected boolean useUTF8String(byte type) {
    return (type == TYPE_ID_STRING);
  }

  private static class LocalClassProvider implements ClassProvider {

    // This method assumes the Class is visible in this VM and can be loaded by the same class loader as this
    // object.
    @Override
    public Class<?> getClassFor(String className) {
      try {
        return Class.forName(className);
      } catch (final ClassNotFoundException e) {
        throw new AssertionError(e);
      }
    }

  }
}
