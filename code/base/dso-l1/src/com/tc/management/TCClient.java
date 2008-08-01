/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.management.beans.TCDumper;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.util.runtime.ThreadIDMap;

public interface TCClient extends TCDumper {

  public ThreadIDMap getThreadIDMap();

  public ClientLockManager getLockManager();

  public void startBeanShell(int port);

}
