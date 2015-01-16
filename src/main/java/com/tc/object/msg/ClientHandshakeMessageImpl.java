/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.BasicObjectIDSet;
import com.tc.util.ObjectIDSet;
import com.tc.util.SequenceID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClientHandshakeMessageImpl extends DSOMessageBase implements ClientHandshakeMessage {

  private static final byte   MANAGED_OBJECT_IDS       = 1;
  private static final byte   LOCK_CONTEXT             = 2;
  private static final byte   TRANSACTION_SEQUENCE_IDS = 3;
  private static final byte   RESENT_TRANSACTION_IDS   = 4;
  private static final byte   REQUEST_OBJECT_IDS       = 5;
  private static final byte   CLIENT_VERSION           = 6;
  private static final byte   SERVER_HIGH_WATER_MARK   = 7;
  private static final byte   ENTERPRISE_CLIENT        = 8;
  private static final byte   OBJECTS_TO_VALIDATE      = 9;
  private static final byte   LOCAL_TIME_MILLS         = 10;

  private final Set<ClientServerExchangeLockContext> lockContexts             = new HashSet<ClientServerExchangeLockContext>();
  private final List<SequenceID>    sequenceIDs        = new ArrayList<SequenceID>();
  private final List<TransactionID> txnIDs             = new ArrayList<TransactionID>();
  private ObjectIDSet         objectsToValidate        = new BasicObjectIDSet();
  private ObjectIDSet         objectIDs                = new BasicObjectIDSet();
  private long                currentLocalTimeMills    = System.currentTimeMillis();
  private boolean             requestObjectIDs;
  private boolean             enterpriseClient         = false;
  private long                serverHighWaterMark      = 0;
  private String              clientVersion            = "UNKNOW";

  public ClientHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                    MessageChannel channel, TCMessageType messageType) {
    super(sessionID, monitor, out, channel, messageType);
  }

  public ClientHandshakeMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                    TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  public Collection<ClientServerExchangeLockContext> getLockContexts() {
    return this.lockContexts;
  }

  @Override
  public ObjectIDSet getObjectIDs() {
    return objectIDs;
  }

  @Override
  public void setObjectIDs(final ObjectIDSet objectIDs) {
    this.objectIDs = objectIDs;
  }

  @Override
  public ObjectIDSet getObjectIDsToValidate() {
    return this.objectsToValidate;
  }

  @Override
  public void setObjectIDsToValidate(final ObjectIDSet objectIDsToValidate) {
    this.objectsToValidate = objectIDsToValidate;
  }

  @Override
  public List<SequenceID> getTransactionSequenceIDs() {
    return this.sequenceIDs;
  }

  @Override
  public List<TransactionID> getResentTransactionIDs() {
    return this.txnIDs;
  }

  @Override
  public boolean isObjectIDsRequested() {
    return this.requestObjectIDs;
  }

  @Override
  public String getClientVersion() {
    return this.clientVersion;
  }

  @Override
  public void addTransactionSequenceIDs(List<SequenceID> seqIDs) {
    this.sequenceIDs.addAll(seqIDs);
  }

  @Override
  public void addResentTransactionIDs(List<TransactionID> resentTransactionIDs) {
    this.txnIDs.addAll(resentTransactionIDs);
  }

  @Override
  public void setIsObjectIDsRequested(boolean request) {
    this.requestObjectIDs = request;
  }

  @Override
  public void setClientVersion(String version) {
    this.clientVersion = version;
  }

  @Override
  public void addLockContext(ClientServerExchangeLockContext ctxt) {
    this.lockContexts.add(ctxt);
  }

  @Override
  public long getServerHighWaterMark() {
    return this.serverHighWaterMark;
  }

  @Override
  public void setServerHighWaterMark(long serverHWM) {
    this.serverHighWaterMark = serverHWM;
  }

  @Override
  public boolean enterpriseClient() {
    return this.enterpriseClient;
  }

  @Override
  public void setEnterpriseClient(boolean isEnterpriseClient) {
    this.enterpriseClient = isEnterpriseClient;
  }

  @Override
  public long getLocalTimeMills() {
    return this.currentLocalTimeMills;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MANAGED_OBJECT_IDS, objectIDs);
    putNVPair(OBJECTS_TO_VALIDATE, objectsToValidate);
    for (final ClientServerExchangeLockContext lockContext : this.lockContexts) {
      putNVPair(LOCK_CONTEXT, lockContext);
    }
    for (final SequenceID sequenceID : this.sequenceIDs) {
      putNVPair(TRANSACTION_SEQUENCE_IDS, sequenceID);
    }
    for (final TransactionID txnID : this.txnIDs) {
      putNVPair(RESENT_TRANSACTION_IDS, txnID);
    }
    putNVPair(REQUEST_OBJECT_IDS, this.requestObjectIDs);
    putNVPair(ENTERPRISE_CLIENT, this.enterpriseClient);
    putNVPair(CLIENT_VERSION, this.clientVersion);
    putNVPair(SERVER_HIGH_WATER_MARK, this.serverHighWaterMark);
    putNVPair(LOCAL_TIME_MILLS, this.currentLocalTimeMills);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MANAGED_OBJECT_IDS:
        objectIDs = getObject(new BasicObjectIDSet());
        return true;
      case OBJECTS_TO_VALIDATE:
        objectsToValidate = getObject(new BasicObjectIDSet());
        return true;
      case LOCK_CONTEXT:
        this.lockContexts.add(getObject(new ClientServerExchangeLockContext()));
        return true;
      case TRANSACTION_SEQUENCE_IDS:
        this.sequenceIDs.add(new SequenceID(getLongValue()));
        return true;
      case RESENT_TRANSACTION_IDS:
        this.txnIDs.add(new TransactionID(getLongValue()));
        return true;
      case REQUEST_OBJECT_IDS:
        this.requestObjectIDs = getBooleanValue();
        return true;
      case ENTERPRISE_CLIENT:
        this.enterpriseClient = getBooleanValue();
        return true;
      case CLIENT_VERSION:
        this.clientVersion = getStringValue();
        return true;
      case SERVER_HIGH_WATER_MARK:
        this.serverHighWaterMark = getLongValue();
        return true;
      case LOCAL_TIME_MILLS:
        this.currentLocalTimeMills = getLongValue();
        return true;
      default:
        return false;
    }
  }

}
