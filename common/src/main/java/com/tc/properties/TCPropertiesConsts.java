/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.properties;

public interface TCPropertiesConsts {

  /*********************************************************************************************************************
   * <code>
   * Section : OLD TC Properties
   * The old properties which were present earlier and now got removed
   * If any of the property is renamed/deleted then make sure that u add that in this section
   * </code>
   ********************************************************************************************************************/

  static final String[]      OLD_PROPERTIES                                                 = {
  };

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Seda stage properties
   * Description : This section contains configuration for SEDA stages for L2
   * apply.stage.threads                : Number of threads for the transaction apply stage
   * faultstage.threads                 : Number of seda fault stage thread
   * managedobjectrequeststage.threads  : Number of threads for object request seda stage
   *                                      (experimental, do not change)
   * managedobjectresponsestage.threads : Number of threads for object response seda stage
   * flushstage.threads                 : Number of threads for flusing of objects to disk
   *                                      seda stage
   * stage.sink.capacity                : Capacity of seda stage queue, Integer.MAX_VALUE if not set
   *                                      (experimental, do not change)
   * </code>
   ********************************************************************************************************************/
  public static final String ENTITY_PROCESSOR_THREADS                                    = "server.entity.processor.threads";
  public static final String MIN_ENTITY_PROCESSOR_THREADS                                    = "server.entity.processor.minthreads";
  public static final String L2_SEDA_STAGE_SINK_CAPACITY                                    = "l2.seda.stage.sink.capacity";
  public static final String L2_SEDA_STAGE_DISABLE_DIRECT_SINKS                                    = "l2.seda.stage.sink.disable.direct";
  public static final String L2_SEDA_STAGE_SINGLE_THREAD                                    = "l2.seda.stage.single.thread";
  public static final String L2_SEDA_STAGE_USE_BACKOFF                                    = "l2.seda.stage.voltron.backoff";
  public static final String L2_SEDA_STAGE_STALL_WARNING                                    = "l2.seda.stage.stall.warning";
  public static final String L2_SEDA_STAGE_ALWAYS_HYDRATE                                    = "l2.seda.stage.always.hydrate";
  public static final String L2_TCCOM_WORKERTHREADS                                                          = "l2.tccom.workerthreads";
  public static final String L2_SEDA_STAGE_WORKERTHREADS                                                     = "l2.seda.stage.workerthreads";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Seda stage properties
   * Description : This section contains configuration for SEDA stages for L1
   * stage.sink.capacity  : Capacity of L1's seda stage queue, Integer.MAX_VALUE if not set
   * pinned.entry.fault.stage.threads : Number of threads for pinned entry fault stage
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SEDA_STAGE_SINK_CAPACITY                                    = "l1.seda.stage.sink.capacity";

  /*********************************************************************************************************************
   * <code>
   * Section : Network HA (nha)
   * Description : If Networked HA is enabled then these values take effect
   * tcgroupcomm.handshake.timeout        - tc-group-comm handshake timeout milliseconds
   * tcgroupcomm.response.timelimit       - tc-group-comm message response timelimit millisecon RuntimeException
   *                                        thrown after timelimit
   * tcgroupcomm.discovery.interval       - tc-group-comm member discovery interval milliseconds
   * send.timeout.millis                  - Number of milliseconds to retry sending a message
   * dirtydb.backup.enabled               - Creates BackUp of DirtyDB only If it is set to true.
   * </code>
   ********************************************************************************************************************/
  public static final String L2_NHA_TCGROUPCOMM_HANDSHAKE_TIMEOUT                           = "l2.nha.tcgroupcomm.handshake.timeout";
  public static final String L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL                          = "l2.nha.tcgroupcomm.discovery.interval";
  // a hidden tc.properties only used for l2 proxy testing purpose
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT                   = "l2.nha.tcgroupcomm.l2proxytoport";
  public static final String L2_NHA_DIRTYDB_AUTODELETE                                      = "l2.nha.dirtydb.autoDelete";
  public static final String L2_NHA_AUTORESTART                                             = "l2.nha.autoRestart";

