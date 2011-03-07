/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
