/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.ClientID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.msg.CommitTransactionMessage;
import com.tc.object.tx.TransactionBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestCommitTransactionMessage implements CommitTransactionMessage {

  public final List             setBatchCalls    = new LinkedList();
  public final List             sendCalls        = new LinkedList();
  public ObjectStringSerializer serializer;
  private List                  tcByteBufferList = new ArrayList();
  private ClientID              clientID         = ClientID.NULL_ID;

  public void setChannelID(ClientID clientID) {
    this.clientID = clientID;
  }

  public void setBatch(TransactionBatch batch, ObjectStringSerializer serializer) {
    setBatchCalls.add(batch);
    this.serializer = serializer;

    TCByteBuffer[] tcbb = batch.getData();
    for (int i = 0; i < tcbb.length; i++) {
      tcByteBufferList.add(tcbb[i]);
    }
  }

  public TCByteBuffer[] getBatchData() {
    TCByteBuffer[] tcbb = new TCByteBuffer[tcByteBufferList.size()];
    int count = 0;
    for (Iterator iter = tcByteBufferList.iterator(); iter.hasNext(); count++) {
      tcbb[count] = (TCByteBuffer) iter.next();
    }
    return tcbb;
  }

  public void send() {
    this.sendCalls.add(new Object());
  }

  public ObjectStringSerializer getSerializer() {
    return serializer;
  }

  public ClientID getClientID() {
    return clientID;
  }

}
