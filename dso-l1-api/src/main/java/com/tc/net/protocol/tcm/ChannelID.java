/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
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