/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface ServerLogListener extends EventListener {
  void messageLogged(String msg);
}
