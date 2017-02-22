/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.entity;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import java.util.Map;


public class VoltronEntityMultiResponseImpl extends DSOMessageBase implements VoltronEntityMultiResponse {
  private static final byte TRANSACTION_ID = 0;
  private static final byte RESULTS_ID = 1;
  private static final byte RECEIVED_ID = 2;
  
  private List<TransactionID> receivedIDs;
  private List<TransactionID> retiredIDs;
  private Map<TransactionID, byte[]> results;

  private boolean stopAdding;
  
  public VoltronEntityMultiResponseImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public VoltronEntityMultiResponseImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }
  
  @Override
  public synchronized boolean send() {
    return super.send();
  }
    
  public void setReceivedTransactions(TransactionID[] tids) {
    receivedIDs = Arrays.asList(tids);
  }
  
  public void setRetiredTransactions(TransactionID[] tids) {
    retiredIDs = Arrays.asList(tids);
  }
  
  public void setResults(Map<TransactionID, byte[]> r) {
    results = r;
  }

  @Override
  public synchronized boolean addReceived(TransactionID tid) {
    if (!stopAdding) {
      if (receivedIDs == null) {
        receivedIDs = new ArrayList<TransactionID>(128);
      }
      receivedIDs.add(tid);
      return true;
    }
    return false;
  }
  
  @Override
  public synchronized boolean addRetired(TransactionID tid) {
    if (!stopAdding) {
      if (retiredIDs == null) {
        retiredIDs = new ArrayList<TransactionID>(128);
      }
      retiredIDs.add(tid);
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean addResult(TransactionID tid, byte[] result) {
    if (!stopAdding) {
      if (results == null) {
        results = new HashMap<TransactionID, byte[]>();
      }
      results.put(tid, result);
      return true;
    }
    return false;
  }
  
  @Override
  public synchronized TransactionID[] getReceivedTransactions() {
    return (this.receivedIDs != null) ? receivedIDs.toArray(new TransactionID[receivedIDs.size()]) : new TransactionID[0];
  }
  
  @Override
  public synchronized TransactionID[] getRetiredTransactions() {
    return (this.retiredIDs != null) ? retiredIDs.toArray(new TransactionID[retiredIDs.size()]) : new TransactionID[0];
  }

  @Override
  public synchronized Map<TransactionID, byte[]> getResults() {
    return (this.results == null) ? Collections.<TransactionID, byte[]>emptyMap() : Collections.unmodifiableMap(this.results);
  }

  @Override
  public synchronized void stopAdding() {
    stopAdding = true;
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    TCByteBufferInputStream input = getInputStream();
    if (name == RECEIVED_ID) {
      int size = getIntValue();
      receivedIDs = new ArrayList<TransactionID>(size);
      for (int x=0;x<size;x++) {
        receivedIDs.add(new TransactionID(input.readLong()));
      }
      return true;
    } else if (name == TRANSACTION_ID) {
      int size = getIntValue();
      retiredIDs = new ArrayList<TransactionID>(size);
      for (int x=0;x<size;x++) {
        retiredIDs.add(new TransactionID(input.readLong()));
      }
      return true;
    } else {
      int size = getIntValue();
      results = new HashMap<TransactionID, byte[]>();
      for (int x=0;x<size;x++) {
        TransactionID id = new TransactionID(input.readLong());
        byte[] read = new byte[input.readInt()];
        input.readFully(read);
        results.put(id, read);
      }
      return true;
    }
  }

  @Override
  protected void dehydrateValues() {
    TCByteBufferOutputStream outputStream = getOutputStream();
    // We don't want to use the NVpair stuff:  it is horrendously complicated, doesn't work well with all types, and doesn't buy us anything.
    putNVPair(RECEIVED_ID, receivedIDs != null ? receivedIDs.size() : 0);
    if (receivedIDs != null) {
      for (TransactionID tid : receivedIDs) {
        outputStream.writeLong(tid.toLong());
      }
    }
    putNVPair(TRANSACTION_ID, retiredIDs != null ? retiredIDs.size() : 0);
    if (retiredIDs != null) {
      for (TransactionID tid : retiredIDs) {
        outputStream.writeLong(tid.toLong());
      }
    }
    putNVPair(RESULTS_ID, results != null ? results.size() : 0);
    if (results != null) {
      for(Map.Entry<TransactionID, byte[]> entries : results.entrySet()) {
        outputStream.writeLong(entries.getKey().toLong());
        outputStream.writeInt(entries.getValue().length);
        outputStream.write(entries.getValue());
      }
    }
  }
}
