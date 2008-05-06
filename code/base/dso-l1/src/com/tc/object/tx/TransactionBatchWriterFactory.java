/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.CommitTransactionMessageFactory;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;

public class TransactionBatchWriterFactory implements TransactionBatchFactory {

  private long                                  batchIDSequence = 0;
  private final CommitTransactionMessageFactory messageFactory;
  private final DNAEncoding                     encoding;
  private final FoldingConfig                   foldingConfig;

  public TransactionBatchWriterFactory(CommitTransactionMessageFactory messageFactory, DNAEncoding encoding,
                                       FoldingConfig foldingConfig) {
    this.messageFactory = messageFactory;
    this.encoding = encoding;
    this.foldingConfig = foldingConfig;
  }

  public synchronized ClientTransactionBatch nextBatch() {
    return new TransactionBatchWriter(new TxnBatchID(++batchIDSequence), new ObjectStringSerializer(), encoding,
                                      messageFactory, foldingConfig);
  }

}
