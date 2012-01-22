/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.object.ClientIDProvider;

public class ClientIDLoggerProvider implements TCLoggerProvider {

  private final ClientIDProvider cidProvider;

  public ClientIDLoggerProvider(ClientIDProvider clientIDProvider) {
    this.cidProvider = clientIDProvider;
  }
  
  public TCLogger getLogger(Class clazz) {
    return new ClientIDLogger(cidProvider, TCLogging.getLogger(clazz));
  }

  public TCLogger getLogger(String name) {
    return new ClientIDLogger(cidProvider, TCLogging.getLogger(name));
  }

}
