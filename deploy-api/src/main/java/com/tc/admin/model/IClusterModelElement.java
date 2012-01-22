/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.beans.PropertyChangeListener;

public interface IClusterModelElement {
  static final String PROP_READY = "ready";

  void addPropertyChangeListener(PropertyChangeListener listener);

  void removePropertyChangeListener(PropertyChangeListener listener);

  boolean isReady();
}
