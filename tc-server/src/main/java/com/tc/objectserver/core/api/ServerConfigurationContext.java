/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.objectserver.core.api;

import com.tc.async.api.ConfigurationContext;
import com.tc.l2.api.L2Coordinator;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;

public interface ServerConfigurationContext extends ConfigurationContext {
  public final static String SINGLE_THREADED_FAST_PATH                          = "single_threaded_fastpath";
  public final static String RESPOND_TO_REQUEST_STAGE                           = "respond_to_request_stage";
  public final static String MONITOR_STAGE                           = "monitor_stage";
  public final static String HYDRATE_MESSAGE_STAGE                              = "hydrate_message_stage";
  public final static String VOLTRON_MESSAGE_STAGE                              = "voltron_message_stage";
  public final static String CLIENT_HANDSHAKE_STAGE                             = "client_handshake_stage";
  public static final String L2_STATE_CHANGE_STAGE                              = "l2_state_change_stage";
  public static final String GROUP_EVENTS_DISPATCH_STAGE                        = "group_events_dispatch_stage";
  public static final String L2_STATE_MESSAGE_HANDLER_STAGE                     = "l2_state_message_handler_stage";
  public static final String L2_STATE_ELECTION_HANDLER                                   = "l2_election_handler_stage";
  public static final String RECEIVE_GROUP_MESSAGE_STAGE                        = "receive_group_message_stage";
  public static final String GROUP_HANDSHAKE_MESSAGE_STAGE                      = "group_handshake_message_stage";
  public static final String GROUP_DISCOVERY_STAGE                              = "group_discovery_stage";
  
  public static final String REQUEST_PROCESSOR_STAGE                            = "request_processor_stage";
  public static final String REQUEST_PROCESSOR_DURING_SYNC_STAGE                            = "request_processor_during_sync_stage";
  
  public static final String ACTIVE_TO_PASSIVE_DRIVER_STAGE                       = "active_to_passive_driver_stage";
  public static final String ACTIVE_TO_PASSIVE_DRIVER_FLUSH_STAGE                       = "active_to_passive_driver_flush_stage";
  public static final String PASSIVE_TO_ACTIVE_DRIVER_STAGE                       = "passive_to_active_driver_stage";
  public static final String PASSIVE_REPLICATION_STAGE                            = "passive_replication_stage";
  public static final String PASSIVE_OUTGOING_RESPONSE_STAGE                            = "passive_outgoing_response_stage";
  public static final String PASSIVE_RELAY_STAGE                            = "passive_relay_stage";
  public static final String PASSIVE_DUPLICATE_STAGE                            = "passive_duplicate_stage";
  public static final String PASSIVE_REPLICATION_ACK_STAGE                            = "passive_replication_ack_stage";

  public static final String PLATFORM_INFORMATION_REQUEST                       = "platform_information_request";

  public L2Coordinator getL2Coordinator();

  public ServerClientHandshakeManager getClientHandshakeManager();

  public ChannelStats getChannelStats();

  public void addShutdownItem(Runnable c);

  void shutdown();
}
