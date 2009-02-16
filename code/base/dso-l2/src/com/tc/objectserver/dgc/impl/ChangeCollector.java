/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.dgc.impl;

import com.tc.objectserver.managedobject.ManagedObjectChangeListener;

import java.util.Set;

public interface ChangeCollector extends ManagedObjectChangeListener {

  public final ChangeCollector NULL_CHANGE_COLLECTOR = new NullChangeCollector();

  public Set addNewReferencesTo(Set set);
}