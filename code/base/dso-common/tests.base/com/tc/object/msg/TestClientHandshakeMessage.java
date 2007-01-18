/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestClientHandshakeMessage implements ClientHandshakeMessage {
  public Set                    clientObjectIds                = new HashSet();
  public Set                    waitContexts                   = new HashSet();
  public NoExceptionLinkedQueue sendCalls                      = new NoExceptionLinkedQueue();
  public ChannelID              channelID;
  public List                   lockContexts                   = new ArrayList();
  public List                   pendingLockContexts            = new ArrayList();
  public boolean                isChangeListener;
  public boolean                requestedObjectIDs;
  public NoExceptionLinkedQueue setTransactionSequenceIDsCalls = new NoExceptionLinkedQueue();
  public NoExceptionLinkedQueue setTransactionIDsCalls         = new NoExceptionLinkedQueue();
  public Collection             transactionSequenceIDs         = new ArrayList();
  public Collection             transactionIDs                 = new ArrayList();
  private TestMessageChannel    channel;

  public void addObjectID(ObjectID id) {
    clientObjectIds.add(id);
  }

  public void send() {
    sendCalls.put(new Object());
  }

  public MessageChannel getChannel() {
    synchronized (this) {
      if (channel == null) {
        channel = new TestMessageChannel();
        channel.channelID = channelID;
      }

      return channel;
    }
  }

  public ChannelID getChannelID() {
    return this.channelID;
  }

  public Collection getObjectIDs() {
    return clientObjectIds;
  }

  public int getCorrelationId(boolean initialize) {
    throw new ImplementMe();
  }

  public void setCorrelationId(int id) {
    throw new ImplementMe();
  }

  public TCMessageType getMessageType() {
    throw new ImplementMe();
  }

  public void hydrate() {
    //
  }

  public void dehydrate() {
    //
  }

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

  public void resend() {
    throw new ImplementMe();

  }

  public void addPendingLockContext(LockContext ctxt) {
    this.pendingLockContexts.add(ctxt);
  }

  public Collection getPendingLockContexts() {
    return pendingLockContexts;
  }

  public Collection getTransactionSequenceIDs() {
    return this.transactionSequenceIDs;
  }

  public void setTransactionSequenceIDs(Collection transactionSequenceIDs) {
    this.transactionSequenceIDs = transactionSequenceIDs;
    this.setTransactionSequenceIDsCalls.put(transactionSequenceIDs);
  }

  public void setResentTransactionIDs(Collection resentTransactionIDs) {
    this.transactionIDs = resentTransactionIDs;
    this.setTransactionIDsCalls.put(resentTransactionIDs);

  }

  public Collection getResentTransactionIDs() {
    return transactionIDs;
  }

  public void setIsObjectIDsRequested(boolean request) {
    this.requestedObjectIDs = request;
  }

  public boolean isObjectIDsRequested() {
    return requestedObjectIDs;
  }
}
