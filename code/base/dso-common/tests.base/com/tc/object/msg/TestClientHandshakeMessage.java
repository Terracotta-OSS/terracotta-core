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
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestClientHandshakeMessage extends TestTCMessage implements ClientHandshakeMessage {
  public Set                    clientObjectIds                = new HashSet();
  public Set                    waitContexts                   = new HashSet();
  public NoExceptionLinkedQueue sendCalls                      = new NoExceptionLinkedQueue();
  public ClientID               clientID;
  public List                   lockContexts                   = new ArrayList();
  public List                   pendingLockContexts            = new ArrayList();
  public boolean                isChangeListener;
  public boolean                requestedObjectIDs;
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

  public void addLockContext(LockContext ctxt) {
    this.lockContexts.add(ctxt);
  }

  public Collection getLockContexts() {
    return this.lockContexts;
  }

  public Collection getWaitContexts() {
    return this.waitContexts;
  }

  public void addWaitContext(WaitContext ctxt) {
    this.waitContexts.add(ctxt);
  }

  public void addPendingLockContext(LockContext ctxt) {
    this.pendingLockContexts.add(ctxt);
  }

  public Collection getPendingLockContexts() {
    return this.pendingLockContexts;
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

  public void addPendingTryLockContext(TryLockContext ctxt) {
    throw new ImplementMe();

  }

  public Collection getPendingTryLockContexts() {
    return Collections.EMPTY_LIST;
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
}
