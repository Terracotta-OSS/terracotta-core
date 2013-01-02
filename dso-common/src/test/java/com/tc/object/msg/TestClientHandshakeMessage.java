/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.exception.ImplementMe;
import com.tc.invalidation.Invalidations;
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
  public Invalidations          validateObjectIds              = new Invalidations();
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

  @Override
  public Collection getLockContexts() {
    return this.lockContexts;
  }

  @Override
  public List getTransactionSequenceIDs() {
    return this.transactionSequenceIDs;
  }

  @Override
  public void addTransactionSequenceIDs(List ids) {
    this.transactionSequenceIDs = ids;
    this.setTransactionSequenceIDsCalls.put(this.transactionSequenceIDs);
  }

  @Override
  public void addResentTransactionIDs(List resentTransactionIDs) {
    this.transactionIDs = resentTransactionIDs;
    this.setTransactionIDsCalls.put(resentTransactionIDs);

  }

  @Override
  public List getResentTransactionIDs() {
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
  public Invalidations getObjectIDsToValidate() {
    return validateObjectIds;
  }

  @Override
  public long getLocalTimeMills() {
    return System.currentTimeMillis();
  }
}
