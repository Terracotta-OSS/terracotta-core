/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util.event;

import java.util.EventListener;

public interface UpdateEventListener extends EventListener {

  void handleUpdate(UpdateEvent e);
}
