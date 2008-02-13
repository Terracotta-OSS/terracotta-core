/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import java.io.Serializable;

public interface TCGroupMessageListener {
  
  public void messageReceived(Serializable serializable, TCGroupMember member);

}