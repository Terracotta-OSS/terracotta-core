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
    
    record.addWaitee(clientID);  
    
    record.processMetaDataCompleted();
      
    assertFalse(record.isComplete());
    
    assertFalse(record.isEmpty());
    
    record.remove(clientID);
    
    assertTrue(record.isEmpty());
    
    assertTrue(record.isComplete());
  }

}
