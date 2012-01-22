/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface RootCreationListener extends EventListener {
  void rootCreated(IBasicObject root);
}
