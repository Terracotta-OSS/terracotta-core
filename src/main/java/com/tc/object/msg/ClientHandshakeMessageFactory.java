/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

public interface ClientHandshakeMessageFactory {

  public ClientHandshakeMessage newClientHandshakeMessage(String clientVersion, boolean isEnterpriseClient);

}
