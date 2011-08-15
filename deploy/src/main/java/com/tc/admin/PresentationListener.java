/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin;

import java.util.EventListener;

public interface PresentationListener extends EventListener {
  void presentationReady(boolean ready);
}
