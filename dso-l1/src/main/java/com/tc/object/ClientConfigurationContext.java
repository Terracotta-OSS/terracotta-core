/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.management.ManagementServicesManager;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;

public class ClientConfigurationContext extends ConfigurationContextImpl {

  public final static String             LOCK_RESPONSE_STAGE                         = "lock_response_stage";
  public final static String             HYDRATE_MESSAGE_STAGE                       = "hydrate_message_stage";
  public static final String             CLIENT_COORDINATION_STAGE                   = "client_coordination_stage";
  public static final String             CLUSTER_EVENTS_STAGE                        = "cluster_events_stage";
  public static final String             JMXREMOTE_TUNNEL_STAGE                      = "jmxremote_tunnel_stage";
  public static final String             CLUSTER_MEMBERSHIP_EVENT_STAGE              = "cluster_membership_event_stage";
  public static final String             MANAGEMENT_STAGE                            = "management_stage";
  public static final String             VOLTRON_ENTITY_RESPONSE_STAGE                      = "request_ack_stage";
  public static final String             SERVER_ENTITY_MESSAGE_STAGE                 = "server_entity_message_stage";

  private final ClientLockManager         lockManager;
  private final ClientEntityManager       entityManager;
  private final ClientHandshakeManager    clientHandshakeManager;
  private final ManagementServicesManager managementServicesManager;

  public ClientConfigurationContext(StageManager stageManager, ClientLockManager lockManager,
                                    ClientEntityManager entityManager,
                                    ClientHandshakeManager clientHandshakeManager,
                                    ManagementServicesManager managementServicesManager) {
    super(stageManager);
    this.lockManager = lockManager;
    this.entityManager = entityManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.managementServicesManager = managementServicesManager;
  }

  public ClientLockManager getLockManager() {
    return this.lockManager;
  }

  public ClientEntityManager getEntityManager() {
    return this.entityManager;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  public ManagementServicesManager getManagementServicesManager() {
    return managementServicesManager;
  }
}
