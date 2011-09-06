/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.ChannelIDProvider;

public class ClientIDProviderImpl implements ClientIDProvider {

  private final ChannelIDProvider channelIDProvider;

  public ClientIDProviderImpl(ChannelIDProvider channelIDProvider) {
    this.channelIDProvider = channelIDProvider;
  }

  public ClientID getClientID() {
    return new ClientID(channelIDProvider.getChannelID().toLong());
  }

}
