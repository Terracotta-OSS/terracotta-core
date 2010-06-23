/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.gtx;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.NullRemoteServerMapManager;
import com.tc.object.tx.TestRemoteTransactionManager;
import com.tc.object.tx.TransactionID;

import junit.framework.TestCase;

public class ClientGlobalTransactionManagerTest extends TestCase {

  private ClientGlobalTransactionManagerImpl mgr;

  @Override
  public void setUp() {
    this.mgr = new ClientGlobalTransactionManagerImpl(new TestRemoteTransactionManager(),
                                                      new NullRemoteServerMapManager());
  }

  public void testBasics() throws Exception {
    final int max = 5;
    for (int i = 100; i <= max; i++) {
      final GlobalTransactionID gtx1 = new GlobalTransactionID(i);
      final ClientID cid = new ClientID(i);
      final TransactionID transactionID = new TransactionID(i);
      // start the apply
      assertTrue(this.mgr.startApply(cid, transactionID, gtx1, GroupID.NULL_ID));
      // a further call to startApply should return false, since the apply is already in progress or complete.
      assertFalse(this.mgr.startApply(cid, transactionID, gtx1, GroupID.NULL_ID));

      if (i > 2) {
        final GlobalTransactionID lowWatermark = new GlobalTransactionID(i - 1);
        final ClientID chIDBelowWatermark = new ClientID(i - 2);
        final TransactionID txIDBelowWatermark = new TransactionID(i - 2);
        final GlobalTransactionID belowLowWatermark = new GlobalTransactionID(i
                                                                              - this.mgr.getAllowedLowWaterMarkDelta());
        this.mgr.setLowWatermark(lowWatermark, GroupID.NULL_ID);

        try {
          this.mgr.startApply(chIDBelowWatermark, txIDBelowWatermark, belowLowWatermark, GroupID.NULL_ID);
          fail("Should have thrown an UnknownTransactionError");
        } catch (final UnknownTransactionError e) {
          // expected
        }
      }
    }
  }

  public void testCleanup() throws Exception {
    final ClientID cid = new ClientID(1);
    final TransactionID txID = new TransactionID(1);
    final GlobalTransactionID gtxID1 = new GlobalTransactionID(1);
    final GlobalTransactionID gtxID2 = new GlobalTransactionID(2);
    final GlobalTransactionID gtxID3 = new GlobalTransactionID(3 + this.mgr.getAllowedLowWaterMarkDelta());

    assertEquals(0, this.mgr.size());
    assertTrue(this.mgr.startApply(cid, txID, gtxID1, GroupID.NULL_ID));
    assertEquals(1, this.mgr.size());

    // calling startApply with a different GlobalTransactionID should have the same result as calling it with the
    // same GlobalTransactionID.
    assertFalse(this.mgr.startApply(cid, txID, gtxID1, GroupID.NULL_ID));
    assertFalse(this.mgr.startApply(cid, txID, gtxID2, GroupID.NULL_ID));
    assertEquals(1, this.mgr.size());

    // setting the low Water mark to a gtxID equal to the lowest recorded for that pair should not remove that pair
    this.mgr.setLowWatermark(gtxID1, GroupID.NULL_ID);
    assertFalse(this.mgr.startApply(cid, txID, gtxID2, GroupID.NULL_ID));
    assertEquals(1, this.mgr.size());

    // setting the low water mark to a gtxID above the highest recorded for that pair SHOULD remove that pair
    // NOTE: The server should never re-send a transaction with the lower GlobalTransactionID: the only reason it will
    // send a different global transaction id is if the server crashed before it was able to commit the earlier global
    // transaction id => server transaction id mapping. In that case, the server will never send the lower GTX as the
    // low water mark because the mapping doesn't exist in the server (because it didn't get committed)
    this.mgr.setLowWatermark(gtxID3, GroupID.NULL_ID);
    assertEquals(0, this.mgr.size());

    // NOTE: the following should never happen in practical use, but is useful to make sure that the cleanup is
    // happening properly
    // The mgr should have forgotten about the channel id and transaction id by setting the watermark above the highest
    // global transaction id recorded for that transaction.
    assertTrue(this.mgr.startApply(cid, txID, gtxID3, GroupID.NULL_ID));
  }
}
