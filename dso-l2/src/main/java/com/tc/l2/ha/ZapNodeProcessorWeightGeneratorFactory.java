/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;

public class ZapNodeProcessorWeightGeneratorFactory extends WeightGeneratorFactory {
  public ZapNodeProcessorWeightGeneratorFactory(final DSOChannelManager channelManager,
                                                final TransactionBatchManager transactionBatchManager,
                                                final ServerTransactionManager serverTransactionManager,
                                                final String host, int port) {
    super();

    add(new ChannelWeightGenerator(channelManager));
    add(new LastTxnTimeWeightGenerator(transactionBatchManager));
    add(new TxnCountWeightGenerator(serverTransactionManager));
    add(new ServerIdentifierWeightGenerator(host, port));
    // add a random generator to break tie
    add(RANDOM_WEIGHT_GENERATOR);

  }
}
