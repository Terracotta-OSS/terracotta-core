/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.logging;

import com.tc.object.ClientIDProvider;

public class ClientIDLogger extends BaseMessageDecoratorTCLogger {

  private final ClientIDProvider cidp;

  public ClientIDLogger(ClientIDProvider clientIDProvider, TCLogger logger) {
    super(logger);
    this.cidp = clientIDProvider;
  }

  protected Object decorate(Object msg) {
    return cidp.getClientID() + ": " + msg;
  }

}
