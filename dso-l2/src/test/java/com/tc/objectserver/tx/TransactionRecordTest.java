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
  
  public void testObjectSync() {
    final ClientID clientID = new ClientID(1);
    final TransactionRecord record = new TransactionRecord(clientID);
    
    assertFalse(record.isComplete());
    assertTrue(!record.isEmpty());
    
    assertTrue(record.remove(clientID));
    
    assertTrue(record.isComplete());
  }

}
