/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCSerializable;

/**
 * This interface represents a Node be it an L2 or and L1 and is mostly used an an unified way of identifying an
 * external entity.
 */
public interface NodeID extends TCSerializable, Comparable {

  public static final byte CLIENT_NODE_TYPE = 0x01;
  public static final byte SERVER_NODE_TYPE = 0x02;
  public static final byte GROUP_NODE_TYPE  = 0x03;
  public static final byte STRIPE_NODE_TYPE = 0x04;

  // /////////////////////////////////////////////////////////////////////////////////////////////////////
  // XXX:: NOTE::
  // Any new implementation of this interface should also implement the serialization methods
  // in NodeIDSerializer.
  // /////////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isNull();

  /**
   * This method should return one of the above defined types.
   */
  public byte getNodeType();
}
