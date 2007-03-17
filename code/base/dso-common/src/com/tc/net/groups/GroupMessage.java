/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import java.io.Externalizable;

public interface GroupMessage extends Externalizable {

  public int getType();
    
  public abstract MessageID getMessageID();

  public abstract MessageID inResponseTo();

  public abstract void setMessageOrginator(NodeID n);

  public abstract NodeID messageFrom();

}