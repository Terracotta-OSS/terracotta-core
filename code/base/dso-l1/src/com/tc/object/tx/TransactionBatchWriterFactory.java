/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.CommitTransactionMessageFactory;

public class TransactionBatchWriterFactory implements TransactionBatchFactory {

  private long                                  batchIDSequence = 0;
  private final CommitTransactionMessageFactory messageFactory;
  private final DNAEncoding                     encoding;

  public TransactionBatchWriterFactory(CommitTransactionMessageFactory messageFactory, DNAEncoding encoding) {
    this.messageFactory = messageFactory;
    this.encoding = encoding;
  }

  public synchronized ClientTransactionBatch nextBatch() {
    return new TransactionBatchWriter(new TxnBatchID(++batchIDSequence), new ObjectStringSerializer(), encoding,
                                      messageFactory);
  }

}
