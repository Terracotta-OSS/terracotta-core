/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.ChannelID;

public interface RequestRootMessage extends Recyclable {

  public String getRootName();

  public void initialize(String name);

  public void send();
  
  public ChannelID getChannelID();

}