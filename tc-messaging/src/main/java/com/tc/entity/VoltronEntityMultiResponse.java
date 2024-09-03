/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.entity;

import com.tc.object.ClientInstanceID;
import com.tc.object.tx.TransactionID;
import com.tc.net.protocol.tcm.TCAction;


/**
 * The generic super-interface for the entity response types since SEDA requires that each thread only process one type of message.
 * This means that the caller needs to down-cast to the specific sub-type, cased on getAckType.
 * In the future, it would be ideal to remove this in favor of a different SEDA implementation.
 */
public interface VoltronEntityMultiResponse extends TCAction {  
  public interface ReplayReceiver {
    void received(TransactionID tid);
    void retired(TransactionID tid);
    void result(TransactionID tid, byte[] result);
    void message(ClientInstanceID cid, byte[] message);
    void message(TransactionID tid, byte[] message);
    void stats(TransactionID tid, long[] message);
  }
  
  int replay(ReplayReceiver receiver);
   
  boolean addReceived(TransactionID tid);
  boolean addRetired(TransactionID tid);
  boolean addResult(TransactionID tid, byte[] result);
  boolean addResultAndRetire(TransactionID tid, byte[] result);
  boolean addServerMessage(ClientInstanceID cid, byte[] message);
  boolean addServerMessage(TransactionID cid, byte[] message);
  boolean addStats(TransactionID cid, long[] timings);
  
  void stopAdding();
  
  boolean startAdding();
  
  default boolean shouldSend() {
    return true;
  }
}
