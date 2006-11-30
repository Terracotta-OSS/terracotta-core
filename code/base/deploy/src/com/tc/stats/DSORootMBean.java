/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.stats;

import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.mgmt.ManagedObjectFacade;

public interface DSORootMBean {

  public String getRootName();

  public ManagedObjectFacade lookupFacade(int limit) throws NoSuchObjectException;

}
