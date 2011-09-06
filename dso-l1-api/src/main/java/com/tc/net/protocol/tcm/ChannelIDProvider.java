/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

/**
 * Provides a ChannelID
 */
public interface ChannelIDProvider {
  /**
   * Get the channel ID of the provider
   * 
   * @return The channel ID
   */
  public ChannelID getChannelID();

}
