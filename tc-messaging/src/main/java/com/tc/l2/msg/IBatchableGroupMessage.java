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
package com.tc.l2.msg;

import com.tc.net.groups.AbstractGroupMessage;


/**
 * The interfaces of a batch message type which can be used by GroupMessageBatchContext.
 * 
 * @param <E> The underlying batch element type.
 */
public interface IBatchableGroupMessage<E> {
  /**
   * Adds a new element to the existing batch.
   * @param element The new element to add.
   */
  public void addToBatch(E element);

  /**
   * @return The current number of elements in the batch.
   */
  public int getBatchSize();

  /**
   * Casts the message into an AbstractGroupMessage for serialization and transmission over the wire.
   * 
   * @return The receiver.
   */
  public AbstractGroupMessage asAbstractGroupMessage();
}