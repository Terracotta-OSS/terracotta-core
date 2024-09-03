/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
