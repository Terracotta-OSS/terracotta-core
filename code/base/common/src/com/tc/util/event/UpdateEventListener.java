/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util.event;

import java.util.EventListener;

public interface UpdateEventListener extends EventListener {

  void handleUpdate(UpdateEvent e);
}
