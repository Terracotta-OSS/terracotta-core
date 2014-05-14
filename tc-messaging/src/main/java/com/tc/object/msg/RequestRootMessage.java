/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.lang.Recyclable;
import com.tc.net.protocol.tcm.TCMessage;

public interface RequestRootMessage extends TCMessage, Recyclable {

  public String getRootName();

  public void initialize(String name);

}
