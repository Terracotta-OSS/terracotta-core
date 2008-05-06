/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.test.TCTestCase;

public class TransactionBatchManagerImplTest extends TCTestCase {

  public TransactionBatchManagerImplTest() {
    super();
  }

  // private TransactionBatchManagerImpl mgr;
  //
  // protected void setUp() throws Exception {
  // super.setUp();
  // this.mgr = new TransactionBatchManagerImpl();
  // }

  public void tests() throws Exception {
    // ChannelID channelID1 = new ChannelID(1);
    // TransactionID txID1 = new TransactionID(1);
    // TxnBatchID batchID1 = new TxnBatchID(1);
    //
    // int count = 10;
    //
    // List batchDescriptors = new LinkedList();
    // for (int i = 1; i <= 2; i++) {
    // batchDescriptors.add(new BatchDescriptor(new ChannelID(i), new TxnBatchID(1), count));
    // batchDescriptors.add(new BatchDescriptor(new ChannelID(i), new TxnBatchID(2), count));
    // }
    //
    // // make sure that you can't call batchComponentComplete unless define batch has been called for that
    // // batch already.
    // try {
    // mgr.batchComponentComplete(channelID1, batchID1, txID1);
    // fail("Expected a NoSuchBatchException");
    // } catch (NoSuchBatchException e) {
    // // expected
    // }
    //
    // // define batches
    // for (Iterator i = batchDescriptors.iterator(); i.hasNext();) {
    // defineBatchFor((BatchDescriptor) i.next());
    // }
    //
    // // make sure that you can't define the same batch more than once
    // for (Iterator i = batchDescriptors.iterator(); i.hasNext();) {
    // try {
    // defineBatchFor((BatchDescriptor) i.next());
    // fail("Expected a BatchDefinedException");
    // } catch (BatchDefinedException e) {
    // // expected
    // }
    // }
    //
    // // call batchComponentComplete() until all the components are complete
    // for (Iterator i = batchDescriptors.iterator(); i.hasNext();) {
    // BatchDescriptor desc = (BatchDescriptor) i.next();
    // for (int j = 1; j <= desc.count; j++) {
    // boolean isComplete = j == desc.count;
    // assertEquals(isComplete, mgr.batchComponentComplete(desc.channelID, desc.batchID, new TransactionID(j)));
    // }
    // }
    //
    // // XXX: This is a bit of a weird way to test this.
    // // Now that the batches have been completed, you should be able to define them again
    // for (Iterator i = batchDescriptors.iterator(); i.hasNext();) {
    // defineBatchFor((BatchDescriptor) i.next());
    // }
    //
    // // now kill a client
    // BatchDescriptor desc = (BatchDescriptor) batchDescriptors.get(0);
    // mgr.shutdownClient(desc.channelID);
    // for (Iterator batchDesIter = batchDescriptors.iterator(); batchDesIter.hasNext();) {
    // BatchDescriptor bd = (BatchDescriptor) batchDesIter.next();
    // if (bd.channelID.equals(desc.channelID)) {
    // for (int i = 1; i <= desc.count; i++) {
    // // batchComponentComplete should never return true...
    // assertFalse(mgr.batchComponentComplete(desc.channelID, desc.batchID, new TransactionID(i)));
    // }
    // }
    // }
    //
    // // now calling batchComponentComplete on an undefined batch if the client has been killed should throw and
    // exception
    // for (int i = 1; i <= desc.count; i++) {
    // try {
    // mgr.batchComponentComplete(desc.channelID, desc.batchID, new TransactionID(i));
    // fail("Expected a NoSuchBatchException");
    // } catch (NoSuchBatchException e) {
    // // expected
    // }
    // }
    //
  }
  //
  // private void defineBatchFor(BatchDescriptor desc) throws Exception {
  // mgr.defineBatch(desc.channelID, desc.batchID, desc.count);
  // }
  //
  // private final class BatchDescriptor {
  // public final ChannelID channelID;
  // public final TxnBatchID batchID;
  // public final int count;
  //
  // private BatchDescriptor(ChannelID channelID, TxnBatchID batchID, int count) {
  // this.channelID = channelID;
  // this.batchID = batchID;
  // this.count = count;
  // }
  // }

}
