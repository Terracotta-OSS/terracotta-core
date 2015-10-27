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
package com.tc.net.protocol.transport;

/**
 * A place to keep the various TC Wire protocol type-of-service (TOS) definitions
 * 
 * @author teck
 */
public class TypeOfService {
  public static final byte          TOS_UNSPECIFIED = 0;
  public static final TypeOfService DEFAULT_TOS     = TypeOfService.getInstance(TOS_UNSPECIFIED);

  private final byte                value;

  // TODO: provide methods for testing / setting specific TOS bits

  public boolean isUnspecified() {
    return (0 == value);
  }

  private TypeOfService(byte b) {
    value = b;
  }

  public static TypeOfService getInstance(byte b) {
    // could cache instances here if need be
    return new TypeOfService(b);
  }

  byte getByteValue() {
    return value;
  }
}
