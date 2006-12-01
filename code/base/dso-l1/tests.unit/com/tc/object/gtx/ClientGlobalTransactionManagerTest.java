/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TestRemoteTransactionManager;
import com.tc.object.tx.TransactionID;

import junit.framework.TestCase;

public class ClientGlobalTransactionManagerTest extends TestCase {

  private ClientGlobalTransactionManagerImpl mgr;

  public void setUp() {
    mgr = new ClientGlobalTransactionManagerImpl(new TestRemoteTransactionManager());
  }

  public void testBasics() throws Exception {
    int max = 5;
    for (int i = 100; i <= max; i++) {
      GlobalTransactionID gtx1 = new GlobalTransactionID(i);
      ChannelID channelID = new ChannelID(i);
      TransactionID transactionID = new TransactionID(i);
      // start the apply
      assertTrue(mgr.startApply(channelID, transactionID, gtx1));
      // a further call to startApply should return false, since the apply is already in progress or complete.
      assertFalse(mgr.startApply(channelID, transactionID, gtx1));

      if (i > 2) {
        GlobalTransactionID lowWatermark = new GlobalTransactionID(i - 1);
        ChannelID chIDBelowWatermark = new ChannelID(i - 2);
        TransactionID txIDBelowWatermark = new TransactionID(i - 2);
        GlobalTransactionID belowLowWatermark = new GlobalTransactionID(i - mgr.getAllowedLowWaterMarkDelta());
        mgr.setLowWatermark(lowWatermark);

        try {
          mgr.startApply(chIDBelowWatermark, txIDBelowWatermark, belowLowWatermark);
          fail("Should have thrown an UnknownTransactionError");
        } catch (UnknownTransactionError e) {
          // expected
        }
      }
    }
  }

  public void testCleanup() throws Exception {
    ChannelID channelID = new ChannelID(1);
    TransactionID txID = new TransactionID(1);
    GlobalTransactionID gtxID1 = new GlobalTransactionID(1);
    GlobalTransactionID gtxID2 = new GlobalTransactionID(2);
    GlobalTransactionID gtxID3 = new GlobalTransactionID(3 + mgr.getAllowedLowWaterMarkDelta());

    assertEquals(0, mgr.size());
    assertTrue(mgr.startApply(channelID, txID, gtxID1));
    assertEquals(1, mgr.size());

    // calling startApply with a different GlobalTransactionID should have the same result as calling it with the
    // same GlobalTransactionID.
    assertFalse(mgr.startApply(channelID, txID, gtxID1));
    assertFalse(mgr.startApply(channelID, txID, gtxID2));
    assertEquals(1, mgr.size());

    // setting the low watermark to a gtxID equal to the lowest recorded for that pair should not remove that pair
    mgr.setLowWatermark(gtxID1);
    assertFalse(mgr.startApply(channelID, txID, gtxID2));
    assertEquals(1, mgr.size());

    // setting the low watermark to a gtxID above the highest recorded for that pair SHOULD remove that pair
    // NOTE: The server should never re-send a transaction with the lower GlobalTransactionID: the only reason it will
    // send a different global transaction id is if the server crashed before it was able to commit the earlier global
    // transaction id => server transaction id mapping. In that case, the server will never send the lower gtx as the
    // low watermark because the mapping doesn't exist in the server (because it didn't get committed)
    mgr.setLowWatermark(gtxID3);
    assertEquals(0, mgr.size());

    // NOTE: the following should never happen in practical use, but is useful to make sure that the cleanup is
    // happening properly
    // The mgr should have forgotten about the channel id and transaction id by setting the watermark above the highest
    // global transaction id recorded for that transaction.
    assertTrue(mgr.startApply(channelID, txID, gtxID3));
  }
}
