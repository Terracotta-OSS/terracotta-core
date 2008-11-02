/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.handler.LockInfoDumpHandler;
import com.tc.management.beans.TCDumper;

public interface TCClient extends TCDumper, LockInfoDumpHandler {

  public void startBeanShell(int port);

}
