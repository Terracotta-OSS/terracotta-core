/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCSerializable;

import java.io.Externalizable;

/**
 * This interface represents a Node be it an L2 or and L1 and is mostly used an an unified way of identifying an
 * external entity.
 */
public interface NodeID extends Externalizable, TCSerializable, Comparable {

  public static final byte L1_NODE_TYPE = 0x01;
  public static final byte L2_NODE_TYPE = 0x02;

  // /////////////////////////////////////////////////////////////////////////////////////////////////////
  // XXX:: NOTE::
  // 1) Any new implementation of this interface should also implement the serialization methods
  // in NodeIDSerializer.
  // 2) Also note that the Externalizable methods should assume that it is getting a TCObjectOutput
  // stream as input and hence should not try to serialize any arbitrary objects, only literal objects are
  // supported. This is done for faster processing and effective serialization plus today there are no complex
  // types contained in NodeID implementations.
  // 3) These classes should implement two different serialization methods since they are used in two different
  // stack implementations. Someday when we move to one comms stack, there is a huge clean up waiting here.
  //
  // /////////////////////////////////////////////////////////////////////////////////////////////////////

  public boolean isNull();

  /**
   * This method should return one of the above defined types.
   */
  public byte getType();
}
