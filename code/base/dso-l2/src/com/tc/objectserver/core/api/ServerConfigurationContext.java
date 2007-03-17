/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.core.api;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.DSOChannelManager;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.tx.ServerTransactionManager;
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
  public static final String RECALL_OBJECTS_STAGE                               = "recall_objects_stage";
  public static final String L2_STATE_CHANGE_STAGE                              = "l2_state_change_stage";
  public static final String OBJECTS_SYNC_STAGE                                 = "objects_sync_stage";
  public static final String OBJECTS_SYNC_DEHYDRATE_STAGE                       = "objects_sync_dehydrate_stage";
  public static final String OBJECTS_SYNC_SEND_STAGE                            = "object_sync_send_stage";
  public static final String TRANSACTION_RELAY_STAGE                            = "transaction_relay_stage";
  public static final String SERVER_TRANSACTION_ACK_PROCESSING_STAGE            = "server_transaction_ack_processing_stage";

  public L2Coordinator getL2Coordinator();

  public ObjectManager getObjectManager();

  public LockManager getLockManager();

  public DSOChannelManager getChannelManager();

  public ClientStateManager getClientStateManager();

  public ServerTransactionManager getTransactionManager();

  public TransactionalObjectManager getTransactionalObjectManager();

  public ManagedObjectStore getObjectStore();

  public ServerClientHandshakeManager getClientHandshakeManager();

  public ChannelStats getChannelStats();

  public TransactionBatchReaderFactory getTransactionBatchReaderFactory();

  public ObjectRequestManager getObjectRequestManager();

}
