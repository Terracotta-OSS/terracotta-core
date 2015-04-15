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
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.ImplementMe;
import com.tc.object.tx.TransactionBatch;

public class TestTransactionBatch implements TransactionBatch {

  private final TCByteBuffer[] batchData;

  public TestTransactionBatch(TCByteBuffer[] batchData) {
    this.batchData = batchData;
  }

  @Override
  public boolean isEmpty() {
    throw new ImplementMe();
  }

  @Override
  public TCByteBuffer[] getData() {
    return batchData;
  }

  @Override
  public void recycle() {
    return;
  }

}
