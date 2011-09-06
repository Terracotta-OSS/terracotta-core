/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface RootCreationListener extends EventListener {
  void rootCreated(IBasicObject root);
}
