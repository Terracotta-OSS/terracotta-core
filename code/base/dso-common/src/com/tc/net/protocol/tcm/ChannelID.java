/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.util.AbstractIdentifier;

/**
 * Identifier for a client session
 * 
 * @author steve
 */
public class ChannelID extends AbstractIdentifier {
  public static final ChannelID NULL_ID = new ChannelID();

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