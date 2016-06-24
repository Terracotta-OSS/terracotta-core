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
    "l2.lockmanager.greedy.locks.enabled",
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
  public static final String L2_SEDA_STAGE_SINK_CAPACITY                                    = "l2.seda.stage.sink.capacity";
  String L2_TCCOM_WORKERTHREADS                                                          = "l2.tccom.workerthreads";
  String L2_SEDA_STAGE_WORKERTHREADS                                                     = "l2.seda.stage.workerthreads";

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
   * tcgroupcomm.reconnect.timeout        - L2-L2 reconnect windows in milliseconds
   * tcgroupcomm.reconnect.sendqueue.cap  - Sendqueue capacity, 0 for Integer.MAX_VALUE
   * tcgroupcomm.reconnect.maxDelayedAcks - At least one ack per maxDelayedAcks messages received
   * tcgroupcomm.reconnect.sendWindow     - Max outstanding messages before ack received
   * tcgroupcomm.reconnect.enabled        - Enable L2-L2 reconnect
   * send.timeout.millis                  - Number of milliseconds to retry sending a message
   * dirtydb.backup.enabled               - Creates BackUp of DirtyDB only If it is set to true.
   * </code>
   ********************************************************************************************************************/
  public static final String L2_NHA_TCGROUPCOMM_HANDSHAKE_TIMEOUT                           = "l2.nha.tcgroupcomm.handshake.timeout";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED                           = "l2.nha.tcgroupcomm.reconnect.enabled";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT                           = "l2.nha.tcgroupcomm.reconnect.timeout";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP                     = "l2.nha.tcgroupcomm.reconnect.sendqueue.cap";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_MAX_DELAYEDACKS                   = "l2.nha.tcgroupcomm.reconnect.maxDelayedAcks";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_SEND_WINDOW                       = "l2.nha.tcgroupcomm.reconnect.sendWindow";
  public static final String L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL                          = "l2.nha.tcgroupcomm.discovery.interval";
  // a hidden tc.properties only used for l2 proxy testing purpose
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT                   = "l2.nha.tcgroupcomm.l2proxytoport";
  public static final String L2_NHA_DIRTYDB_AUTODELETE                                      = "l2.nha.dirtydb.autoDelete";
  public static final String L2_NHA_DIRTYDB_ROLLING                                         = "l2.nha.dirtydb.rolling";
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
  public static final String L2_STARTUPLOCK_RETRIES_ENABLED                                 = "l2.startuplock.retries.enabled";
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
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED                 = "l1.transactionmanager.strings.compress.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED         = "l1.transactionmanager.strings.compress.logging.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE                 = "l1.transactionmanager.strings.compress.minSize";
  
  public static final String CLIENT_MAX_PENDING_REQUESTS                                    = "client.requests.pending.max";
  public static final String CLIENT_MAX_SENT_REQUESTS                                       = "client.requests.sent.max";

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
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SOCKET_CONNECT_TIMEOUT                                      = "l1.socket.connect.timeout";
  public static final String L1_SOCKET_RECONNECT_WAIT_INTERVAL                              = "l1.socket.reconnect.waitInterval";
  public static final String L1_CLUSTEREVENTS_OOB_JOINTIME_MILLIS                           = "l1.clusterevents.outofbandnotifier.jointime.millis";
  public static final String L1_CLUSTEREVENT_EXECUTOR_MAX_THREADS                           = "l1.clusterevent.executor.maxThreads";
  public static final String L1_CLUSTEREVENT_EXECUTOR_MAX_WAIT_SECONDS                      = "l1.clusterevent.executor.maxWaitSeconds";

  /*********************************************************************************************************************
   * <code>
   * Section: L1 Reconnect Properties
   * Description: This section contains properties controlling L1 reconnect feature Note that l1 get these properties from l2, so the local copy of l1 doesn't matter
   * enabled        - If true, enables l1 reconnect feature (and Once-And-Only-Once protocol)
   * timeout.millis - Number of milliseconds a disconnected L1 is allowed to
   * sendqueue.cap  - Sendqueue capacity, 0 for Integer.MAX_VALUE
   *                  reconnect to L2 that has not crashed
   * maxDelayedAcks - Max number of messages received for which ack may not be sent
   * sendWindow     - Max number of messages that can be sent without getting an ack back
   * rejoin.sleep.millis - Number of milliseconds to sleep before retry rejoin, if rejoin attempt was unsuccessful for some reason
   * </code>
   ********************************************************************************************************************/
  public static final String L2_L1RECONNECT_ENABLED                                         = "l2.l1reconnect.enabled";
  public static final String L2_L1RECONNECT_TIMEOUT_MILLS                                   = "l2.l1reconnect.timeout.millis";
  public static final String L2_L1RECONNECT_SENDQUEUE_CAP                                   = "l2.l1reconnect.sendqueue.cap";
  public static final String L2_L1RECONNECT_MAX_DELAYEDACKS                                 = "l2.l1reconnect.maxDelayedAcks";
  public static final String L2_L1RECONNECT_SEND_WINDOW                                     = "l2.l1reconnect.sendWindow";

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
   * stage.monitor.enabled                : <true/false>    - Enable or Disable Monitoring
   * stage.monitor.delay                  : long            - frequency in milliseconds
   * bytebuffer.pooling.enabled           : Enable/disable tc byte buffer pooling
   * bytebuffer.common.pool.maxcount      : Max size of pool for tc byte buffer
   * bytebuffer.threadlocal.pool.maxcount : Thread pool size
   * </code>
   ********************************************************************************************************************/
  public static final String TC_STAGE_MONITOR_ENABLED                                       = "tc.stage.monitor.enabled";
  public static final String TC_STAGE_MONITOR_DELAY                                         = "tc.stage.monitor.delay";
  public static final String TC_BYTEBUFFER_POOLING_ENABLED                                  = "tc.bytebuffer.pooling.enabled";
  public static final String TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT                             = "tc.bytebuffer.common.pool.maxcount";
  public static final String TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT                        = "tc.bytebuffer.threadlocal.pool.maxcount";
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
   * Section : L1 Lock Manager Properties
   * Description       : This section contains the defaults for the client lock manager for the L1
   * striped.count     : striping count for l1 lock manager
   * timeout.interval  : time after which an unused lock will be a candidate for lock GC
   * </code>
   ********************************************************************************************************************/
  public static final String L1_LOCKMANAGER_STRIPED_COUNT                                   = "l1.lockmanager.striped.count";
  public static final String L1_LOCKMANAGER_TIMEOUT_INTERVAL                                = "l1.lockmanager.timeout.interval";
  public static final String L1_LOCKMANAGER_PINNING_ENABLED                                 = "l1.lockmanager.pinning.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : Lock statistics
   * lock.statistics.enabled            : Enables/disables lock statistics
   * l1.lock.statistics.traceDepth      : Depth of locks given to L1s for gathering the statistics
   * l1.lock.statistics.gatherInterval  : Poll interval for gathering lock statistics
   * </code>
   ********************************************************************************************************************/
  public static final String LOCK_STATISTICS_ENABLED                                        = "lock.statistics.enabled";
  public static final String L1_LOCK_STATISTICS_TRACEDEPTH                                  = "l1.lock.statistics.traceDepth";
  public static final String L1_LOCK_STATISTICS_GATHERINTERVAL                              = "l1.lock.statistics.gatherInterval";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Lock Manager
   * enabled            : Enable/disable greedy locks grant from L2
   * leaseTimeInMillis  : Time for which greedy locks are given to L1 if more than one of them
   *                      are contending for them
   * </code>
   ********************************************************************************************************************/
  public static final String L2_LOCKMANAGER_GREEDY_LEASE_ENABLED                            = "l2.lockmanager.greedy.lease.enabled";
  public static final String L2_LOCKMANAGER_GREEDY_LEASE_LEASETIME_INMILLS                  = "l2.lockmanager.greedy.lease.leaseTimeInMillis";

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
  public static final String L2_HEALTHCHECK_L1_PING_ENABLED                                 = "l2.healthcheck.l1.ping.enabled";
  public static final String L2_HEALTHCHECK_L1_PING_IDLETIME                                = "l2.healthcheck.l1.ping.idletime";
  public static final String L2_HEALTHCHECK_L1_PING_INTERVAL                                = "l2.healthcheck.l1.ping.interval";
  public static final String L2_HEALTHCHECK_L1_PING_PROBES                                  = "l2.healthcheck.l1.ping.probes";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT                              = "l2.healthcheck.l1.socketConnect";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT_TIMEOUT                      = "l2.healthcheck.l1.socketConnectTimeout";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT_COUNT                        = "l2.healthcheck.l1.socketConnectCount";
  public static final String L2_HEALTHCHECK_L1_CHECK_TIME_ENABLED                           = "l2.healthcheck.l1.checkTime.enabled";
  public static final String L2_HEALTHCHECK_L1_CHECK_TIME_INTERVAL                          = "l2.healthcheck.l1.checkTime.interval";
  public static final String L2_HEALTHCHECK_L1_CHECK_TIME_THRESHOLD                         = "l2.healthcheck.l1.checkTime.threshold";

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

  public static final String L1_HEALTHCHECK_L2_BIND_ADDRESS                                 = "l1.healthcheck.l2.bindAddress";
  public static final String L1_HEALTHCHECK_L2_BIND_PORT                                    = "l1.healthcheck.l2.bindPort";
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
   * Section :  EnterpriseLicenseResovler
   * license.path                                - path to license key
   * </code>
   ********************************************************************************************************************/
  public static final String PRODUCTKEY_RESOURCE_PATH                                       = "productkey.resource.path";
  public static final String PRODUCTKEY_PATH                                                = "productkey.path";
  public static final String LICENSE_PATH                                                   = "license.path";

  /*********************************************************************************************************************
   * <code>
   * l2.dump.on.exception.timeout - After get an uncaught exception, the server takes a dump. If the dump doesn't
   * happen within this timeout the server will exit (in seconds).
   * </code>
   ********************************************************************************************************************/
  public static final String L2_DUMP_ON_EXCEPTION_TIMEOUT                                   = "l2.dump.on.exception.timeout";

  /*********************************************************************************************************************
   * <code>
   * Dev console Settings
   *  l2.operator.events.store      -   Number of operator events L2s will store to keep the history of the events
   *  tc.time.sync.threshold        -   Number of second of tolerable system time difference between
   *                                    two nodes of cluster beyond which and operator event will be thrown
   *  l2.logs.store                 -   Number of logs L2s will store to keep the history of the logs
   * </code>
   ********************************************************************************************************************/
  public static final String L2_OPERATOR_EVENTS_STORE                                       = "l2.operator.events.store";
  public static final String TC_TIME_SYNC_THRESHOLD                                         = "tc.time.sync.threshold";
  public static final String L2_LOGS_STORE                                                  = "l2.logs.store";

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
   * Section :  Server Event settings
   * </code>
   ********************************************************************************************************************/
  String                     L2_SERVER_EVENT_BATCHER_INTERVAL_MS                            = "l2.serverEvent.batcher.intervalInMillis";
  String                     L2_SERVER_EVENT_BATCHER_QUEUE_SIZE                             = "l2.serverEvent.batcher.queueSize";
  String                     L1_SERVER_EVENT_DELIVERY_THREADS                               = "l1.serverEvent.delivery.threads";
  String                     L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE                            = "l1.serverEvent.delivery.queueSize";
  String                     L1_SERVER_EVENT_DELIVERY_TIMEOUT_INTERVAL                      = "l1.serverEvent.delivery.timeout.intervalInSec";

  /*********************************************************************************************************************
   * <code>
   * Section :  Version Settings
   * version.compatibility.check - check version compatibility for client<->server and server<-> connections
   * </code>
   ********************************************************************************************************************/
  public static final String VERSION_COMPATIBILITY_CHECK                                    = "version.compatibility.check";

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
  public static final String  L1_LOCK_MANAGER_CATEGORY                                      = "l1.lockmanager";
  public static final String  LOGGING_CATEGORY                                              = "logging";
  public static final String  NETCORE_CATEGORY                                              = "net.core";

}