  /*********************************************************************************************************************
   * <code>
   * Section : Misc L2 Properties
   * Description : Other Miscellaneous L2 Properties
   * enable.legacy.production.mode : If true then L2 will require -force to shutdown an active
   *                                 instance in a cluster with no passives present
   * startuplock.retries.enabled   : If true then L2s will try to lock indefinitely on the data
   *                                 directory while starting up
   * </code>
   ********************************************************************************************************************/
  public static final String PLUGIN_CLASSLOADER_COMPATIBILITY                               = "server.classloader.compatibility";
  public static final String ENTITY_DEFERMENT_QUEUE_SIZE                                    = "server.entity.deferment.queue.size";
  
  /*********************************************************************************************************************
   * <code>
   * Section : L1 Transaction Manager Properties
   * Description : This section contains the defaults for the Transaction manager for the L1
   *    logging.enabled            - If true, enables some logging in the transaction manager
   *    strings.compress.enabled   - Enables string compression when sending to the L2. There
   *                                 is a processing overhead at the L1, but saves network
   *                                 bandwidth, reduces memory requirements in the L2 and also
   *                                 reduces disk io at the L2.
   *    strings.compress.minSize   - Strings with lengths less that this number are not
   *                                 compressed
   *    timeoutForAckOnExit        - Max wait time in seconds to wait for ACKs before exit.
   *                                 value 0 for infinite wait.
   * </code>
   ********************************************************************************************************************/  

  public static final String TC_TRANSPORT_HANDSHAKE_TIMEOUT                                 = "tc.transport.handshake.timeout";
  public static final String TC_CONFIG_SOURCEGET_TIMEOUT                                    = "tc.config.getFromSource.timeout";
  public static final String TC_CONFIG_TOTAL_TIMEOUT                                        = "tc.config.total.timeout";

  /*********************************************************************************************************************
   * <code>
   * Section: L1 Connect Properties
   * Description: This section contains properties controlling L1 connect feature
   * socket.connect.timeout            - Socket timeout (ms) when connecting to server
   * reconnect.waitInterval            - Sleep time (ms) between trying connections to the server
   *                                     (values less than 10ms will be set to 10ms)
   * l2.l1redirect.enabled             - Allow the server to redirect the client to the current active
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SOCKET_CONNECT_TIMEOUT                                      = "l1.socket.connect.timeout";
  public static final String L1_SOCKET_RECONNECT_WAIT_INTERVAL                              = "l1.socket.reconnect.waitInterval";
  public static final String L2_L1REDIRECT_ENABLED                                          = "l2.l1redirect.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Logging properties for both L1 and L2
   * Description : Logging attributes that can be overridden.
   * maxBackups       - The maximum number of backup log files to keep maxLogFileSize - The maximum size of a log file in megabytes
   * </code>
   ********************************************************************************************************************/
  public static final String LOGGING_MAXBACKUPS                                             = "logging.maxBackups";
  public static final String LOGGING_MAX_LOGFILE_SIZE                                       = "logging.maxLogFileSize";
  public static final String LOGGING_LONG_GC_THRESHOLD                                      = "logging.longgc.threshold";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Stage Monitoring properties for both L1 and L2
   * Description : Stage monitoring can be enabled or disabled for debugging.
   * gc.monitor.enabled                   : <true/false>    - Enable or Disable GC Monitoring
   * gc.monitor.delay                     : long            - frequency in milliseconds
   * stage.monitor.enabled                : <true/false>    - Enable or Disable stage Monitoring
   * stage.monitor.delay                  : long            - frequency in milliseconds
   * bytebuffer.pooling.enabled           : Enable/disable tc byte buffer pooling
   * bytebuffer.common.pool.maxcount      : Max size of pool for tc byte buffer
   * bytebuffer.threadlocal.pool.maxcount : Thread pool size
   * </code>
   ********************************************************************************************************************/
  
