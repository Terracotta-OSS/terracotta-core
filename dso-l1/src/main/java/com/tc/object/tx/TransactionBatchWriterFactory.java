/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public synchronized ClientTransactionBatch nextBatch(GroupID groupID) {
    return new ClientTransactionBatchWriter(groupID, new TxnBatchID(++batchIDSequence),
                                            new ObjectStringSerializerImpl(), encoding, messageFactory, foldingConfig);
  }

}
