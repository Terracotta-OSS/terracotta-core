/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.util.AbstractIdentifier;

/**
 * Identifier for a client session
 * 
 * @author steve
 */
public class ChannelID extends AbstractIdentifier {
  /**
   * Indicates no ID
   */
  public static final ChannelID NULL_ID      = new ChannelID();
  
  /**
   * Special ID, indicates L2 server ID (value = Long.MIN_VALUE)
   */
  public static final ChannelID L2_SERVER_ID = new ChannelID(Long.MIN_VALUE);

  /**
   * Construct with specific id value
   * @param id ID value
   */
  public ChannelID(long id) {
    super(id);
  }

  private ChannelID() {
    super();
  }

  public String getIdentifierType() {
    return "ChannelID";
  }
}