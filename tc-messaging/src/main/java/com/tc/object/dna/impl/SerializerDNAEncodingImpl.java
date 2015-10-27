/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
