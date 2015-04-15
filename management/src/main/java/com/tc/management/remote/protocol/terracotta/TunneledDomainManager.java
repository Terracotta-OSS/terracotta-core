/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
