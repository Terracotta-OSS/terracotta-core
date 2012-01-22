/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

/**
 *
 * @author gkeim
 */
public interface ClientConnectionListener extends EventListener {
  void clientConnected(IClient client);
  void clientDisconnected(IClient client);
}
