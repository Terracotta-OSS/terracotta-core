/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

import com.tc.net.protocol.tcm.ChannelIDProvider;

public class ChannelIDLoggerProvider implements TCLoggerProvider {

  private final ChannelIDProvider cidProvider;

  public ChannelIDLoggerProvider(ChannelIDProvider cidProvider) {
    this.cidProvider = cidProvider;
  }
  
  public TCLogger getLogger(Class clazz) {
    return new ChannelIDLogger(cidProvider, TCLogging.getLogger(clazz));
  }

  public TCLogger getLogger(String name) {
    return new ChannelIDLogger(cidProvider, TCLogging.getLogger(name));
  }

}
