/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
