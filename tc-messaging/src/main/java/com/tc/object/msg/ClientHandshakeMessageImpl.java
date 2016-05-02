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
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.entity.ResendVoltronEntityMessage;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.Comparator;


public class ClientHandshakeMessageImpl extends DSOMessageBase implements ClientHandshakeMessage {
  private static final byte   LOCK_CONTEXT             = 1;
  private static final byte   CLIENT_VERSION           = 2;
  private static final byte   ENTERPRISE_CLIENT        = 3;
  private static final byte   LOCAL_TIME_MILLS         = 4;
  private static final byte   RECONNECT_REFERENCES     = 5;
  private static final byte   RESEND_MESSAGES          = 6;
  private static final byte   CLIENT_PID               = 7;
  private static final byte   CLIENT_UUID              = 8;
  private static final byte   CLIENT_NAME              = 9;

  private final Set<ClientServerExchangeLockContext> lockContexts             = new HashSet<ClientServerExchangeLockContext>();
  private long                currentLocalTimeMills    = System.currentTimeMillis();
  private boolean             enterpriseClient         = false;
  private String                uuid                     = com.tc.util.UUID.NULL_ID.toString();
  private String              name                     = "";
  private String              clientVersion            = "UNKNOWN";
  private int                 pid                      = -1;
  private final Set<ClientEntityReferenceContext> reconnectReferences = new HashSet<ClientEntityReferenceContext>();
  private final Set<ResendVoltronEntityMessage> resendMessages = new TreeSet<ResendVoltronEntityMessage>(new Comparator<ResendVoltronEntityMessage>() {
    @Override
    public int compare(ResendVoltronEntityMessage first, ResendVoltronEntityMessage second) {
      return first.getTransactionID().compareTo(second.getTransactionID());
    }
  });

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
  public String getClientVersion() {
    return this.clientVersion;
  }

  @Override
  public void setClientPID(int pid) {
    this.pid = pid;
  }

  @Override
  public int getClientPID() {
    return pid;
  }

  @Override
  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  @Override
  public String getUUID() {
    return this.uuid;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
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
    for (final ClientServerExchangeLockContext lockContext : this.lockContexts) {
      putNVPair(LOCK_CONTEXT, lockContext);
    }
    putNVPair(ENTERPRISE_CLIENT, this.enterpriseClient);
    putNVPair(CLIENT_UUID, this.uuid);
    putNVPair(CLIENT_NAME, this.name);
    putNVPair(CLIENT_VERSION, this.clientVersion);
    putNVPair(CLIENT_PID, this.pid);
    putNVPair(LOCAL_TIME_MILLS, this.currentLocalTimeMills);
    for (final ClientEntityReferenceContext referenceContext : this.reconnectReferences) {
      putNVPair(RECONNECT_REFERENCES, referenceContext);
    }
    for (final ResendVoltronEntityMessage resendMessage : this.resendMessages) {
      putNVPair(RESEND_MESSAGES, resendMessage);
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case LOCK_CONTEXT:
        this.lockContexts.add(getObject(new ClientServerExchangeLockContext()));
        return true;
      case ENTERPRISE_CLIENT:
        this.enterpriseClient = getBooleanValue();
        return true;
      case CLIENT_VERSION:
        this.clientVersion = getStringValue();
        return true;
      case LOCAL_TIME_MILLS:
        this.currentLocalTimeMills = getLongValue();
        return true;
      case RECONNECT_REFERENCES:
        this.reconnectReferences.add(getObject(new ClientEntityReferenceContext()));
        return true;
      case RESEND_MESSAGES:
        this.resendMessages.add(getObject(new ResendVoltronEntityMessage()));
        return true;
      case CLIENT_PID:
        this.pid = getIntValue();
        return true;
      case CLIENT_UUID:
        this.uuid = getStringValue();
        return true;
      case CLIENT_NAME:
        this.name = getStringValue();
        return true;
      default:
        return false;
    }
  }

  @Override
  public void addReconnectReference(ClientEntityReferenceContext context) {
    boolean newAddition = this.reconnectReferences.add(context);
    Assert.assertTrue(newAddition);
  }

  @Override
  public Collection<ClientEntityReferenceContext> getReconnectReferences() {
    return this.reconnectReferences;
  }

  @Override
  public void addResendMessage(ResendVoltronEntityMessage message) {
    boolean newAddition = this.resendMessages.add(message);
    Assert.assertTrue(newAddition);
  }

  @Override
  public Collection<ResendVoltronEntityMessage> getResendMessages() {
    return this.resendMessages;
  }
}
