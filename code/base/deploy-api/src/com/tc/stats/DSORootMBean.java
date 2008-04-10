/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public interface DSORootMBean {

  public String getRootName();

  public ManagedObjectFacade lookupFacade(int limit) throws NoSuchObjectException;

}
