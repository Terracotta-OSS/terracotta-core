/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

import java.io.IOException;

/**
 * Sending this network message to the L2 signals that the L1 has new MBeans that should be tunneled through the L2.
 */
public class TunneledDomainsChanged extends DSOMessageBase {
  private static final byte DOMAINS         = 1;

  private String[]          tunneledDomains = null;

  public TunneledDomainsChanged(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TunneledDomainsChanged(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(String[] domains) {
    this.tunneledDomains = domains;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(DOMAINS, tunneledDomains.length);
    for (String domain : tunneledDomains) {
      getOutputStream().writeString(domain);
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
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

  public String[] getTunneledDomains() {
    return this.tunneledDomains;
  }
}
