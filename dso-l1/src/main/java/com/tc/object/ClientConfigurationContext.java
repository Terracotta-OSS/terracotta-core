/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.management.ManagementServicesManager;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.platform.rejoin.RejoinManagerInternal;

public class ClientConfigurationContext extends ConfigurationContextImpl {

  public final static String             LOCK_RESPONSE_STAGE                         = "lock_response_stage";
  public final static String             LOCK_RECALL_STAGE                           = "lock_recall_stage";
  public final static String             RECEIVE_ROOT_ID_STAGE                       = "receive_root_id_stage";
  public final static String             RECEIVE_OBJECT_STAGE                        = "receive_object_stage";
  public final static String             RECEIVE_TRANSACTION_STAGE                   = "receive_transaction_stage";
  public final static String             OBJECT_ID_REQUEST_RESPONSE_STAGE            = "object_id_request_response_stage";
  public final static String             RECEIVE_TRANSACTION_COMPLETE_STAGE          = "receive_transaction_complete_stage";
  public final static String             HYDRATE_MESSAGE_STAGE                       = "hydrate_message_stage";
  public final static String             BATCH_TXN_ACK_STAGE                         = "batch_txn_ack_stage";
  public static final String             CONFIG_MESSAGE_STAGE                        = "config_message_stage";
  public static final String             CLIENT_COORDINATION_STAGE                   = "client_coordination_stage";
  public static final String             CLUSTER_EVENTS_STAGE                        = "cluster_events_stage";
  public static final String             JMXREMOTE_TUNNEL_STAGE                      = "jmxremote_tunnel_stage";
  public static final String             OOO_NET_SEND_STAGE                          = "ooo_net_send_stage";
  public static final String             OOO_NET_RECEIVE_STAGE                       = "ooo_net_receive_stage";
  public static final String             CLUSTER_METADATA_STAGE                      = "cluster_metadata_stage";
  public static final String             RECEIVED_SYNC_WRITE_TRANSACTION_ACK_STAGE   = "received_sync_write_transaction_ack_stage";
  public static final String             CLUSTER_MEMBERSHIP_EVENT_STAGE              = "cluster_membership_event_stage";
  public static final String             RECEIVE_SERVER_MAP_RESPONSE_STAGE           = "receive_server_map_response_stage";
  public static final String             RECEIVE_SEARCH_QUERY_RESPONSE_STAGE         = "receive_search_query_response_stage";
  public static final String             RECEIVE_SEARCH_RESULT_RESPONSE_STAGE        = "receive_search_result_response_stage";
  public static final String             CAPACITY_EVICTION_STAGE                     = "capacity_eviction_stage";
  public static final String             LOCAL_CACHE_TXN_COMPLETE_STAGE              = "local_cache_transaction_complete_stage";
  public static final String             TTI_TTL_EVICTION_STAGE                      = "tti_ttl_eviction_stage";
  public static final String             RECEIVE_INVALIDATE_OBJECTS_STAGE            = "receive_invalidate_objects_stage";
  public static final String             PINNED_ENTRY_FAULT_STAGE                    = "pinned_entry_fault_stage";
  public static final String             RESOURCE_MANAGER_STAGE                      = "resource_manager_stage";
  public static final String             SERVER_EVENT_DELIVERY_STAGE                 = "server_event_delivery_stage";
  public static final String             MANAGEMENT_STAGE                            = "management_stage";

  private final ClientLockManager         lockManager;
  private final RemoteObjectManager       remoteObjectManager;
  private final ClientTransactionManager  txManager;
  private final ClientHandshakeManager    clientHandshakeManager;
  private final ClusterMetaDataManager    clusterMetaDataManager;
  private final RejoinManagerInternal     rejoinManager;
  private final ManagementServicesManager managementServicesManager;

  public ClientConfigurationContext(final StageManager stageManager, final ClientLockManager lockManager,
                                    final RemoteObjectManager remoteObjectManager,
                                    final ClientTransactionManager txManager,
                                    final ClientHandshakeManager clientHandshakeManager,
                                    final ClusterMetaDataManager clusterMetaDataManager,
                                    final RejoinManagerInternal rejoinManager,
                                    final ManagementServicesManager managementServicesManager) {
    super(stageManager);
    this.lockManager = lockManager;
    this.remoteObjectManager = remoteObjectManager;
    this.txManager = txManager;
    this.clientHandshakeManager = clientHandshakeManager;
    this.clusterMetaDataManager = clusterMetaDataManager;
    this.rejoinManager = rejoinManager;
    this.managementServicesManager = managementServicesManager;
  }

  public RejoinManagerInternal getRejoinManager() {
    return rejoinManager;
  }

  public ClientLockManager getLockManager() {
    return this.lockManager;
  }

  public RemoteObjectManager getObjectManager() {
    return this.remoteObjectManager;
  }

  public ClientTransactionManager getTransactionManager() {
    return this.txManager;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return this.clientHandshakeManager;
  }

  public ClusterMetaDataManager getClusterMetaDataManager() {
    return this.clusterMetaDataManager;
  }

  public ManagementServicesManager getManagementServicesManager() {
    return managementServicesManager;
  }

}
