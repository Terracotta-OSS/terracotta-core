/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;

public interface ClientHandshakeMessageFactory {

  public ClientHandshakeMessage newClientHandshakeMessage(NodeID remoteNode, String clientVersion,
                                                          boolean isEnterpriseClient);

}
