/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.beans.PropertyChangeEvent;
import java.util.EventListener;

public interface ServerStateListener extends EventListener {
  void serverStateChanged(IServer server, PropertyChangeEvent pce);
}
