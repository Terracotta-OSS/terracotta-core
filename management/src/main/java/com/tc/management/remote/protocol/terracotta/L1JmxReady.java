/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.util.UUID;

import java.io.IOException;

/**
 * Sending this network message to the L2 signals that the L1 has successfully started its JMX server and registered all
 * of its beans, and can be connected to and interrogated by the L2 server.
 */
public class L1JmxReady extends DSOMessageBase {
  private static final byte UUID = 1;
  private static final byte DOMAINS = 2;

  private UUID              uuid = null;
  private String[]          tunneledDomains = null;

  public L1JmxReady(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel,
                    TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public L1JmxReady(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header,
                    TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(UUID theUUID, String[] domains) {
    this.uuid = theUUID;
    this.tunneledDomains = domains;
  }

  public L1JmxReady createResponse() {
    L1JmxReady rv = (L1JmxReady) getChannel().createMessage(TCMessageType.CLIENT_JMX_READY_MESSAGE);
    rv.uuid = getUUID();
    return rv;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(UUID, uuid.toString());
    putNVPair(DOMAINS, tunneledDomains.length);
    for (String domain : tunneledDomains) {
      getOutputStream().writeString(domain);
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case UUID:
        uuid = new UUID(getStringValue());
        return true;
      case DOMAINS:
        int numberOfDomains = getIntValue();
        tunneledDomains = new String[numberOfDomains];
        for (int i = 0; i < numberOfDomains; i++) {
          tunneledDomains[i] = getInputStream().readString();
        }
        return true;
      default:
        return false;
    }
  }

  public UUID getUUID() {
    return this.uuid;
  }
  
  public String[] getTunneledDomains() {
    return this.tunneledDomains;
  }

}
