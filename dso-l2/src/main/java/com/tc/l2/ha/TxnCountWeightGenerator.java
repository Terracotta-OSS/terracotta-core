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

import com.tc.l2.ha.WeightGeneratorFactory.WeightGenerator;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;

public class TxnCountWeightGenerator implements WeightGenerator {
  private final ServerTransactionManager serverTransactionManager;

  public TxnCountWeightGenerator(ServerTransactionManager serverTransactionManager) {
    Assert.assertNotNull(serverTransactionManager);
    this.serverTransactionManager = serverTransactionManager;
  }

  @Override
  public long getWeight() {
    return serverTransactionManager.getTotalNumOfActiveTransactions();
  }

}
