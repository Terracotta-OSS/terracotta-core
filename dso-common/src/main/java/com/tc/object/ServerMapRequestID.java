/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

public class ServerMapRequestID extends AbstractIdentifier {

  public static final ServerMapRequestID NULL_ID = new ServerMapRequestID();

  private static final String            ID_TYPE = "ServerMapRequestID";

  public ServerMapRequestID(final long id) {
    super(id);
  }

  private ServerMapRequestID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return ID_TYPE;
  }

}
