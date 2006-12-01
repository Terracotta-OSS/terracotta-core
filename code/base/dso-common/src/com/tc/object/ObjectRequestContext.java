/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.net.protocol.tcm.ChannelID;

import java.util.Set;

public interface ObjectRequestContext {
  
  public ObjectRequestID getRequestID();
  
  public ChannelID getChannelID();
  
  public Set getObjectIDs();
  
  public int getRequestDepth();

}