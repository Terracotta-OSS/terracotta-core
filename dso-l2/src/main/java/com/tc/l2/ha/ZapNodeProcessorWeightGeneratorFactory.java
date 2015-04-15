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
