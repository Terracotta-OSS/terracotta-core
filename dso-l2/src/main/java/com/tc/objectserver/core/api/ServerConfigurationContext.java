/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.GarbageCollectionManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.api.ServerMapRequestManager;
import com.tc.objectserver.clustermetadata.ServerClusterMetaDataManager;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.locks.LockManager;
import com.tc.objectserver.metadata.MetaDataManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.search.IndexManager;
import com.tc.objectserver.search.SearchRequestManager;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.objectserver.tx.TransactionalObjectManager;

public interface ServerConfigurationContext extends ConfigurationContext {

  public final static String APPLY_CHANGES_STAGE                                = "apply_changes_stage";
  public static final String APPLY_COMPLETE_STAGE                               = "apply_complete_stage";
  public final static String BROADCAST_CHANGES_STAGE                            = "broadcast_changes_stage";
  public final static String MANAGED_ROOT_REQUEST_STAGE                         = "send_managed_object_stage";
  public final static String RESPOND_TO_OBJECT_REQUEST_STAGE                    = "respond_to_request_stage";
  public final static String MANAGED_OBJECT_REQUEST_STAGE                       = "managed_object_request_stage";
  public final static String PROCESS_TRANSACTION_STAGE                          = "process_transaction_stage";
  public final static String SYNC_WRITE_TXN_RECVD_STAGE                         = "sync_write_transaction_recvd_stage";
  public final static String TRANSACTION_LOOKUP_STAGE                           = "transaction_lookup_stage";
  public final static String RESPOND_TO_LOCK_REQUEST_STAGE                      = "respond_to_lock_request_stage";
  public final static String REQUEST_LOCK_STAGE                                 = "request_lock_stage";
  public final static String CHANNEL_LIFE_CYCLE_STAGE                           = "channel_life_cycle_stage";
  public final static String OBJECT_ID_BATCH_REQUEST_STAGE                      = "object_id_batch_request_stage";
  public final static String TRANSACTION_ACKNOWLEDGEMENT_STAGE                  = "transaction_acknowledgement_stage";
  public final static String CLIENT_HANDSHAKE_STAGE                             = "client_handshake_stage";
  public final static String CONFIG_MESSAGE_STAGE                               = "config_message_stage";
  public final static String HYDRATE_MESSAGE_SINK                               = "hydrate_message_stage";
  public static final String REQUEST_BATCH_GLOBAL_TRANSACTION_ID_SEQUENCE_STAGE = "request_batch_global_transaction_id_sequence_stage";
  public static final String COMMIT_CHANGES_STAGE                               = "commit_changes_stage";
  public static final String JMX_EVENTS_STAGE                                   = "jmx_events_stage";
  public static final String MANAGED_OBJECT_FAULT_STAGE                         = "managed_object_fault_stage";
  public static final String MANAGED_OBJECT_FLUSH_STAGE                         = "managed_object_flush_stage";
  public static final String JMXREMOTE_TUNNEL_STAGE                             = "jmxremote_tunnel_stage";
  public static final String JMXREMOTE_CONNECT_STAGE                            = "jmxremote_connect_stage";
  public static final String JMXREMOTE_DISCONNECT_STAGE                         = "jmxremote_disconnect_stage";
  public static final String RECALL_OBJECTS_STAGE                               = "recall_objects_stage";
  public static final String L2_STATE_CHANGE_STAGE                              = "l2_state_change_stage";
  public static final String OBJECTS_SYNC_REQUEST_STAGE                         = "object_sync_request_stage";
  public static final String OBJECTS_SYNC_DEHYDRATE_STAGE                       = "objects_sync_dehydrate_stage";
  public static final String OBJECTS_SYNC_SEND_STAGE                            = "object_sync_send_stage";
  public static final String OBJECTS_SYNC_STAGE                                 = "objects_sync_stage";
  public static final String TRANSACTION_RELAY_STAGE                            = "transaction_relay_stage";
  public static final String SERVER_TRANSACTION_ACK_PROCESSING_STAGE            = "server_transaction_ack_processing_stage";
  public static final String GROUP_EVENTS_DISPATCH_STAGE                        = "group_events_dispatch_stage";
  public static final String L2_STATE_MESSAGE_HANDLER_STAGE                     = "l2_state_message_handler_stage";
  public static final String GC_RESULT_PROCESSING_STAGE                         = "gc_result_processing_stage";
  public static final String CLIENT_LOCK_STATISTICS_RESPOND_STAGE               = "client_lock_statistics_respond_stage";
  public static final String CLIENT_LOCK_STATISTICS_ENABLE_DISABLE_STAGE        = "client_lock_statistics_enable_disable_stage";
  public static final String TRANSACTION_LOWWATERMARK_STAGE                     = "transaction_lowwatermark_stage";
  public static final String RECEIVE_GROUP_MESSAGE_STAGE                        = "receive_group_message_stage";
  public static final String GROUP_HANDSHAKE_MESSAGE_STAGE                      = "group_handshake_message_stage";
  public static final String GROUP_DISCOVERY_STAGE                              = "group_discovery_stage";
  public final static String GROUP_HYDRATE_MESSAGE_STAGE                        = "group_hydrate_message_stage";
  public static final String GC_DELETE_FROM_DISK_STAGE                          = "gc_delete_from_disk_stage";
  public final static String CLUSTER_METADATA_STAGE                             = "cluster_metadata_stage";
  public static final String SERVER_MAP_REQUEST_STAGE                           = "server_map_request_stage";
  public static final String SERVER_MAP_RESPOND_STAGE                           = "server_map_response_stage";
  public static final String SERVER_MAP_CAPACITY_EVICTION_STAGE                 = "server_map_capacity_eviction_stage";
  public static final String SERVER_MAP_EVICTION_PROCESSOR_STAGE                = "server_map_eviction_processor_stage";
  public static final String SERVER_MAP_EVICTION_BROADCAST_STAGE                = "server_map_eviction_broadcast_stage";
  public static final String SEARCH_EVENT_STAGE                                 = "search_event_stage";
  public static final String SEARCH_QUERY_REQUEST_STAGE                         = "search_query_request_stage";
  public static final String INVALIDATE_OBJECTS_STAGE                           = "invalidate_objects_stage";
  public static final String VALIDATE_OBJECTS_STAGE                             = "validate_objects_stage";
  public static final String GARBAGE_COLLECT_STAGE                              = "garbage_collect_stage";

