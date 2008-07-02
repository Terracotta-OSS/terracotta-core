/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.model;

import java.beans.PropertyChangeEvent;
import java.util.EventListener;

public interface ServerStateListener extends EventListener {
  void serverStateChanged(IServer server, PropertyChangeEvent pce);
}
