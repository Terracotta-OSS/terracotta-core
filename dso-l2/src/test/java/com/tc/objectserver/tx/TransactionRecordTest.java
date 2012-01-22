/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.ClientID;

import junit.framework.TestCase;


public class TransactionRecordTest extends TestCase {
  
  
  public void tests() {
    
    final ClientID clientID = new ClientID(1);
    final TransactionRecord record = new TransactionRecord();
    
    assertFalse(record.isComplete());
    
    record.applyAndCommitSkipped();
    
    assertFalse(record.isComplete());
    
    record.broadcastCompleted();
    
    assertFalse(record.isComplete());
    
    record.relayTransactionComplete();
    
    assertFalse(record.isComplete());
    
    record.processMetaDataCompleted();
    
    assertTrue(record.isComplete());
  
    record.addWaitee(clientID);
    
    assertFalse(record.isComplete());
    
    assertFalse(record.isEmpty());
    
    record.remove(clientID);
    
    assertTrue(record.isEmpty());
    
    assertTrue(record.isComplete());
  }

}
