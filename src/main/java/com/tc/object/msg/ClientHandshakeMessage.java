/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.locks.ClientServerExchangeLockContext;
import java.util.Collection;


public interface ClientHandshakeMessage extends TCMessage {
  void addLockContext(ClientServerExchangeLockContext ctxt);

  Collection<ClientServerExchangeLockContext> getLockContexts();

  void setClientVersion(String v);

  String getClientVersion();

  void setEnterpriseClient(boolean isEnterpirseClient);

  boolean enterpriseClient();

  long getLocalTimeMills();

  void addReconnectReference(ClientEntityReferenceContext context);

  Collection<ClientEntityReferenceContext> getReconnectReferences();
}
