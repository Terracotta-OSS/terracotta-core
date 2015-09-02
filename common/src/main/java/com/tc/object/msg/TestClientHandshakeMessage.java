/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.tcm.TestMessageChannel;
import com.tc.net.protocol.tcm.TestTCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;
import com.tc.util.concurrent.NoExceptionLinkedQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestClientHandshakeMessage extends TestTCMessage implements ClientHandshakeMessage {
  public ObjectIDSet            clientObjectIds                = new BitSetObjectIDSet();
  public ObjectIDSet            validateObjectIds              = new BitSetObjectIDSet();
  public NoExceptionLinkedQueue<Object> sendCalls              = new NoExceptionLinkedQueue<Object>();
  public ClientID               clientID;
  public List<ClientServerExchangeLockContext> lockContexts    = new ArrayList<ClientServerExchangeLockContext>();
  public boolean                isChangeListener;
  public boolean                requestedObjectIDs;
  private boolean               enterpriseClient               = false;
  public NoExceptionLinkedQueue<List<SequenceID>> setTransactionSequenceIDsCalls = new NoExceptionLinkedQueue<List<SequenceID>>();
  public NoExceptionLinkedQueue<List<TransactionID>> setTransactionIDsCalls         = new NoExceptionLinkedQueue<List<TransactionID>>();
  public List<SequenceID>                   transactionSequenceIDs         = new ArrayList<SequenceID>();
  public List<TransactionID>                   transactionIDs                 = new ArrayList<TransactionID>();
  private TestMessageChannel    channel;
  private String                clientVersion;
  private final Set<ClientEntityReferenceContext> reconnectReferenceSet = new HashSet<ClientEntityReferenceContext>();
  private final Set<ResendVoltronEntityMessage> resendMessageSet = new HashSet<ResendVoltronEntityMessage>();

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
  public TCMessageType getMessageType() {
    throw new UnsupportedOperationException();
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
  public Collection<ClientServerExchangeLockContext> getLockContexts() {
    return this.lockContexts;
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
  public long getLocalTimeMills() {
    return System.currentTimeMillis();
  }

  @Override
  public void addReconnectReference(ClientEntityReferenceContext context) {
    boolean isNew = this.reconnectReferenceSet.add(context);
    Assert.assertTrue(isNew);
  }

  @Override
  public Collection<ClientEntityReferenceContext> getReconnectReferences() {
    return this.reconnectReferenceSet;
  }

  @Override
  public void addResendMessage(ResendVoltronEntityMessage message) {
    boolean isNew = this.resendMessageSet.add(message);
    Assert.assertTrue(isNew);
  }

  @Override
  public Collection<ResendVoltronEntityMessage> getResendMessages() {
    return this.resendMessageSet;
  }
}
