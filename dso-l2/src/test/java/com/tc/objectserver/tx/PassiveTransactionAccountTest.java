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
package com.tc.objectserver.tx;

import com.tc.net.ClientID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.util.HashSet;

public class PassiveTransactionAccountTest extends TCTestCase {
  public void testBasic() throws Exception {
    ClientID cid = new ClientID(1);
    PassiveTransactionAccount passiveTransactionAccount = new PassiveTransactionAccount(cid);

    ServerTransactionID txId1 = new ServerTransactionID(cid, new TransactionID(1));
    ServerTransactionID txId2 = new ServerTransactionID(cid, new TransactionID(2));

    HashSet<ServerTransactionID> serverTxnsIDs = new HashSet<ServerTransactionID>();
    serverTxnsIDs.add(txId1);
    serverTxnsIDs.add(txId2);

    passiveTransactionAccount.incomingTransactions(serverTxnsIDs);

    // check pending txns
    HashSet<ServerTransactionID> tempServerTxnsIDs = new HashSet<ServerTransactionID>();
    passiveTransactionAccount.addAllPendingServerTransactionIDsTo(tempServerTxnsIDs);

    Assert.assertEquals(2, tempServerTxnsIDs.size());
    Assert.assertTrue(tempServerTxnsIDs.contains(txId1));
    Assert.assertTrue(tempServerTxnsIDs.contains(txId2));

    // just commit tx 1
    Assert.assertFalse(passiveTransactionAccount.skipApplyAndCommit(txId1.getClientTransactionID()));

    tempServerTxnsIDs.clear();
    passiveTransactionAccount.addAllPendingServerTransactionIDsTo(tempServerTxnsIDs);
    Assert.assertEquals(2, tempServerTxnsIDs.size());
    Assert.assertTrue(tempServerTxnsIDs.contains(txId1));
    Assert.assertTrue(tempServerTxnsIDs.contains(txId2));

    // process meta data too
    Assert.assertTrue(passiveTransactionAccount.processMetaDataCompleted(txId1.getClientTransactionID()));

    tempServerTxnsIDs.clear();
    passiveTransactionAccount.addAllPendingServerTransactionIDsTo(tempServerTxnsIDs);
    Assert.assertEquals(1, tempServerTxnsIDs.size());
    Assert.assertTrue(tempServerTxnsIDs.contains(txId2));

    boolean error = false;
    try {
      passiveTransactionAccount.broadcastCompleted(txId2.getClientTransactionID());
    } catch (AssertionError e) {
      error = true;
    }
    Assert.assertTrue(error);

    error = false;
    try {
      passiveTransactionAccount.relayTransactionComplete(txId2.getClientTransactionID());
    } catch (AssertionError e) {
      error = true;
    }
    Assert.assertTrue(error);

    Assert.assertFalse(passiveTransactionAccount.processMetaDataCompleted(txId2.getClientTransactionID()));
    Assert.assertTrue(passiveTransactionAccount.skipApplyAndCommit(txId2.getClientTransactionID()));

    tempServerTxnsIDs.clear();
    passiveTransactionAccount.addAllPendingServerTransactionIDsTo(tempServerTxnsIDs);
    Assert.assertEquals(0, tempServerTxnsIDs.size());
  }
}
