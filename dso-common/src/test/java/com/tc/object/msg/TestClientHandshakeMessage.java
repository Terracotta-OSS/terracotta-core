/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestClientHandshakeMessage extends TestTCMessage implements ClientHandshakeMessage {
  public Set                    clientObjectIds                = new HashSet();
  public Set                    validateObjectIds              = new HashSet();
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

  public Set getObjectIDs() {
    return this.clientObjectIds;
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

  public Collection getLockContexts() {
    return this.lockContexts;
  }

  public List getTransactionSequenceIDs() {
    return this.transactionSequenceIDs;
  }

  public void addTransactionSequenceIDs(List ids) {
    this.transactionSequenceIDs = ids;
    this.setTransactionSequenceIDsCalls.put(this.transactionSequenceIDs);
  }

  public void addResentTransactionIDs(List resentTransactionIDs) {
    this.transactionIDs = resentTransactionIDs;
    this.setTransactionIDsCalls.put(resentTransactionIDs);

  }

  public List getResentTransactionIDs() {
    return this.transactionIDs;
  }

  public void setIsObjectIDsRequested(boolean request) {
    this.requestedObjectIDs = request;
  }

  public boolean isObjectIDsRequested() {
    return this.requestedObjectIDs;
  }

  public String getClientVersion() {
    return this.clientVersion;
  }

  public void setClientVersion(String v) {
    this.clientVersion = v;
  }

  public long getServerHighWaterMark() {
    return 0;
  }

  public void setServerHighWaterMark(long serverHighWaterMark) {
    throw new ImplementMe();
  }

  public void addLockContext(ClientServerExchangeLockContext ctxt) {
    this.lockContexts.add(ctxt);
  }

  public boolean enterpriseClient() {
    return this.enterpriseClient;
  }

  public void setEnterpriseClient(boolean isEnterpirseClient) {
    this.enterpriseClient = isEnterpirseClient;
  }

  public Set getObjectIDsToValidate() {
    return validateObjectIds;
  }

  public long getLocalTimeMills() {
    return System.currentTimeMillis();
  }
}
