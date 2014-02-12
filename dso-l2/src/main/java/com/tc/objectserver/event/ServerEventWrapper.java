/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.event;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.VersionedServerEvent;

import java.util.Set;

public class ServerEventWrapper {
  enum TYPE {
    BEGIN, END, SERVER_EVENT
  }

  private final TYPE                type;
  private final GlobalTransactionID gtxId;
  private final VersionedServerEvent event;
  private final Set<ClientID>       clients;

  private ServerEventWrapper(final TYPE type, final GlobalTransactionID gtxId, final VersionedServerEvent event,
                             final Set<ClientID> clients) {
    this.type = type;
    this.gtxId = gtxId;
    this.event = event;
    this.clients = clients;
  }

  public static ServerEventWrapper createBeginEvent(final GlobalTransactionID gtxId) {
    return new ServerEventWrapper(TYPE.BEGIN, gtxId, null, null);
  }

  public static ServerEventWrapper createEndEvent(final GlobalTransactionID gtxId) {
    return new ServerEventWrapper(TYPE.END, gtxId, null, null);
  }

  public static ServerEventWrapper createServerEventWrapper(final GlobalTransactionID gtxId,
                                                            final VersionedServerEvent event,
                                                            final Set<ClientID> clients) {
    return new ServerEventWrapper(TYPE.SERVER_EVENT, gtxId, event, clients);
  }

  public TYPE getType() {
    return type;
  }

  public GlobalTransactionID getGtxId() {
    return gtxId;
  }

  public VersionedServerEvent getEvent() {
    return event;
  }

  public Set<ClientID> getClients() {
    return this.clients;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((event == null) ? 0 : event.hashCode());
    result = prime * result + ((gtxId == null) ? 0 : gtxId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ServerEventWrapper other = (ServerEventWrapper) obj;
    if (event == null) {
      if (other.event != null) return false;
    } else if (!event.equals(other.event)) return false;
    if (gtxId == null) {
      if (other.gtxId != null) return false;
    } else if (!gtxId.equals(other.gtxId)) return false;
    if (type != other.type) return false;
    return true;
  }

  @Override
  public String toString() {
    return "ServerEventWrapper [type=" + type + ", gtxId=" + gtxId + ", event=" + event + "]";
  }

}
