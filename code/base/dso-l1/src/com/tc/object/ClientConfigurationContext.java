/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.async.api.StageManager;
import com.tc.async.impl.ConfigurationContextImpl;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.tx.ClientTransactionManager;

public class ClientConfigurationContext extends ConfigurationContextImpl {

  public final static String             LOCK_RESPONSE_STAGE                = "lock_response_stage";
  public final static String             LOCK_RECALL_STAGE                  = "lock_recall_stage";
  public final static String             RECEIVE_ROOT_ID_STAGE              = "receive_root_id_stage";
  public final static String             RECEIVE_OBJECT_STAGE               = "receive_object_stage";
  public final static String             RECEIVE_TRANSACTION_STAGE          = "receive_transaction_stage";
  public final static String             OBJECT_ID_REQUEST_RESPONSE_STAGE   = "object_id_request_response_stage";
  public final static String             RECEIVE_TRANSACTION_COMPLETE_STAGE = "receive_transaction_complete_stage";
  public final static String             HYDRATE_MESSAGE_STAGE              = "hydrate_message_stage";
  public final static String             BATCH_TXN_ACK_STAGE                = "batch_txn_ack_stage";
  public static final String             CONFIG_MESSAGE_STAGE               = "config_message_stage";
  public static final String             CLIENT_COORDINATION_STAGE          = "client_coordination_stage";
  public static final String             JMXREMOTE_TUNNEL_STAGE             = "jmxremote_tunnel_stage";
  public static final String             DMI_STAGE                          = "dmi_stage";

  private final ClientLockManager        lockManager;
  private final RemoteObjectManager      remoteObjectManager;
  private final ClientTransactionManager txManager;
  private final ClientHandshakeManager   clientHandshakeManager;

  public ClientConfigurationContext(StageManager stageManager, ClientLockManager lockManager,
                                    RemoteObjectManager remoteObjectManager, ClientTransactionManager txManager,
                                    ClientHandshakeManager clientHandshakeManager) {
    super(stageManager);
    this.lockManager = lockManager;
    this.remoteObjectManager = remoteObjectManager;
    this.txManager = txManager;
    this.clientHandshakeManager = clientHandshakeManager;
  }

  public ClientLockManager getLockManager() {
    return lockManager;
  }

  public RemoteObjectManager getObjectManager() {
    return remoteObjectManager;
  }

  public ClientTransactionManager getTransactionManager() {
    return txManager;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

}