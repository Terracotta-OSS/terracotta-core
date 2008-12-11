/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.TCMessage;

public interface RequestRootMessage extends TCMessage, Recyclable {

  public String getRootName();

  public void initialize(String name);

}