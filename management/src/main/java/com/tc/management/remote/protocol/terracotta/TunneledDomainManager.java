/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.TunneledDomainUpdater;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.config.DSOMBeanConfig;

public class TunneledDomainManager implements TunneledDomainUpdater {

  private static final TCLogger       LOGGER = TCLogging.getLogger(TunneledDomainManager.class);

  private final MessageChannel        channel;

  private final DSOMBeanConfig        config;

  private final TunnelingEventHandler tunnelingEventHandler;

  public TunneledDomainManager(final MessageChannel channel, final DSOMBeanConfig config,
                               final TunnelingEventHandler teh) {
    this.channel = channel;
    this.config = config;
    this.tunnelingEventHandler = teh;
  }

  public void sendCurrentTunneledDomains() {
    if (tunnelingEventHandler.isTunnelingReady()) {
      LOGGER
          .info("Sending current registered tunneled domains to L2 server to set up the tunneled connections for the mbeans that match.");
      TunneledDomainsChanged message = (TunneledDomainsChanged) channel
          .createMessage(TCMessageType.TUNNELED_DOMAINS_CHANGED_MESSAGE);
      message.initialize(this.config.getTunneledDomains());
      message.send();
    } else {
      LOGGER.info("Tunneling isn't ready, not sending the tunneled mbean domains.");
    }
  }
}