  // TODO::Move to enterprise
  public static final String AA_TRANSACTION_WATERMARK_BROADCAST_STAGE           = "aa_transaction_watermark_broadcast_stage";
  public static final String AA_TRANSACTION_WATERMARK_RECEIVE_STAGE             = "aa_transaction_watermark_receive_stage";
  public static final String AA_OBJECT_REQUEST_LOOKUP_STAGE                     = "aa_object_request_lookup_stage";
  public static final String AA_CLOSE_CHANNEL_STAGE                             = "aa_close_channel_stage";

  public static final String INDEXES_SYNC_REQUEST_STAGE                         = "indexes_sync_request_stage";
  public static final String INDEXES_SYNC_STAGE                                 = "indexes_sync_stage";

  public L2Coordinator getL2Coordinator();

  public ObjectManager getObjectManager();

  public ObjectRequestManager getObjectRequestManager();

  public ServerMapRequestManager getServerMapRequestManager();

  public LockManager getLockManager();

  public DSOChannelManager getChannelManager();

  public ClientStateManager getClientStateManager();

  public ServerTransactionManager getTransactionManager();

  public TransactionalObjectManager getTransactionalObjectManager();

  public ManagedObjectStore getObjectStore();

  public ServerClientHandshakeManager getClientHandshakeManager();

  public ChannelStats getChannelStats();

  public TransactionBatchReaderFactory getTransactionBatchReaderFactory();

  public TransactionBatchManager getTransactionBatchManager();

  public ServerGlobalTransactionManager getServerGlobalTransactionManager();

  public ServerClusterMetaDataManager getClusterMetaDataManager();

  public MetaDataManager getMetaDataManager();

  public IndexManager getIndexManager();

  public SearchRequestManager getSearchRequestManager();

  public GarbageCollectionManager getGarbageCollectionManager();
}
