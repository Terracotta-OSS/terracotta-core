/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
