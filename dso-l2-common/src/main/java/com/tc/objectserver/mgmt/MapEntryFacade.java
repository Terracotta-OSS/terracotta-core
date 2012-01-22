/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.mgmt;

/**
 * A facade for an entry in logical DSO map
 */
public interface MapEntryFacade {

  Object getKey();

  Object getValue();

}
