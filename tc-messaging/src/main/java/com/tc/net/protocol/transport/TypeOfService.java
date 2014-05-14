/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
