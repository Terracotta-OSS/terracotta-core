/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.mgmt;

/**
 * A facade for an entry in logical DSO map
 */
public interface MapEntryFacade {

  Object getKey();

  Object getValue();

}