  public static final String TC_GC_MONITOR_ENABLED                                          = "tc.gc.monitor.enabled";
  public static final String TC_GC_MONITOR_DELAY                                            = "tc.gc.monitor.delay";
  public static final String TC_STAGE_MONITOR_ENABLED                                       = "tc.stage.monitor.enabled";
  public static final String TC_STAGE_MONITOR_DELAY                                         = "tc.stage.monitor.delay";
  public static final String TC_MESSAGE_GROUPING_ENABLED                                    = "tc.messages.grouping.enabled";
  public static final String TC_MESSAGE_GROUPING_MAXSIZE_KB                                 = "tc.messages.grouping.maxSizeKiloBytes";
  public static final String TC_MESSAGE_PACKUP_ENABLED                                      = "tc.messages.packup.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : Common property for TC Management MBean
   * Description : TC Management MBeans can be enabled/disabled
   * mbeans.enabled : <true/false> - All mbeans enabled/disabled test.mbeans.enabled : <true/false> - Test mode mbeans
   * enabled/disabled
   * </code>
   ********************************************************************************************************************/
  public static final String TC_MANAGEMENT_MBEANS_ENABLED                                   = "tc.management.mbeans.enabled";
  public static final String TC_MANAGEMENT_TEST_MBEANS_ENABLED                              = "tc.management.test.mbeans.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : Memory Monitor
   * forcebasic : enable/disable only basic memory monitoring
   * </code>
   ********************************************************************************************************************/
  public static final String MEMORY_MONITOR_FORCEBASIC                                      = "memory.monitor.forcebasic";

  /*********************************************************************************************************************
   * <code>
   * Section : TCP Settings
   * tcpnodelay : Enable/disable tcp packet batching
   * keepalive  : Enable/disable tcp probe for running/broken connections
   * </code>
   ********************************************************************************************************************/
  public static final String NET_CORE_KEEPALIVE                                             = "net.core.keepalive";
  public static final String NET_CORE_TCP_NO_DELAY                                          = "net.core.tcpnodelay";

  /*********************************************************************************************************************
   * <code>
   *  Section : HealthChecker { server->client, server->server (HA), client->server }
   *  ping.enabled         - If true, healthchecker is enabled.
   *  ping.idletime        - Connection idletime (in milliseconds), after which healthchecker
   *                         starts its ping test.
   *  ping.interval        - The interval (in milliseconds) between healthchecker sending ping
   *                         messages to the peer node which doesn't reply to its previous msgs.
   *  ping.probes          - Total number of ping messages to be sent to the peer node before
   *                         concluding the peer is dead.
   *  socketConnect        - If true, apart from above ping-probe cycle, healthcheker does extra
   *                         check like socket connect (to detect long GC) to see if the peer has
   *                         any traces of life left
   *  socketConnectCount   - Max number of successful socket connect that healthcheker
   *                         can trust. Beyond which, no socket connects will be
   *                         attempted and peer node is tagged as dead.
   *  socketConnectTimeout - Socket timeout (integer, in number of ping.interval) when
   *                         connecting to the peer node. On timeout, healthchecker
   *                         concludes peer node as dead irrespective of previous
   *                         successful socket connects
   * </code>
   ********************************************************************************************************************/

  public static final String L2_HEALTHCHECK_L2_PING_ENABLED                                 = "l2.healthcheck.l2.ping.enabled";
  public static final String L2_HEALTHCHECK_L2_PING_IDLETIME                                = "l2.healthcheck.l2.ping.idletime";
  public static final String L2_HEALTHCHECK_L2_PING_INTERVAL                                = "l2.healthcheck.l2.ping.interval";
  public static final String L2_HEALTHCHECK_L2_PING_PROBES                                  = "l2.healthcheck.l2.ping.probes";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT                              = "l2.healthcheck.l2.socketConnect";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT                      = "l2.healthcheck.l2.socketConnectTimeout";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                        = "l2.healthcheck.l2.socketConnectCount";
  public static final String L2_HEALTHCHECK_L2_CHECK_TIME_ENABLED                           = "l2.healthcheck.l2.checkTime.enabled";
  public static final String L2_HEALTHCHECK_L2_CHECK_TIME_INTERVAL                          = "l2.healthcheck.l2.checkTime.interval";
  public static final String L2_HEALTHCHECK_L2_CHECK_TIME_THRESHOLD                         = "l2.healthcheck.l2.checkTime.threshold";

