/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.net.protocol.tcm.TestTCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.tx.TransactionID;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestClientHandshakeMessage extends TestTCMessage implements ClientHandshakeMessage {
  public ObjectIDSet            clientObjectIds                = new BitSetObjectIDSet();
  public ObjectIDSet            validateObjectIds              = new BitSetObjectIDSet();
  public NoExceptionLinkedQueue sendCalls                      = new NoExceptionLinkedQueue();
  public ClientID               clientID;
  public List                   lockContexts                   = new ArrayList();
  public boolean                isChangeListener;
  public boolean                requestedObjectIDs;
  private boolean               enterpriseClient               = false;
  public NoExceptionLinkedQueue setTransactionSequenceIDsCalls = new NoExceptionLinkedQueue();
  public NoExceptionLinkedQueue setTransactionIDsCalls         = new NoExceptionLinkedQueue();
  public List                   transactionSequenceIDs         = new ArrayList();
  public List                   transactionIDs                 = new ArrayList();
  private TestMessageChannel    channel;
  private String                clientVersion;

  @Override
  public void send() {
    this.sendCalls.put(new Object());
  }

  @Override
  public MessageChannel getChannel() {
    synchronized (this) {
      if (this.channel == null) {
        this.channel = new TestMessageChannel();
        this.channel.channelID = new ChannelID(this.clientID.toLong());
      }

      return this.channel;
    }
  }

  @Override
  public NodeID getSourceNodeID() {
    return this.clientID;
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return clientObjectIds;
  }

  @Override
  public void setObjectIDs(final ObjectIDSet objectIDs) {
    clientObjectIds = objectIDs;
  }

  @Override
  public void setObjectIDsToValidate(final ObjectIDSet objectIDsToValidate) {
    validateObjectIds = objectIDsToValidate;
  }

  @Override
  public TCMessageType getMessageType() {
    throw new ImplementMe();
  }

  @Override
  public void hydrate() {
    //
  }

  @Override
  public void dehydrate() {
    //
  }

  @Override
  public int getTotalLength() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Collection getLockContexts() {
    return this.lockContexts;
  }

  @Override
  public List<SequenceID> getTransactionSequenceIDs() {
    return this.transactionSequenceIDs;
  }

  @Override
  public void addTransactionSequenceIDs(List<SequenceID> ids) {
    this.transactionSequenceIDs = ids;
    this.setTransactionSequenceIDsCalls.put(this.transactionSequenceIDs);
  }

  @Override
  public void addResentTransactionIDs(List<TransactionID> resentTransactionIDs) {
    this.transactionIDs = resentTransactionIDs;
    this.setTransactionIDsCalls.put(resentTransactionIDs);

  }

  @Override
  public List<com.tc.object.tx.TransactionID> getResentTransactionIDs() {
    return this.transactionIDs;
  }

  @Override
  public void setIsObjectIDsRequested(boolean request) {
    this.requestedObjectIDs = request;
  }

  @Override
  public boolean isObjectIDsRequested() {
    return this.requestedObjectIDs;
  }

  @Override
  public String getClientVersion() {
    return this.clientVersion;
  }

  @Override
  public void setClientVersion(String v) {
    this.clientVersion = v;
  }

  @Override
  public long getServerHighWaterMark() {
    return 0;
  }

  @Override
  public void setServerHighWaterMark(long serverHighWaterMark) {
    throw new ImplementMe();
  }

  @Override
  public void addLockContext(ClientServerExchangeLockContext ctxt) {
    this.lockContexts.add(ctxt);
  }

  @Override
  public boolean enterpriseClient() {
    return this.enterpriseClient;
  }

  @Override
  public void setEnterpriseClient(boolean isEnterpirseClient) {
    this.enterpriseClient = isEnterpirseClient;
  }

  @Override
  public ObjectIDSet getObjectIDsToValidate() {
    return validateObjectIds;
  }

  @Override
  public long getLocalTimeMills() {
    return System.currentTimeMillis();
  }
}
