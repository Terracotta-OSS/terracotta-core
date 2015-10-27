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
package com.tc.net;

import com.tc.io.TCSerializable;

/**
 * This interface represents a Node be it an L2 or and L1 and is mostly used an an unified way of identifying an
 * external entity.
 */
public interface NodeID extends TCSerializable<NodeID>, Comparable<NodeID> {

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
