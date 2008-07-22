/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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

  public static final String[] OLD_PROPERTIES                                         = { "l1.reconnect.enabled",
      "l1.reconnect.timeout.millis", "l2.nha.ooo.maxDelayedAcks", "l2.nha.ooo.sendWindow",
      "l2.objectmanager.loadObjectID.checkpoint.changes", "l2.objectmanager.loadObjectID.checkpoint.timeperiod" };

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Cache Manager Properties 
   * Description : This section contains the defaults for the cache manager for the L2 
   * TODO : Explain all these parameters
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_CACHEMANAGER_ENABLED                                = "l2.cachemanager.enabled";
  public static final String   L2_CACHEMANAGER_LOGGING_ENABLED                        = "l2.cachemanager.logging.enabled";
  public static final String   L2_CACHEMANAGER_LEASTCOUNT                             = "l2.cachemanager.leastCount";
  public static final String   L2_CACHEMANAGER_PERCENTAGETOEVICT                      = "l2.cachemanager.percentageToEvict";
  public static final String   L2_CACHEMANAGER_SLEEPINTERVAL                          = "l2.cachemanager.sleepInterval";
  public static final String   L2_CACHEMANAGER_CRITICALTHRESHOLD                      = "l2.cachemanager.criticalThreshold";
  public static final String   L2_CACHEMANAGER_THRESHOLD                              = "l2.cachemanager.threshold";
  public static final String   L2_CACHEMANAGER_MONITOROLDGENONLY                      = "l2.cachemanager.monitorOldGenOnly";
  public static final String   L2_CACHEMANAGER_CRITICALOBJECTTHRESHOLD                = "l2.cachemanager.criticalObjectThreshold";

  /*********************************************************************************************************************
   * Section : L2 Transaction Manager Properties
   ********************************************************************************************************************/
  public static final String   L2_TRANSACTIONMANAGER_LOGGING_ENABLED                  = "l2.transactionmanager.logging.enabled";
  public static final String   L2_TRANSACTIONMANAGER_LOGGING_VERBOSE                  = "l2.transactionmanager.logging.verbose";
  public static final String   L2_TRANSACTIONMANAGER_LOGGING_PRINTSTATS               = "l2.transactionmanager.logging.printStats";
  public static final String   L2_TRANSACTIONMANAGER_LOGGING_PRINTCOMMITS             = "l2.transactionmanager.logging.printCommits";
  public static final String   L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_ENABLED         = "l2.transactionmanager.passive.throttle.enabled";
  public static final String   L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_THRESHOLD       = "l2.transactionmanager.passive.throttle.threshold";
  public static final String   L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_MAXSLEEPSECONDS = "l2.transactionmanager.passive.throttle.maxSleepSeconds";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Object Manager Properties Description : 
   * This section contains the defaults for the object manager of the L2 
   * cachePolicy : <lru>/<lfu>  - Least Recently Used or Least Frequently used 
   * deleteBatchSize            - Max number of objects deleted in one transaction when removing from the object store after a GC
   * maxObjectsToCommit         - Max number of Objects commited in one transaction in the commit stage and flush stage
   * maxObjectsInTxnObjGrouping - Max number of Objects allowed in the TransactionalObject grouping
   * maxTxnsInTxnObjectGrouping - Max number of Transations allowed in the TransactionalObject grouping
   * fault.logging.enabled      - Enables/Disables logging of ManagedObject Faults from disk. If enabled, it logs every 100 faults.
   * loadObjectID.fastLoad      - Enables/Disables fast loading of ObjectIDs. Only effective for persistence with mode permanent-store. 
   *                              This will speed up Object-Ids loading at restart but some overhead occurred at regular operations. 
   *                              You can go from enable to disable but need a fresh start if change from disable to enable for building 
   *                              up compressed object-Id. 
   * loadObjectID.longsPerDiskEntry - Size of long array entry to store object IDs in persistent store. One bit for each ID. 
   * loadObjectID.checkpoint.changes - number of changes to trigger objectID checkpoint 
   * loadObjectID.checkpoint.maxlimit - max number of changes to process in one run checkpoint. 
   * loadObjectID.checkpoint.timeperiod - time period in milliseconds between checkpoints
   * passive.sync.batch.size    - Number of objects in each message that is sent from active to passive while synching
   * passive.sync.throttle.timeInMillis - Time to wait before sending the next batch of objects to the passive
   * dgc.young.enabled          - Enables/Disables the young gen collector
   * dgc.young.frequencyInMillis - The time in millis between each young gen collection. (default : 1 min, not advisable to run more frequently)
   * </code>
   ********************************************************************************************************************/

  public static final String   L2_OBJECTMANAGER_DELETEBATCHSIZE                       = "l2.objectmanager.deleteBatchSize";
  public static final String   L2_OBJECTMANAGER_CACHEPOLICY                           = "l2.objectmanager.cachePolicy";
  public static final String   L2_OBJECTMANAGER_MAXOBJECTS_TO_COMMIT                  = "l2.objectmanager.maxObjectsToCommit";
  public static final String   L2_OBJECTMANAGER_MAXOBJECTS_INTXNOBJ_GROUPING          = "l2.objectmanager.maxObjectsInTxnObjGrouping";
  public static final String   L2_OBJECTMANAGER_MAXTXNS_INTXNOBJECT_GROUPING          = "l2.objectmanager.maxTxnsInTxnObjectGrouping";
  public static final String   L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED                 = "l2.objectmanager.fault.logging.enabled";
  public static final String   L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED             = "l2.objectmanager.persistor.logging.enabled";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_FASTLOAD                 = "l2.objectmanager.loadObjectID.fastLoad";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY       = "l2.objectmanager.loadObjectID.longsPerDiskEntry";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_CHANGES       = "l2.objectmanager.loadObjectID.checkpoint.changes";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT      = "l2.objectmanager.loadObjectID.checkpoint.maxlimit";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_TIMEPERIOD    = "l2.objectmanager.loadObjectID.checkpoint.timeperiod";
  public static final String   L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE               = "l2.objectmanager.passive.sync.batch.size";
  public static final String   L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_TIME            = "l2.objectmanager.passive.sync.throttle.timeInMillis";
  public static final String   L2_OBJECTMANAGER_DGC_YOUNG_ENABLED                     = "l2.objectmanager.dgc.young.enabled";
  public static final String   L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY                   = "l2.objectmanager.dgc.young.frequencyInMillis";
  public static final String   L2_DATA_BACKUP_THROTTLE_TIME                           = "l2.data.backup.throttle.timeInMillis";
  public static final String   L2_OBJECTMANAGER_PERSISTOR_MEASURE_PERF                = "l2.objectmanager.persistor.measure.performance";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_MAPDB_LONGS_PERDISKENTRY = "l2.objectmanager.loadObjectID.mapsdatabase.longsPerDiskEntry";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF             = "l2.objectmanager.loadObjectID.measure.performance";
  public static final String   L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP      = "l2.objectmanager.loadObjectID.checkpoint.maxsleep";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Seda stage properties 
   * Description : This section contains configuration for SEDA stages for L2
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_SEDA_COMMITSTAGE                                    = "l2.seda.commitstage.threads";
  public static final String   L2_SEDA_FAULTSTAGE_THREADS                             = "l2.seda.faultstage.threads";
  public static final String   L2_SEDA_FLUSHSTAGE_THREAD                              = "l2.seda.flushstage.threads";
  public static final String   L2_SEDA_STAGE_SINK_CAPACITY                            = "l2.seda.stage.sink.capacity";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Seda stage properties 
   * Description : This section contains configuration for SEDA stages for L1
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_SEDA_STAGE_SINK_CAPACITY                            = "l1.seda.stage.sink.capacity";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Berkeley DB Persistence Layer Properties 
   * Description : This section contains the of Berkeley DB JE properties thats used in L2 
   * For an explanation of these properties look at Berkeley DB documentation 
   * (l2.berkeleydb is removed before giving to Berkeley DB JE) 
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_BERKELEYDB_JE_LOCK_TIMEOUT                          = "l2.berkeleydb.je.lock.timeout";
  public static final String   L2_BERKELEYDB_JE_MAXMEMORYPERCENT                      = "l2.berkeleydb.je.maxMemoryPercent";
  public static final String   L2_BERKELEYDB_JE_LOCK_NLOCK_TABLES                     = "l2.berkeleydb.je.lock.nLockTables";
  public static final String   L2_BERKELEYDB_JE_CLEANER_BYTES_INTERVAL                = "l2.berkeleydb.je.cleaner.bytesInterval";
  public static final String   L2_BERKELEYDB_JE_CHECKPOINTER_BYTESINTERVAL            = "l2.berkeleydb.je.checkpointer.bytesInterval";
  public static final String   L2_BERKELEYDB_JE_CLEANER_DETAIL_MAXMEMORY_PERCENTAGE   = "l2.berkeleydb.je.cleaner.detailMaxMemoryPercentage";
  public static final String   L2_BERKELEYDB_JE_CLEANER_LOOKAHEAD_CACHESIZE           = "l2.berkeleydb.je.cleaner.lookAheadCacheSize";
  public static final String   L2_BERKELEYDB_JE_CLEANER_MINAGE                        = "l2.berkeleydb.je.cleaner.minAge";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 LFU cachepolicy defaults 
   * Description : If cachePolicy is set to lfu, then these values take effect
   * agingFactor (float)                    - valid values 0 to 1 
   * recentlyAccessedIgnorePercentage (int) - valid values 0 - 100
   * debug.enabled                          - valid values true/false
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_LFU_AGINGFACTOR                                     = "l2.lfu.agingFactor";
  public static final String   L2_LFU_RECENTLY_ACCESSED_IGNORE_PERCENTAGE             = "l2.lfu.recentlyAccessedIgnorePercentage";
  public static final String   L2_LFU_DEBUG_ENABLED                                   = "l2.lfu.debug.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Bean shell Properties 
   * Description : Bean shell can be enabled in the server for debugging.
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_BEANSHELL_ENABLED                                   = "l2.beanshell.enabled";
  public static final String   L2_BEANSHELL_PORT                                      = "l2.beanshell.port";

  /*********************************************************************************************************************
   * <code>
   * Section : Network HA (nha) 
   * Description : If Networked HA is enabled then these values take effect 
   * groupcomm.type                   - communication layer can be "tc-group-comm" or "tribes" 
   * tcgroupcomm.handshake.timeout    - tc-group-comm handshake timeout milliseconds 
   * tcgroupcomm.response.timelimit   - tc-group-comm message response timelimit millisecon RuntimeException thrown after timelimit 
   * tcgroupcomm.discovery.interval   - tc-group-comm member discovery interval milliseconds 
   * tcgroupcomm.reconnect.timeout    - L2-L2 reconnect windows in milliseconds
   * tcgroupcomm.reconnect.sendqueue.cap - sendqueue capacity, 0 for Integer.MAX_VALUE
   * tcgroupcomm.reconnect.maxDelayedAcks - at least one ack per maxDelayedAcks messages received
   * tcgroupcomm.reconnect.sendWindow - max outstanding messages before ack received
   * send.timeout.millis              - number of milliseconds to retry sending a message 
   * tribes.failuredetector.millis    - number of milliseconds for a node to response a check otherwise will be removed from group. 
   * mcast.enabled                    - If true, uses Multicast instead of TCP for L2-L2 discovery 
   * l2.nha.tribes.mcast.*            - these properties are passed to tribes
   * tcgroupcomm.reconnect.enabled  -  enable L2-L2 reconnect
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_NHA_GROUPCOMM_TYPE                                  = "l2.nha.groupcomm.type";
  public static final String   L2_NHA_TCGROUPCOMM_HANDSHAKE_TIMEOUT                   = "l2.nha.tcgroupcomm.handshake.timeout";
  public static final String   L2_NHA_TCGROUPCOMM_RESPONSE_TIMELIMIT                  = "l2.nha.tcgroupcomm.response.timelimit";
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED                   = "l2.nha.tcgroupcomm.reconnect.enabled";
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT                   = "l2.nha.tcgroupcomm.reconnect.timeout";
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP             = "l2.nha.tcgroupcomm.reconnect.sendqueue.cap";
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_MAX_DELAYEDACKS           = "l2.nha.tcgroupcomm.reconnect.maxDelayedAcks";
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_SEND_WINDOW               = "l2.nha.tcgroupcomm.reconnect.sendWindow";
  public static final String   L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL                  = "l2.nha.tcgroupcomm.discovery.interval";
  // a hidden tc.properties only used for l2 proxy testing purpose
  public static final String   L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT           = "l2.nha.tcgroupcomm.l2proxytoport";
  public static final String   L2_NHA_SEND_TIMEOUT_MILLS                              = "l2.nha.send.timeout.millis";
  public static final String   L2_NHA_TRIBES_FAILURE_DETECTOR_MILLS                   = "l2.nha.tribes.failuredetector.millis";
  public static final String   L2_NHA_MCAST_ENABLED                                   = "l2.nha.mcast.enabled";
  public static final String   L2_NHA_TRIBES_ORDER_INTERCEPTOR_ENABLED                = "l2.nha.tribes.orderinterceptor.enabled";
  public static final String   L2_NHA_TRIBES_MCAST_MCASTPORT                          = "l2.nha.tribes.mcast.mcastPort";
  public static final String   L2_NHA_TRIBES_MCAST_MCASTADDRESS                       = "l2.nha.tribes.mcast.mcastAddress";
  public static final String   L2_NHA_TRIBES_MCAST_MEMBERDROPTIME                     = "l2.nha.tribes.mcast.memberDropTime";
  public static final String   L2_NHA_TRIBES_MCAST_MCASTFREQUENCY                     = "l2.nha.tribes.mcast.mcastFrequency";
  public static final String   L2_NHA_TRIBES_MCAST_TCPLISTENPORT                      = "l2.nha.tribes.mcast.tcpListenPort";
  public static final String   L2_NHA_TRIBES_MCAST_TCPLISTENHOST                      = "l2.nha.tribes.mcast.tcpListenHost";
  public static final String   L2_NHA_DIRTYDB_AUTODELETE                              = "l2.nha.dirtydb.autoDelete";
  public static final String   L2_NHA_AUTORESTART                                     = "l2.nha.autoRestart";

  /*********************************************************************************************************************
   * <code>
   * Section : Misc L2 Properties 
   * Description : Other Miscellaneous L2 Properties
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_STARTUPLOCK_RETRIES_ENABLED                         = "l2.startuplock.retries.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 JVM Compatibility Properties 
   * Description : This section contains the defaults for the JVM compatibility for the L1 
   * TODO : Explain all these parameters
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_JVM_CHECK_COMPATIBILITY                             = "l1.jvm.check.compatibility";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Integration Modules 
   * Description : This section contains the defaults for the L1 integration modules
   * repositories                 - comma-separated list of additional module repositories URL's; if the tc.install-root system property is set, a default 
   *                                repository of (tc.install-root)/modules will be injected default - comma-separated list of integration modules that are 
   *                                implicitly loaded by the L1 in the form specified by the Required-Bundles OSGI
   * manifest header additional   - list of additional integration modules to be started, in the form specified by the OSGI Required-Bundles manifest header 
   * tc-version-check             - off|warn|enforce|strict
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_MODULES_REPOSITORIES                                = "l1.modules.repositories";
  public static final String   L1_MODULES_DEFAULT                                     = "l1.modules.default";
  public static final String   L1_MODULES_ADDITIONAL                                  = "l1.modules.additional";
  public static final String   L1_MODULES_TC_VERSION_CHECK                            = "l1.modules.tc-version-check";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Cache Manager Properties 
   * Description : This section contains the defaults for the cache manager for the L1 
   * TODO : Explain all these parameters
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_CACHEMANAGER_ENABLED                                = "l1.cachemanager.enabled";
  public static final String   L1_CACHEMANAGER_LOGGING_ENABLED                        = "l1.cachemanager.logging.enabled";
  public static final String   L1_CACHEMANAGER_LEASTCOUNT                             = "l1.cachemanager.leastCount";
  public static final String   L1_CACHEMANAGER_PERCENTAGE_TOEVICT                     = "l1.cachemanager.percentageToEvict";
  public static final String   L1_CACHEMANAGER_SLEEPINTERVAL                          = "l1.cachemanager.sleepInterval";
  public static final String   L1_CACHEMANAGER_CRITICAL_THRESHOLD                     = "l1.cachemanager.criticalThreshold";
  public static final String   L1_CACHEMANAGER_THRESHOLD                              = "l1.cachemanager.threshold";
  public static final String   L1_CACHEMANAGER_MONITOR_OLDGENONLY                     = "l1.cachemanager.monitorOldGenOnly";
  public static final String   L1_CACHEMANAGER_CRITICAL_OBJECT_THRESHOLD              = "l1.cachemanager.criticalObjectThreshold";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Transaction Manager Properties 
   * Description : This section contains the defaults for the Transaction manager for the L1 
   * logging.enabled             - if true, enables some logging in the transaction manager
   * maxOutstandingBatchSize     - the max number of batches of transaction that each L1 sends to the L2 at once
   * maxBatchSizeInKiloBytes     - the max size of batches that are send to the L2 from the L1. The units is in Kilobytes
   * maxPendingBatches           - the max number of pending batches the client creates before a Batch ack is received from the server, after which the client stalls 
   *                               until a Batch ack is received. 
   * maxSleepTimeBeforeHalt      - the max time that a user thread will wait for L2 to catchup if the L2 is behind applying transactions. This time is used before 
   *                               maxPendingBatches is reached. The units are in milliseconds 
   * completedAckFlushTimeout    - the timeout in milliseconds after which a NullTransaction is send to the server if completed txn acks are still pending
   * strings.compress.enabled    - Enables string compression when sending to the L2. There is a processing overhead at the L1, but saves network bandwidth, 
   *                               reduces memory requirements in the L2 and also reduces disk io at the L2.
   * strings.compress.minSize    - Strings with lengths less that this number are not compressed 
   * folding.enabled             - true/false whether txn folding is enabled. Folding is the act of combining similar (but unique) application transactions into 
   *                               a single txn (for more optimal processing on the server). Only transactions that share common  locks and objects can be folded. 
   * folding.lock.limit          - the maximum number of distinct locks permitted in folded txns (0 or less means infinite) 
   * folding.object.limit        - the maximum number of distinct objects permitted in folded txns (0 or less means infinite)
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_TRANSACTIONMANAGER_LOGGING_ENABLED                  = "l1.transactionmanager.logging.enabled";
  public static final String   L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE         = "l1.transactionmanager.maxOutstandingBatchSize";
  public static final String   L1_TRANSACTIONMANAGER_MAXBATCHSIZE_INKILOBYTES         = "l1.transactionmanager.maxBatchSizeInKiloBytes";
  public static final String   L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES               = "l1.transactionmanager.maxPendingBatches";
  public static final String   L1_TRANSACTIONMANAGER_MAXSLEEPTIME_BEFOREHALT          = "l1.transactionmanager.maxSleepTimeBeforeHalt";
  public static final String   L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT      = "l1.transactionmanager.completedAckFlushTimeout";
  public static final String   L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED         = "l1.transactionmanager.strings.compress.enabled";
  public static final String   L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED = "l1.transactionmanager.strings.compress.logging.enabled";
  public static final String   L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE         = "l1.transactionmanager.strings.compress.minSize";
  public static final String   L1_TRANSACTIONMANAGER_FOLDING_ENABLED                  = "l1.transactionmanager.folding.enabled";
  public static final String   L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT             = "l1.transactionmanager.folding.object.limit";
  public static final String   L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT               = "l1.transactionmanager.folding.lock.limit";
  public static final String   L1_TRANSACTIONMANAGER_FOLDING_DEBUG                    = "l1.transactionmanager.folding.debug";

  /*********************************************************************************************************************
   * <code>
   * Section: L1 Connect Properties 
   * Description: This section contains properties controlling L1 connect feature
   * max.connect.retries - maximum L2 connection attempts 
   * connect.versionMatchCheck.enabled - if true, connection is established only when L1 and L2 are of the same DSO version
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_MAX_CONNECT_RETRIES                                 = "l1.max.connect.retries";
  public static final String   L1_CONNECT_VERSION_MATCH_CHECK                         = "l1.connect.versionMatchCheck.enabled";
  public static final String   L1_SOCKET_CONNECT_TIMEOUT                              = "l1.socket.connect.timeout";
  public static final String   L1_SOCKET_RECONNECT_WAIT_INTERVAL                      = "l1.socket.reconnect.waitInterval";

  /*********************************************************************************************************************
   * <code>
   * Section: L1 Reconnect Properties 
   * Description: This section contains properties controlling L1 reconnect feature Note that l1 get these properties from l2, so the local copy of l1 doesn't matter 
   * enabled - if true, enables l1 reconnect feature (and Once-And-Only-Once protocol) 
   * timeout.millis - number of milliseconds a disconnected L1 is allowed to reconnect to L2 that has not crashed
   * </code>
   ********************************************************************************************************************/
  public static final String   L2_L1RECONNECT_ENABLED                                 = "l2.l1reconnect.enabled";
  public static final String   L2_L1RECONNECT_TIMEOUT_MILLS                           = "l2.l1reconnect.timeout.millis";
  public static final String   L2_L1RECONNECT_SENDQUEUE_CAP                           = "l2.l1reconnect.sendqueue.cap";
  public static final String   L2_L1RECONNECT_MAX_DELAYEDACKS                         = "l2.l1reconnect.maxDelayedAcks";
  public static final String   L2_L1RECONNECT_SEND_WINDOW                             = "l2.l1reconnect.sendWindow";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Object Manager Properties 
   * Description : This section contains the defaults for the Object manager for the L1
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE                = "l1.objectmanager.remote.maxDNALRUSize";
  public static final String   L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED                = "l1.objectmanager.remote.logging.enabled";
  public static final String   L1_OBJECTMANAGER_OBJECTID_REQUEST_SIZE                 = "l1.objectmanager.objectid.request.size";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Logging properties for both L1 and L2 
   * Description : Logging attributes that can be overridden.
   * maxBackups - The maximum number of backup log files to keep maxLogFileSize - The maximum size of a log file in megabytes
   * </code>
   ********************************************************************************************************************/
  public static final String   L1_LOGGING_MAXBACKUPS                                  = "logging.maxBackups";
  public static final String   L1_LOGGING_MAX_LOGFILE_SIZE                            = "logging.maxLogFileSize";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Stage Monitoring properties for both L1 and L2 
   * Description : Stage monitoring can be enabled or disabled for debugging. 
   * enabled : <true/false> - Enable or Disable Monitoring 
   * delay : long - frequency in milliseconds
   * </code>
   ********************************************************************************************************************/
  public static final String   TC_STAGE_MONITOR_ENABLED                               = "tc.stage.monitor.enabled";
  public static final String   TC_STAGE_MONITOR_DELAY                                 = "tc.stage.monitor.delay";
  public static final String   TC_BYTEBUFFER_POOLING_ENABLED                          = "tc.bytebuffer.pooling.enabled";
  public static final String   TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT                     = "tc.bytebuffer.common.pool.maxcount";
  public static final String   TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT                = "tc.bytebuffer.threadlocal.pool.maxcount";

  /*********************************************************************************************************************
   * <code>
   * Section : Common property for TC Management MBean 
   * Description : TC Management MBeans can be enabled/disabled
   * mbeans.enabled : <true/false> - All mbeans enabled/disabled test.mbeans.enabled : <true/false> - Test mode mbeans
   * enabled/disabled
   * </code>
   ********************************************************************************************************************/
  public static final String   TC_MANAGEMENT_MBEANS_ENABLED                           = "tc.management.mbeans.enabled";
  public static final String   TC_MANAGEMENT_TEST_MBEANS_ENABLED                      = "tc.management.test.mbeans.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section                         : Session properties (applies to all DSO session enabled web apps in this VM) 
   * id.length                       : The length (in chars) for session identifiers (min 8) 
   * serverid                        : The server identifier to place in the session ID 
   * delimiter                       : Thedelimiter that separates the server ID from the session ID 
   * cookie.domain                   : Domain value for session cookie
   * cookie.secure                   : Enable / disable the secure flag in the session cookie 
   * cookie.maxage.seconds           : The maximum lifetime of the session cookie 
   * cookie.name                     : Name of the session cookie cookie.enabled : Enable / disable the use of cookies for session tracking 
   * maxidle.seconds                 : Session idle timeout in seconds 
   * tracking.enabled                : Enable / disable session tracking completely 
   * urlrewrite.enabled              : Enable / disable the URL functionality 
   * attribute.listeners             : Comma separated list of HttpSessionAttributeListener classes 
   * listeners                       : Comma separated list of HttpSessionListener
   * classes invalidator.sleep       : Sleep time between runs of the session invalidator 
   * request.bench.enabled           : Enable / disable request benchmark logging 
   * invalidator.bench.enabled       : Enable / disable benchmark logging for session
   * invalidation request.tracking   : Enable / disable the stuck request monitor 
   * request.tracking.dump           : Enable / disable thread dumping when stuck requests discovered (unix only) 
   * request.tracking.interval       : Frequency (ms) of stuck request inspection 
   * request.tracking.threshold      : Threshold (ms) before requests are considered "stuck" 
   * debug.hops                      : Log session hopping (ie. processing of session by more than one VM) 
   * debug.hops.interval             : Number of hops between debug printing 
   * debug.invalidate                : Log session invalidation vhosts.excluded : comma separated list of virtual hosts that should never use Terracotta clustered 
   *                                   sessions (tomcat only) 
   * session.debug.sessions          : output additional debug information when sessions are looked up, created, etc
   * </code>
   ********************************************************************************************************************/
  public static final String   SESSION_INVALIDATOR_SLEEP                              = "session.invalidator.sleep";
  public static final String   SESSION_INVALIDATOR_BENCH_ENABLED                      = "session.invalidator.bench.enabled";
  public static final String   SESSION_REQUEST_BENCH_ENABLED                          = "session.request.bench.enabled";
  public static final String   SESSION_REQUEST_TRACKING                               = "session.request.tracking";
  public static final String   SESSION_REQUEST_TRACKING_DUMP                          = "session.request.tracking.dump";
  public static final String   SESSION_REQUEST_TRACKING_INTERVAL                      = "session.request.tracking.interval";
  public static final String   SESSION_REQUEST_TRACKING_THRESHOLD                     = "session.request.tracking.threshold";
  public static final String   SESSION_DEBUG_HOPS                                     = "session.debug.hops";
  public static final String   SESSION_DEBUG_HOPS_INTERVAL                            = "session.debug.hops.interval";
  public static final String   SESSION_DEBUG_INVALIDATE                               = "session.debug.invalidate";
  public static final String   SESSION_DEBUG_SESSIONS                                 = "session.debug.sessions";
  public static final String   SESSION_VHOSTS_EXCLUDED                                = "session.vhosts.excluded";

  /*********************************************************************************************************************
   * Section : Memory Monitor
   ********************************************************************************************************************/
  public static final String   MEMORY_MONITOR_FORCEBASIC                              = "memory.monitor.forcebasic";

  /*********************************************************************************************************************
   * Section : Ehcache
   ********************************************************************************************************************/
  public static final String   EHCAHCE_LOGGING_ENABLED                                = "ehcache.logging.enabled";
  public static final String   EHCAHCE_EVICTOR_LOGGING_ENABLED                        = "ehcache.evictor.logging.enabled";
  public static final String   EHCAHCE_EVICTOR_POOL_SIZE                              = "ehcache.evictor.pool.size";
  public static final String   EHCAHCE_CONCURRENCY                                    = "ehcache.concurrency";
  public static final String   EHCAHCE_GLOBAL_EVICTION_ENABLE                         = "ehcache.global.eviction.enable";
  public static final String   EHCAHCE_GLOBAL_EVICTION_FREQUENCY                      = "ehcache.global.eviction.frequency";
  public static final String   EHCAHCE_GLOBAL_EVICTION_SEGMENTS                       = "ehcache.global.eviction.segments";
  public static final String   EHCAHCE_GLOBAL_EVICTION_REST_TIMEMILLS                 = "ehcache.global.eviction.rest.timeMillis";
  public static final String   EHCAHCE_LOCK_READLEVEL                                 = "ehcache.lock.readLevel";
  public static final String   EHCAHCE_LOCK_WRITELEVEL                                = "ehcache.lock.writeLevel";

  /*********************************************************************************************************************
   * Section : Lock statistics
   ********************************************************************************************************************/
  public static final String   LOCK_STATISTICS_ENABLED                                = "lock.statistics.enabled";
  public static final String   L1_LOCK_STATISTICS_TRACEDEPTH                          = "l1.lock.statistics.traceDepth";
  public static final String   L1_LOCK_STATISTICS_GATHERINTERVAL                      = "l1.lock.statistics.gatherInterval";

  /*********************************************************************************************************************
   * Section : Greedy Lease Lock
   ********************************************************************************************************************/
  public static final String   L2_LOCKMANAGER_GREEDY_LEASE_ENABLED                    = "l2.lockmanager.greedy.lease.enabled";
  public static final String   L2_LOCKMANAGER_GREEDY_LEASE_LEASETIME_INMILLS          = "l2.lockmanager.greedy.lease.leaseTimeInMillis";

  /*********************************************************************************************************************
   * Section : AdminConsole
   ********************************************************************************************************************/
  public static final String   CONSOLE_SHOW_OBJECTID                                  = "console.showObjectID";

  /*********************************************************************************************************************
   * <code>
   * Section : tcCom 
   * l2.tccom.workerthreads - Number of workers threads for network communications. Defaults to java.lang.Runtime.availableProcessors()
   * </code>
   ********************************************************************************************************************/
  // l2.tccom.workerthreads =
  /*********************************************************************************************************************
   * Section : TCP Settings
   ********************************************************************************************************************/
  public static final String   NET_CORE_KEEPALIVE                                     = "net.core.keepalive";
  public static final String   NET_CORE_RECV_BUFFER                                   = "net.core.recv.buffer";
  public static final String   NET_CORE_SEND_BUFFER                                   = "net.core.send.buffer";
  public static final String   NET_CORE_TCP_NO_DELAY                                  = "net.core.tcpnodelay";

  /*********************************************************************************************************************
   * <code>
   * Section : CVT 
   * cvt.retriever.notification.interval   - Interval between log file messages when the CVT retriever is running (in seconds)
   * cvt.statistics.logging.interval       - Interval between logging of statistics data (in seconds).
   * cvt.buffer.randomsuffix.enabled       - If true, add a random suffix when a buffer is created
   * cvt.store.randomsuffix.enabled        - If true, add a random suffix when a store is created
   * </code>
   ********************************************************************************************************************/
  public static final String   CVT_RETRIEVER_NOTIFICATION_INTERVAL                    = "cvt.retriever.notification.interval";
  public static final String   CVT_STATISTICS_LOGGING_INTERVAL                        = "cvt.statistics.logging.interval";
  public static final String   CVT_BUFFER_RANDOM_SUFFIX_ENABLED                       = "cvt.buffer.randomsuffix.enabled";
  public static final String   CVT_STORE_RANDOM_SUFFIX_ENABLED                        = "cvt.store.randomsuffix.enabled";

  /*********************************************************************************************************************
   * Section : HealthChecker { server->client, server->server (HA), client->server }
   ********************************************************************************************************************/
  public static final String   L2_HEALTHCHECK_L1_PING_ENABLED                         = "l2.healthcheck.l1.ping.enabled";
  public static final String   L2_HEALTHCHECK_L1_PING_IDLETIME                        = "l2.healthcheck.l1.ping.idletime";
  public static final String   L2_HEALTHCHECK_L1_PING_INTERVAL                        = "l2.healthcheck.l1.ping.interval";
  public static final String   L2_HEALTHCHECK_L1_PING_PROBES                          = "l2.healthcheck.l1.ping.probes";
  public static final String   L2_HEALTHCHECK_L1_SOCKECT_CONNECT                      = "l2.healthcheck.l1.socketConnect";
  public static final String   L2_HEALTHCHECK_L1_SOCKECT_CONNECT_TIMEOUT              = "l2.healthcheck.l1.socketConnectTimeout";
  public static final String   L2_HEALTHCHECK_L1_SOCKECT_CONNECT_COUNT                = "l2.healthcheck.l1.socketConnectCount";

  public static final String   L2_HEALTHCHECK_L2_PING_ENABLED                         = "l2.healthcheck.l2.ping.enabled";
  public static final String   L2_HEALTHCHECK_L2_PING_IDLETIME                        = "l2.healthcheck.l2.ping.idletime";
  public static final String   L2_HEALTHCHECK_L2_PING_INTERVAL                        = "l2.healthcheck.l2.ping.interval";
  public static final String   L2_HEALTHCHECK_L2_PING_PROBES                          = "l2.healthcheck.l2.ping.probes";
  public static final String   L2_HEALTHCHECK_L2_SOCKECT_CONNECT                      = "l2.healthcheck.l2.socketConnect";
  public static final String   L2_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT              = "l2.healthcheck.l2.socketConnectTimeout";
  public static final String   L2_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                = "l2.healthcheck.l2.socketConnectCount";

  public static final String   L1_HEALTHCHECK_L2_PING_ENABLED                         = "l1.healthcheck.l2.ping.enabled";
  public static final String   L1_HEALTHCHECK_L2_PING_IDLETIME                        = "l1.healthcheck.l2.ping.idletime";
  public static final String   L1_HEALTHCHECK_L2_PING_INTERVAL                        = "l1.healthcheck.l2.ping.interval";
  public static final String   L1_HEALTHCHECK_L2_PING_PROBES                          = "l1.healthcheck.l2.ping.probes";
  public static final String   L1_HEALTHCHECK_L2_SOCKECT_CONNECT                      = "l1.healthcheck.l2.socketConnect";
  public static final String   L1_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT              = "l1.healthcheck.l2.socketConnectTimeout";
  public static final String   L1_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                = "l1.healthcheck.l2.socketConnectCount";

  /*********************************************************************************************************************
   * <code>
   * Section : TCMessage debug monitoring 
   * tcm.monitor.enabled - if enabled the count and size of TC messages will be collected and logged 
   * tcm.monitor.delay   - the delay (in seconds) between reporting to the log
   * </code>
   ********************************************************************************************************************/
  public static final String   TCM_MONITOR_ENABLED                                    = "tcm.monitor.enabled";
  public static final String   TCM_MONITOR_DELAY                                      = "tcm.monitor.delay";

  /*********************************************************************************************************************
   * <code>
   * Section : HTTP 
   * http.defaultservlet.enabled                - If true, will serve files through embedded HTTP server
   * http.defaultservlet.attribute.aliases      - If true, allows aliases like symlinks to be followed while serving files
   * http.defaultservlet.attribute.dirallowed   - If true, directory listings are returned if no welcome file is found
   * </code>
   ********************************************************************************************************************/
  public static final String   HTTP_DEFAULT_SERVLET_ENABLED                           = "http.defaultservlet.enabled";
  public static final String   HTTP_DEFAULT_SERVLET_ATTRIBUTE_ALIASES                 = "http.defaultservlet.attribute.aliases";
  public static final String   HTTP_DEFAULT_SERVLET_ATTRIBUTE_DIR_ALLOWED             = "http.defaultservlet.attribute.dirallowed";
}
