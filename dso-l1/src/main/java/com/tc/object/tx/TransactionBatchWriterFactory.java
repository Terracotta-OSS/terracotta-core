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
package com.tc.object.tx;

import com.tc.net.GroupID;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;

public class TransactionBatchWriterFactory implements TransactionBatchFactory {

  private long                                  batchIDSequence = 0;
  private final CommitTransactionMessageFactory messageFactory;
  private final DNAEncodingInternal             encoding;
  private final FoldingConfig                   foldingConfig;

  public TransactionBatchWriterFactory(CommitTransactionMessageFactory messageFactory, DNAEncodingInternal encoding,
                                       FoldingConfig foldingConfig) {
    this.messageFactory = messageFactory;
    this.encoding = encoding;
    this.foldingConfig = foldingConfig;
  }

  @Override
  public synchronized ClientTransactionBatch nextBatch(GroupID groupID) {
    return new ClientTransactionBatchWriter(groupID, new TxnBatchID(++batchIDSequence),
                                            new ObjectStringSerializerImpl(), encoding, messageFactory, foldingConfig);
  }

  
    @Override
  public boolean isFoldingSupported() {
      return this.foldingConfig.isFoldingEnabled();
  }
}
