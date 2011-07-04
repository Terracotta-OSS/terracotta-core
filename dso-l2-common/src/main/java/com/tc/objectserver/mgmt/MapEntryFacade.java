/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.mgmt;

/**
 * A facade for an entry in logical DSO map
 */
public interface MapEntryFacade {

  Object getKey();

  Object getValue();

}