  public static final String L1_HEALTHCHECK_L2_PING_ENABLED                                 = "l1.healthcheck.l2.ping.enabled";
  public static final String L1_HEALTHCHECK_L2_PING_IDLETIME                                = "l1.healthcheck.l2.ping.idletime";
  public static final String L1_HEALTHCHECK_L2_PING_INTERVAL                                = "l1.healthcheck.l2.ping.interval";
  public static final String L1_HEALTHCHECK_L2_PING_PROBES                                  = "l1.healthcheck.l2.ping.probes";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT                              = "l1.healthcheck.l2.socketConnect";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT                      = "l1.healthcheck.l2.socketConnectTimeout";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                        = "l1.healthcheck.l2.socketConnectCount";
  public static final String L1_HEALTHCHECK_L2_CHECK_TIME_ENABLED                           = "l1.healthcheck.l2.checkTime.enabled";
  public static final String L1_HEALTHCHECK_L2_CHECK_TIME_INTERVAL                          = "l1.healthcheck.l2.checkTime.interval";
  public static final String L1_HEALTHCHECK_L2_CHECK_TIME_THRESHOLD                         = "l1.healthcheck.l2.checkTime.threshold";

  /*********************************************************************************************************************
   * <code>
   * Section : TCMessage debug monitoring
   * tcm.monitor.enabled - If enabled the count and size of TC messages will be collected and logged
   * tcm.monitor.delay   - The delay (in seconds) between reporting to the log
   * </code>
   ********************************************************************************************************************/
  public static final String TCM_MONITOR_ENABLED                                            = "tcm.monitor.enabled";
  public static final String TCM_MONITOR_DELAY                                              = "tcm.monitor.delay";

  /*********************************************************************************************************************
   * <code>
   * Section :  Stats Printer
   * stats.printer.intervalInMillis              - Interval at which gathered stats are printed
   * </code>
   ********************************************************************************************************************/
  public static final String STATS_PRINTER_INTERVAL                                         = "stats.printer.intervalInMillis";


  /*********************************************************************************************************************
   * <code>
   * l2.dump.on.exception.timeout - After get an uncaught exception, the server takes a dump. If the dump doesn't
   * happen within this timeout the server will exit (in seconds).
   * </code>
   ********************************************************************************************************************/
  public static final String L2_DUMP_ON_EXCEPTION_TIMEOUT                                   = "l2.dump.on.exception.timeout";
  public static final String L2_LOGS_STORE                                                  = "l2.logs.store";
  public static final String L2_ELECTION_TIMEOUT                                            = "l2.election.timeout";
  public static final String L2_CLASSLOADER_COMPATIBILITY                                   = "l2.classloader.compatibility";

  /*********************************************************************************************************************
   * <code>
   * Section :  L1 Shutdown Settings
   * l1.shutdown.threadgroup.gracetime - time allowed for termination of all threads in the TC thread group (in milliseconds).
   * l1.shutdown.force.finalization    - call System.runFinalization() at the end of the L1 shutdown procedure.
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SHUTDOWN_THREADGROUP_GRACETIME                              = "l1.shutdown.threadgroup.gracetime";
  public static final String L1_SHUTDOWN_FORCE_FINALIZATION                                 = "l1.shutdown.force.finalization";

  /*********************************************************************************************************************
   * <code>
   * Section :  Some useful subcategories
   * </code>
   ********************************************************************************************************************/

  public static final String  L1_CATEGORY                                                   = "L1";
  public static final String  L2_CATEGORY                                                   = "L2";
  public static final String  L1_L2_HEALTH_CHECK_CATEGORY                                   = "l1.healthcheck.l2";
  public static final String  L2_L1_HEALTH_CHECK_CATEGORY                                   = "l2.healthcheck.l1";
  public static final String  L2_L2_HEALTH_CHECK_CATEGORY                                   = "l2.healthcheck.l2";
  public static final String  LOGGING_CATEGORY                                              = "logging";
  public static final String  NETCORE_CATEGORY                                              = "net.core";

  String[] TC_PROPERTIES_WITH_NO_DEFAULTS = {
      ENTITY_PROCESSOR_THREADS,
      L2_TCCOM_WORKERTHREADS,
      L2_SEDA_STAGE_WORKERTHREADS,
      L2_SEDA_STAGE_DISABLE_DIRECT_SINKS,
      L2_SEDA_STAGE_USE_BACKOFF,
      L2_SEDA_STAGE_SINGLE_THREAD,
      L2_SEDA_STAGE_STALL_WARNING,
      L2_SEDA_STAGE_ALWAYS_HYDRATE,
      L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT,
  };

}
