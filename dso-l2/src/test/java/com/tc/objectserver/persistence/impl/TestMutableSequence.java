/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.persistence.impl;

import com.tc.exception.ImplementMe;
import com.tc.util.Assert;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.MutableSequence;

public class TestMutableSequence implements MutableSequence {

  public long                         sequence       = 0;
  public final NoExceptionLinkedQueue nextBatchQueue = new NoExceptionLinkedQueue();

  @Override
  public long next() {
    return ++sequence;
  }

  @Override
  public long current() {
    return sequence;
  }

  @Override
  public long nextBatch(long batchSize) {
    nextBatchQueue.put(new Object[] { Integer.valueOf((int) batchSize) });
    long ls = sequence;
    sequence += batchSize;
    return ls;
  }

  @Override
  public String getUID() {
    throw new ImplementMe();
  }

  @Override
  public void setNext(long next) {
    Assert.assertTrue(this.sequence <= next);
    sequence = next;
  }

}
