/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  static final String[]      OLD_PROPERTIES                                                 = { "l1.reconnect.enabled",
      "l1.reconnect.timeout.millis", "l2.nha.ooo.maxDelayedAcks", "l2.nha.ooo.sendWindow",
      "l2.objectmanager.loadObjectID.checkpoint.changes", "l2.objectmanager.loadObjectID.checkpoint.timeperiod",
      "l2.nha.groupcomm.type", "l2.nha.tribes.failuredetector.millis", "l2.nha.tribes.orderinterceptor.enabled",
      "l2.nha.tribes.mcast.mcastPort", "l2.nha.tribes.mcast.mcastAddress", "l2.nha.tribes.mcast.memberDropTime",
      "l2.nha.tribes.mcast.mcastFrequency", "l2.nha.tribes.mcast.tcpListenPort", "l2.nha.tribes.mcast.tcpListenHost",
      "l2.nha.mcast.enabled", "l2.nha.tcgroupcomm.response.timelimit", "net.core.recv.buffer", "net.core.send.buffer",
      "l2.objectmanager.loadObjectID.measure.performance", "console.showObjectID", "l2.lfu.debug.enabled",
      "l1.serverarray.objectCreationStrategy.roundRobin.coordinatorLoad", "l2.objectmanager.loadObjectID.fastLoad",
      "ehcache.incoherent.putsBatchSize", "ehcache.incoherent.throttlePutsAtSize",
      "l2.objectmanager.dgc.young.enabled", "l2.objectmanager.dgc.young.frequencyInMillis",
      "l2.objectmanager.dgc.enterpriseMarkStageInterval", "l2.objectmanager.dgc.faulting.optimization",
      "l2.cachemanager.enabled", "l2.cachemanager.logging.enabled", "l2.cachemanager.leastCount",
      "l2.cachemanager.percentageToEvict", "l2.cachemanager.sleepInterval", "l2.cachemanager.criticalThreshold",
      "l2.cachemanager.threshold", "l2.cachemanager.monitorOldGenOnly", "l2.cachemanager.criticalObjectThreshold",
      "l2.cachemanager.resourcePollInterval", "l2.objectmanager.deleteBatchSize", "l2.objectmanager.cachePolicy",
      "l2.objectmanager.maxObjectsToCommit", "l2.objectmanager.maxObjectsInTxnObjGrouping",
      "l2.objectmanager.fault.logging.enabled", "l2.objectmanager.flush.logging.enabled",
      "l2.objectmanager.loadObjectID.longsPerDiskEntry", "l2.objectmanager.loadObjectID.checkpoint.changes",
      "l2.objectmanager.loadObjectID.checkpoint.maxlimit", "l2.objectmanager.loadObjectID.checkpoint.timeperiod",
      "l2.data.backup.throttle.timeInMillis", "l2.objectmanager.loadObjectID.mapsdatabase.longsPerDiskEntry",
      "l2.objectmanager.loadObjectID.measure.performance", "l2.objectmanager.loadObjectID.checkpoint.maxsleep",
      "l2.seda.faultstage.threads", "l2.seda.flushstage.threads", "l2.seda.commitstage.threads",
      "l2.seda.gcdeletestage.threads", "l2.berkeleydb.je.lock.timeout", "l2.berkeleydb.je.maxMemoryPercent",
      "l2.berkeleydb.je.lock.nLockTables", "l2.berkeleydb.je.cleaner.bytesInterval",
      "l2.berkeleydb.je.checkpointer.bytesInterval", "l2.berkeleydb.je.cleaner.detailMaxMemoryPercentage",
      "l2.berkeleydb.je.cleaner.lookAheadCacheSize", "l2.berkeleydb.je.cleaner.minAge",
      "l2.derbydb.derby.storage.pageSize", "l2.derbydb.derby.storage.pageCacheSize",
      "l2.derbydb.derby.system.durability", "l2.derbydb.derby.stream.error.method",
      "l2.derbydb.derby.maxMemoryPercent", "l2.derbydb.derby.storage.logBufferSize", "l2.derbydb.logDevice",
      "l2.derbydb.derby.storage.checkpointInterval", "l2.derbydb.derby.storage.logSwitchInterval",
      "l2.derbydb.derby.locks.escalationThreshold", "l2.derbydb.derby.locks.deadlockTimeout",
      "l2.derbydb.derby.locks.waitTimeout", "l2.derbydb.derby.locks.deadlockTrace", "l2.lfu.agingFactor",
      "l2.lfu.recentlyAccessedIgnorePercentage", "aw.asmclassinfo.ignore.errors",
      "l2.offHeapCache.operator.event.generator.threshold", "l2.offHeapCache.operator.event.generator.sleepInterval",
      "l2.offHeapCache.allocation.slow", "l2.offHeapCache.allocation.critical",
      "l2.offHeapCache.allocation.critical.halt", "l2.offHeapCache.min.page.size", "l2.offHeapCache.max.page.size",
      "l2.offHeapCache.max.page.count", "l2.offHeapCache.map.tableSize", "l2.offHeapCache.map.concurrency",
      "l2.offHeapCache.operator.event.generator.threshold", "l2.offHeapCache.operator.event.generator.sleepInterval",
      "l2.offHeapCache.max.chunk.size", "l2.offHeapCache.min.chunk.size", "l2.offHeapCache.object.initialDataSize",
      "l2.offHeapCache.object.tableSize", "l2.offHeapCache.object.concurrency",
      "l2.offHeapCache.temp.swap.flush.to.disk.count", "l2.offHeapCache.temp.swap.throttle.megaBytes",
      "l2.offHeapCache.skip.jvmarg.check", "l1.cachemanager.enabled", "l1.cachemanager.logging.enabled",
      "l1.cachemanager.leastCount", "l1.cachemanager.percentageToEvict", "l1.cachemanager.sleepInterval",
      "l1.cachemanager.criticalThreshold", "l1.cachemanager.threshold", "l1.cachemanager.monitorOldGenOnly",
      "l1.cachemanager.criticalObjectThreshold", "l1.connect.versionMatchCheck.enabled", "l1.jvm.check.compatibility",
      "l1.max.connect.retries"                                                             };

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Evictor properties
   * Description : This section contains the defaults for evictor on the L2
   * resourcePoolInterval    : poll time on resource monitoring in msec
   * haltThreshold           : the threshold where l2 singles l1 to halt additive operations
   * criticalUpperbound      : the upperbound bytes available above the threshold levels
   * criticalLowerbound      : the lowerbound bytes available above the threshold levels
   * vital offheap stoppage      : the lowerbound bytes available above the threshold levels
   * criticalLowerbound      : the lowerbound bytes available above the threshold levels
   * </code>
   ********************************************************************************************************************/

  public static final String L2_EVICTION_CRITICALTHRESHOLD                                  = "l2.eviction.criticalThreshold";
  public static final String L2_EVICTION_RESOURCEPOLLINGINTERVAL                            = "l2.eviction.resourcePollInterval";
  public static final String L2_EVICTION_HALTTHRESHOLD                                      = "l2.eviction.haltThreshold";
  public static final String L2_EVICTION_OFFHEAP_STOPPAGE                                   = "l2.eviction.offheap.stoppage";
  public static final String L2_EVICTION_STORAGE_STOPPAGE                                   = "l2.eviction.storage.stoppage";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Transaction Manager Properties
   * logging.enabled              : Enable/disable L2's tx mgr logging
   * logging.verbose              : Turns on debug loggings for tx mgr
   * logging.printStats           : Enables/disables logging for tx stats
   * logging.printCommits         : Enables/disables logging for tx commits
   * logging.printBroadCastStats  : Enables/disables logging for tx Broadcasts
   * passive.throttle.enabled     : Enables/disables throttling of active from Passive when
   *                                the number of pending txns reaches the threshold
   * passive.throttle.threshold   : Number of pending transactions after which passive will
   *                                throttle the active
   * passive.throttle.maxSleepSeconds  : Sleep time for active when passive throttles it
   * broadcast.durability.level   : Controls how much persistence to ensure before sending out the result
   *                                of a transaction.
   *                                   - NONE : just send it immediately
   *                                   - RELAYED : make sure it's relayed to all passives
   *                                   - DISK : make sure it's on disk (only applicable when restartable is on)
   * </code>
   ********************************************************************************************************************/
  public static final String L2_TRANSACTIONMANAGER_LOGGING_ENABLED                          = "l2.transactionmanager.logging.enabled";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_VERBOSE                          = "l2.transactionmanager.logging.verbose";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINTSTATS                       = "l2.transactionmanager.logging.printStats";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINTCOMMITS                     = "l2.transactionmanager.logging.printCommits";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINT_BROADCAST_STATS            = "l2.transactionmanager.logging.printBroadcastStats";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_ENABLED                 = "l2.transactionmanager.passive.throttle.enabled";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_THRESHOLD               = "l2.transactionmanager.passive.throttle.threshold";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_MAXSLEEPSECONDS         = "l2.transactionmanager.passive.throttle.maxSleepSeconds";
  public static final String L2_TRANSACTIONMANAGER_BROADCAST_DURABILITY_LEVEL               = "l2.transactionmanager.broadcast.durability.level";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Object Manager Properties Description :
   * This section contains the defaults for the object manager of the L2
   * maxTxnsInTxnObjectGrouping     - Max number of Transactions allowed in the
   *                                  TransactionalObject grouping
   * objectrequest.split.size       - The maximum number of objects that l2 will lookup in one shot
   * objectrequest.logging.enabled  - Turn on logging to see what object request cache saved
   * request.logging.enabled        - Enables/Disables logging of ManagedObject requests from
   *                                  clients. If enabled, logs counts of requested instance types
   *                                  every 5 seconds.
   * persistor.logging.enabled      - Enables/Disables logging of commits to disk while running
   *                                  in persistent mode.
   * passive.sync.batch.size        - Number of objects in each message that is sent from
   *                                  active to passive while synching
   * passive.sync.throttle.timeInMillis - Time to wait before sending the next batch of
   *                                  objects to the passive
   * dgc.throttle.timeInMillis     - Throttle time for dgc for each cycle for every requestsPerThrottle
   *                                 requests for references from object manager
   * dgc.throttle.requestsPerThrottle - Number of objects for which object references are requested
   *                                 from object manager after which dgc will throttle
   * dgc.inline.enabled             - Enables/disable inline dgc of CDSMs.
   * dgc.inline.intervalInSeconds   - Interval in seconds at which to delete objects selected by inline dgc.
   * dgc.inline.maxObjects          - Maximum inline dgc batch size
   * dgc.inline.cleanup.delaySeconds - Seconds to delay the start of inline dgc cleanup after a server becomes active
   * l2.objectmanager.invalidate.strong.cache.enabled - Enable/disable invalidations for strong cache
   * </code>
   ********************************************************************************************************************/

  public static final String L2_OBJECTMANAGER_MAXTXNS_INTXNOBJECT_GROUPING                  = "l2.objectmanager.maxTxnsInTxnObjectGrouping";
  public static final String L2_OBJECTMANAGER_OBJECT_REQUEST_SPLIT_SIZE                     = "l2.objectmanager.objectrequest.split.size";
  public static final String L2_OBJECTMANAGER_OBJECT_REQUEST_LOGGING_ENABLED                = "l2.objectmanager.objectrequest.logging.enabled";
  public static final String L2_OBJECTMANAGER_REQUEST_LOGGING_ENABLED                       = "l2.objectmanager.request.logging.enabled";
  public static final String L2_OBJECTMANAGER_REQUEST_PREFETCH_ENABLED                      = "l2.objectmanager.request.prefetch.enabled";
  public static final String L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED                     = "l2.objectmanager.persistor.logging.enabled";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE                       = "l2.objectmanager.passive.sync.batch.size";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_MAXSIZE_MB               = "l2.objectmanager.passive.sync.message.maxSizeInMegaBytes";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_TIME                    = "l2.objectmanager.passive.sync.throttle.timeInMillis";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_PENDING_MSGS            = "l2.objectmanager.passive.sync.throttle.maxPendingMessages";
  public static final String L2_OBJECTMANAGER_DGC_THROTTLE_TIME                             = "l2.objectmanager.dgc.throttle.timeInMillis";
  public static final String L2_OBJECTMANAGER_DGC_REQUEST_PER_THROTTLE                      = "l2.objectmanager.dgc.throttle.requestsPerThrottle";
  public static final String L2_OBJECTMANAGER_DGC_INLINE_ENABLED                            = "l2.objectmanager.dgc.inline.enabled";
  public static final String L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS                   = "l2.objectmanager.dgc.inline.intervalInSeconds";
  public static final String L2_OBJECTMANAGER_DGC_INLINE_MAX_OBJECTS                        = "l2.objectmanager.dgc.inline.maxObjects";
  public static final String L2_OBJECTMANAGER_DGC_INLINE_CLEANUP_DELAY_SECONDS              = "l2.objectmanager.dgc.inline.cleanup.delaySeconds";
  public static final String L2_OBJECTMANAGER_INVALIDATE_STRONG_CACHE_ENABLED               = "l2.objectmanager.invalidateStrongCache.enabled";
  public static final String L2_OBJECTMANAGER_OIDSET_TYPE                                   = "l2.objectmanager.oidset.type";
  public static final String L2_OBJECTMANAGER_CLIENT_STATE_VERBOSE_THRESHOLD                = "l2.objectmanager.client.state.verbose.threshold";

  /**
   * ******************************************************************************************************************
   * <code>
   * Section : L2 FRS Properties
   * Description : This section contains configuration for FRS on the L2
   * compactor.policy                   : Compactor policy to use. One of LSNGapCompactionPolicy, SizeBasedCompactionPolicy
   * compactor.lsnGap.minLoad           : LSNGapCompactionPolicy: lower load threshold before compaction starts.
   * compactor.lsnGap.maxLoad           : LSNGapCompactionPolicy: load at which a compaction run stops.
   * compactor.sizeBased.threshold      : SizeBasedCompactionPolicy: Start threshold.
   * compactor.sizeBased.amount         : SizeBasedCompactionPolicy: Amount to compact per compactor run.
   * </code>
   * ******************************************************************************************************************
   */
  public static final String L2_FRS_PREFIX                                                  = "l2.frs";
  public static final String L2_FRS_COMPACTOR_POLICY                                        = L2_FRS_PREFIX
                                                                                              + ".compactor.policy";
  public static final String L2_FRS_COMPACTOR_LSNGAP_MIN_LOAD                               = L2_FRS_PREFIX
                                                                                              + ".compactor.lsnGap.minLoad";
  public static final String L2_FRS_COMPACTOR_LSNGAP_MAX_LOAD                               = L2_FRS_PREFIX
                                                                                              + ".compactor.lsnGap.maxLoad";
  public static final String L2_FRS_COMPACTOR_SIZEBASED_THRESHOLD                           = L2_FRS_PREFIX
                                                                                              + ".compactor.sizeBased.threshold";
  public static final String L2_FRS_COMPACTOR_SIZEBASED_AMOUNT                              = L2_FRS_PREFIX
                                                                                              + ".compactor.sizeBased.amount";

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
  public static final String L2_SEDA_APPLY_STAGE_THREADS                                    = "l2.seda.apply.stage.threads";
  public static final String L2_SEDA_MANAGEDOBJECTRESPONSESTAGE_THREADS                     = "l2.seda.managedobjectresponsestage.threads";
  public static final String L2_SEDA_MANAGEDOBJECTREQUESTSTAGE_THREADS                      = "l2.seda.managedobjectrequeststage.threads";
  public static final String L2_SEDA_STAGE_SINK_CAPACITY                                    = "l2.seda.stage.sink.capacity";
  public static final String L2_SEDA_EVICTION_PROCESSORSTAGE_SINK_SIZE                      = "l2.seda.evictionprocessorstage.sink.capacity";
  public static final String L2_SEDA_SEARCH_THREADS                                         = "l2.seda.search.threads";
  public static final String L2_SEDA_QUERY_THREADS                                          = "l2.seda.query.threads";
  public static final String L2_SEDA_SERVER_MAP_CAPACITY_EVICTION_STAGE_THREADS             = "l2.seda.server.map.capacity.eviction.stage.threads";
  public static final String L2_LOCAL_CACHE_TXN_COMPLETE_THREADS                            = "l2.seda.local.cache.transaction.complete.threads";
  public static final String L2_LOCAL_CACHE_TXN_COMPLETE_SINK_CAPACITY                      = "l2.seda.local.cache.transaction.complete.sink.capacity";
  public static final String L2_LOCAL_CACHE_INVALIDATIONS_SINK_CAPACITY                     = "l2.seda.local.cache.invalidations.sink.capacity";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Seda stage properties
   * Description : This section contains configuration for SEDA stages for L1
   * stage.sink.capacity  : Capacity of L1's seda stage queue, Integer.MAX_VALUE if not set
   * pinned.entry.fault.stage.threads : Number of threads for pinned entry fault stage
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SEDA_STAGE_SINK_CAPACITY                                    = "l1.seda.stage.sink.capacity";
  public static final String L1_SEDA_PINNED_ENTRY_FAULT_STAGE_THREADS                       = "l1.seda.pinned.entry.fault.stage.threads";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Bean shell Properties
   * Description : Bean shell can be enabled in the server for debugging.
   * enabled     : Enables/disables Beanshell
   * port        : Port number for Beanshell
   * </code>
   ********************************************************************************************************************/
  public static final String L2_BEANSHELL_ENABLED                                           = "l2.beanshell.enabled";
  public static final String L2_BEANSHELL_PORT                                              = "l2.beanshell.port";

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
  public static final String L2_NHA_SEND_TIMEOUT_MILLS                                      = "l2.nha.send.timeout.millis";
  public static final String L2_NHA_DIRTYDB_AUTODELETE                                      = "l2.nha.dirtydb.autoDelete";
  public static final String L2_NHA_DIRTYDB_ROLLING                                         = "l2.nha.dirtydb.rolling";
  public static final String L2_NHA_AUTORESTART                                             = "l2.nha.autoRestart";
  public static final String L2_NHA_DIRTYDB_BACKUP_ENABLED                                  = "l2.nha.dirtydb.backup.enabled";

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
  public static final String L2_ENABLE_LEGACY_PRODUCTION_MODE                               = "l2.enable.legacy.production.mode";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Server Array Properties
   * serverarray.2pc.enabled  :Enables/disables 2 phase commit for enterprise server array
   *                           (experimental, do not change)
   * </code>
   ********************************************************************************************************************/
  public static final String L2_SERVERARRAY_2PC_ENABLED                                     = "l2.serverarray.2pc.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Server Array Properties
   * objectCreationStrategy     - Supported types round-robin, group-affinity
   * roundRobin.startIndex      - The first index to start at for each client. Supports
   *                              sequential, random
   * roundRobin.coordinatorLoad - Load to apply to coordinator in % compared to other groups
   *                              [0-100], 100 being equal load as others
   * 
   * objectCreationStrategy.groupAffinity- Mirror group name as available in tc-config,
   *                                      if group-affinity object creation strategy
   *                                      is chosen
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY                          = "l1.serverarray.objectCreationStrategy";
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY_GROUPAFFINITY_GROUPNAME  = "l1.serverarray.objectCreationStrategy.groupAffinity.groupName";
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY_ROUND_ROBIN_START_INDEX  = "l1.serverarray.objectCreationStrategy.roundRobin.startIndex";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 L2 Config match Property
   * Description : This property will check if the client has to match server config i.e. check cluster topology
   * </code>
   ********************************************************************************************************************/
  public static final String L1_L2_CONFIG_VALIDATION_ENABLED                                = "l1.l2.config.validation.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Memory Manager Properties
   * Description : This section contains the defaults for the cache manager for the L1
   * criticalThreshold   : % of memory used after which memory manager will evict aggressively
   * </code>
   ********************************************************************************************************************/
  public static final String L1_MEMORYMANAGER_CRITICAL_THRESHOLD                            = "l1.memorymanager.criticalThreshold";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Transaction Manager Properties
   * Description : This section contains the defaults for the Transaction manager for the L1
   *    logging.enabled            - If true, enables some logging in the transaction manager
   *    maxOutstandingBatchSize    - The max number of batches of transaction that each L1
   *                                 sends to the L2 at once
   *    maxBatchSizeInKiloBytes    - The max size of  batches that are send to the L2 from
   *                                 the L1. The units is in Kilobytes
   *    maxPendingBatches          - The max number of pending batches the client creates
   *                                 before a Batch ack is received from the server, after
   *                                 which the client stalls until a Batch ack is received.
   *    maxSleepTimeBeforeHalt     - The max time that a user thread will wait for L2 to
   *                                 catchup if the L2 is behind applying transactions. This
   *                                 time is used before maxPendingBatches is reached. The
   *                                 units are in milliseconds
   *    completedAckFlushTimeout   - The timeout in milliseconds after which a NullTransaction
   *                                 is send to the server if completed txn acks are still pending
   *    strings.compress.enabled   - Enables string compression when sending to the L2. There
   *                                 is a processing overhead at the L1, but saves network
   *                                 bandwidth, reduces memory requirements in the L2 and also
   *                                 reduces disk io at the L2.
   *    strings.compress.minSize   - Strings with lengths less that this number are not
   *                                 compressed
   *    folding.enabled            - True/false whether txn folding is enabled. Folding is
   *                                 the act of combining similar (but unique) application
   *                                 transactions into a single txn (for more optimal processing
   *                                 on the server). Only transactions that share common locks
   *                                 and objects can be folded.
   *    folding.lock.limit         - The maximum number of distinct locks permitted in folded txns
   *                                 (0 or less means infinite)
   *    folding.object.limit       - Object count threshold for short circuiting txn folding logic
   *                                 (0 or less means infinite). If a txn contains more distinct
   *                                 than this threshold, there will be no search to determine a
   *                                 possible fold target
   *    folding.debug              - Enable debug logging for the transaction folder. Use with
   *                                 care -- This will cause *lots* of logging to occur
   *    timeoutForAckOnExit        - Max wait time in seconds to wait for ACKs before exit.
   *                                 value 0 for infinite wait.
   * </code>
   ********************************************************************************************************************/
  public static final String L1_TRANSACTIONMANAGER_LOGGING_ENABLED                          = "l1.transactionmanager.logging.enabled";
  public static final String L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE                 = "l1.transactionmanager.maxOutstandingBatchSize";
  public static final String L1_TRANSACTIONMANAGER_MAXBATCHSIZE_INKILOBYTES                 = "l1.transactionmanager.maxBatchSizeInKiloBytes";
  public static final String L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES                       = "l1.transactionmanager.maxPendingBatches";
  public static final String L1_TRANSACTIONMANAGER_MAXSLEEPTIME_BEFOREHALT                  = "l1.transactionmanager.maxSleepTimeBeforeHalt";
  public static final String L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT              = "l1.transactionmanager.completedAckFlushTimeout";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED                 = "l1.transactionmanager.strings.compress.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED         = "l1.transactionmanager.strings.compress.logging.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE                 = "l1.transactionmanager.strings.compress.minSize";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_ENABLED                          = "l1.transactionmanager.folding.enabled";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT                     = "l1.transactionmanager.folding.object.limit";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT                       = "l1.transactionmanager.folding.lock.limit";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_DEBUG                            = "l1.transactionmanager.folding.debug";
  public static final String L1_TRANSACTIONMANAGER_TIMEOUTFORACK_ONEXIT                     = "l1.transactionmanager.timeoutForAckOnExit";

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
  public static final String L2_L1REJOIN_SLEEP_MILLIS                                       = "l2.l1rejoin.sleep.millis";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Object Manager Properties
   * Description : This section contains the defaults for the Object manager for the L1
   * remote.maxDNALRUSize    : Count of dnas after which l1s will remove unrequested object
   * remote.logging.enabled  : Enable/disable logging of remote object manager
   * remote.maxRequestSentImmediately
   *                         : Maximum number of requests send immediately after which it will be batched
   * objectid.request.size   : Number of object ids requested at once from L2 for creating
   *                           new objects
   * flush.logging.enabled   : Enable/disable object's flush logging
   * fault.logging.enabled   : Enable/disable object's fault logging
   * removed.objects.send.timer : Max interval in milliseconds before sending a batch of removed object ids
   * removed.objects.threshold : Max number of removed objects before immediately sending a batch of removed ids.
   * fault.count               : Default number of additional reachable objects to also fault when requesting a remote object
   * </code>
   ********************************************************************************************************************/
  public static final String L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE                        = "l1.objectmanager.remote.maxDNALRUSize";
  public static final String L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED                        = "l1.objectmanager.remote.logging.enabled";
  public static final String L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY           = "l1.objectmanager.remote.maxRequestSentImmediately";
  public static final String L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD               = "l1.objectmanager.remote.batchLookupTimePeriod";
  public static final String L1_OBJECTMANAGER_OBJECTID_REQUEST_SIZE                         = "l1.objectmanager.objectid.request.size";
  public static final String L1_OBJECTMANAGER_FLUSH_LOGGING_ENABLED                         = "l1.objectmanager.flush.logging.enabled";
  public static final String L1_OBJECTMANAGER_FAULT_LOGGING_ENABLED                         = "l1.objectmanager.fault.logging.enabled";
  public static final String L1_OBJECTMANAGER_REMOVED_OBJECTS_SEND_TIMER                    = "l1.objectmanager.removed.objects.send.timer";
  public static final String L1_OBJECTMANAGER_REMOVED_OBJECTS_THRESHOLD                     = "l1.objectmanager.removed.objects.threshold";
  public static final String L1_OBJECTMANAGER_FAULT_COUNT                                   = "l1.objectmanager.fault.count";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 ServerMap Manager Properties
   * Description : This section contains the defaults for the ServerMap manager for the L1
   * remote.maxRequestSentImmediately
   *                         : Maximum number of requests send immediately after which it will be batched
   * remote.batchLookupTimePeriod
   *                         : Time to wait before sending batch requests
   * faultInvalidatedPinnedEntries : If enabled pinned entries will be faulted again from L2 on invalidations.
   * 
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SERVERMAPMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY        = "l1.servermapmanager.remote.maxRequestSentImmediately";
  public static final String L1_SERVERMAPMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD            = "l1.servermapmanager.remote.batchLookupTimePeriod";
  public static final String L1_SERVERMAPMANAGER_FAULT_INVALIDATED_PINNED_ENTRIES           = "l1.servermapmanager.faultInvalidatedPinnedEntries";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 ServerMap Properties
   * Description : This section contains the defaults for the ServerMap for the L2
   * 
   * eviction.clientObjectReferences.refresh.interval
   *              : ServerMap Eviction Client Object References refresh interval in milliseconds
   * eviction.broadcast.maxkeys
   *              : ServerMap Eviction Broadcast Message contain max key count entries
   * </code>
   ********************************************************************************************************************/
  public static final String L2_SERVERMAP_EVICTION_CLIENTOBJECT_REFERENCES_REFRESH_INTERVAL = "l2.servermap.eviction.clientObjectReferences.refresh.interval";
  public static final String L2_SERVERMAP_EVICTION_BROADCAST_MAXKEYS                        = "l2.servermap.eviction.broadcast.maxkeys";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Logging properties for both L1 and L2
   * Description : Logging attributes that can be overridden.
   * maxBackups       - The maximum number of backup log files to keep maxLogFileSize - The maximum size of a log file in megabytes
   * longgc.threshold - DGC taking greater than the time mentioned will be logged
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
   * Section                         : Session properties (applies to all DSO session enabled web apps in this VM)
   * id.length                       : The length (in chars) for session identifiers (min 8)
   * serverid                        : The server identifier to place in the session ID
   * delimiter                       : The delimiter that separates the server ID from the session ID
   * cookie.domain                   : Domain value for session cookie
   * cookie.secure                   : Enable / disable the secure flag in the session cookie
   * cookie.maxage.seconds           : The maximum lifetime of the session cookie
   * cookie.name                     : Name of the session cookie cookie.enabled : Enable / disable the use of cookies for session tracking
   * maxidle.seconds                 : Session idle timeout in seconds
   * tracking.enabled                : Enable / disable session tracking completely
   * urlrewrite.enabled              : Enable / disable the URL functionality
   * attribute.listeners             : Comma separated list of HttpSessionAttributeListener classes
   * listeners                       : Comma separated list of HttpSessionListener
   * debug.invalidate                : Log session invalidation vhosts.excluded : comma separated list of virtual hosts that should never use Terracotta clustered
   *                                   sessions (tomcat only)
   * debug.sessions                  : output additional debug information when sessions are looked up, created, etc
   * </code>
   ********************************************************************************************************************/
  public static final String SESSION_INVALIDATOR_SLEEP                                      = "session.invalidator.sleep";
  public static final String SESSION_DEBUG_INVALIDATE                                       = "session.debug.invalidate";
  public static final String SESSION_DEBUG_SESSIONS                                         = "session.debug.sessions";

  /*********************************************************************************************************************
   * <code>
   * Section : Memory Monitor
   * forcebasic : enable/disable only basic memory monitoring
   * </code>
   ********************************************************************************************************************/
  public static final String MEMORY_MONITOR_FORCEBASIC                                      = "memory.monitor.forcebasic";

  /*********************************************************************************************************************
   * <code>
   * Section : Ehcache
   * clusterAllCacheManagers    : Whether all CacheManager instances are auto-clustered by default,
   *                              i.e. whether static fields CacheManager.ALL_CACHE_MANAGERS and
   *                              CacheManager.singleton will be configured as roots.
   *  logging.enabled           : Enable/disable ehcache logging
   *  evictor.logging.enabled   : Enable/disable evictor's logging
   *  concurrency               : Specifies the number of internal segments and gates the maximum
   *                              number of possible concurrent writers to the cache at one time.
   *                              There is memory and management overhead associated with each
   *                              segment. It is best for the hash function used in tim-ehcache
   *                              if the concurrency is a power of 2.
   *  evictor.pool.size         : Thread pool size for evictor
   *  global.eviction.enable    : Enable/disable global eviction from the cache
   *  global.eviction.frequency : Number of local eviction cycles after which global eviction may
   *                              start
   *  global.eviction.segments  : Number of segments of objects for global evictor
   *  global.eviction.rest.timeMillis : Sleep time between each segment's eviction
   *  readLevel                 : The lock level used during cache read operations. Allowed values are
   *                              READ (default), CONCURRENT, and NO_LOCK.  NO_LOCK is only appropriate
   *                              in the case of read-only or single-threaded cache usage.
   *  writeLevel                : The lock level used during cache write operations.  Allowed values are
   *                              WRITE (default), and CONCURRENT.  WRITE is strongly recommended.
   *  storageStrategy.dcv2.localcache.enabled
   *                            : The property enabled/disables the local cache when ehcache has a
   *                              storage strategy of DCV2
   *  storageStrategy.dcv2.periodicEviction.enabled
   *                            : The property enabled/disables the periodic eviction when ehcache has a
   *                              storage strategy of DCV2
   *  storageStrategy.dcv2.localcache.incoherentCachedItemsRecycleMillis
   *                            : The maximum time in millis after which incoherent cached items will be discarded from the local cache.
   *  storageStrategy.dcv2.eviction.overshoot
   *                            : % overshoot required to trigger capacity eviction
   *  clustered.config.override.mode
   *                            : Configures the level of configuration override. Choices are:
   *                                NONE - Override no local configuration with cluster configurations
   *                                GLOBAL - Only override settings applicable to both server and client components of a cache
   *                                ALL - Accepts overrides from the server for all settings
   * 
   * 
   * </code>
   ********************************************************************************************************************/
  public static final String EHCACHE_CLUSTER_ALL_CACHE_MANAGERS                             = "ehcache.clusterAllCacheManagers";
  public static final String EHCACHE_LOGGING_ENABLED                                        = "ehcache.logging.enabled";
  public static final String EHCACHE_EVICTOR_LOGGING_ENABLED                                = "ehcache.evictor.logging.enabled";
  public static final String EHCACHE_EVICTOR_POOL_SIZE                                      = "ehcache.evictor.pool.size";
  public static final String EHCACHE_CONCURRENCY                                            = "ehcache.concurrency";
  public static final String EHCACHE_GLOBAL_EVICTION_ENABLE                                 = "ehcache.global.eviction.enable";
  public static final String EHCACHE_GLOBAL_EVICTION_FREQUENCY                              = "ehcache.global.eviction.frequency";
  public static final String EHCACHE_GLOBAL_EVICTION_SEGMENTS                               = "ehcache.global.eviction.segments";
  public static final String EHCACHE_GLOBAL_EVICTION_REST_TIMEMILLS                         = "ehcache.global.eviction.rest.timeMillis";
  public static final String EHCACHE_LOCK_READLEVEL                                         = "ehcache.lock.readLevel";
  public static final String EHCACHE_LOCK_WRITELEVEL                                        = "ehcache.lock.writeLevel";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED                = "ehcache.storageStrategy.dcv2.localcache.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_PERELEMENT_TTI_TTL_ENABLED        = "ehcache.storageStrategy.dcv2.perElementTTITTL.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_EVICT_UNEXPIRED_ENTRIES_ENABLED   = "ehcache.storageStrategy.dcv2.evictUnexpiredEntries.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED          = "ehcache.storageStrategy.dcv2.periodicEviction.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT                = "ehcache.storageStrategy.dcv2.eviction.overshoot";
  public static final String EHCACHE_CLUSTERED_CONFIG_OVERRIDE_MODE                         = "ehcache.clustered.config.override.mode";
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
  public static final String L2_LOCKMANAGER_GREEDY_LOCKS_ENABLED                            = "l2.lockmanager.greedy.locks.enabled";
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
   * Section : HTTP
   * http.defaultservlet.enabled                - If true, will serve files through embedded HTTP server
   * http.defaultservlet.attribute.aliases      - If true, allows aliases like symlinks to be followed while serving files
   * http.defaultservlet.attribute.dirallowed   - If true, directory listings are returned if no welcome file is found
   * </code>
   ********************************************************************************************************************/
  public static final String HTTP_DEFAULT_SERVLET_ENABLED                                   = "http.defaultservlet.enabled";
  public static final String HTTP_DEFAULT_SERVLET_ATTRIBUTE_ALIASES                         = "http.defaultservlet.attribute.aliases";
  public static final String HTTP_DEFAULT_SERVLET_ATTRIBUTE_DIR_ALLOWED                     = "http.defaultservlet.attribute.dirallowed";

  /*********************************************************************************************************************
   * <code>
   * Section : Remote JMX
   * l2.remotejmx.maxthreads                     - Maximum number of concurrent remote jmx operations permitted
   * l2.remotejmx.idletime                       - Idle timeout (in seconds) for remote jmx processing threads
   * </code>
   ********************************************************************************************************************/
  public static final String L2_REMOTEJMX_MAXTHREADS                                        = "l2.remotejmx.maxthreads";
  public static final String L2_REMOTEJMX_IDLETIME                                          = "l2.remotejmx.idletime";
  public static final String L2_REMOTEJMX_CONNECT_TIMEOUT                                   = "l2.remotejmx.connect.timeout";

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
   * REST management Settings
   *  management.rest.enabled       -   Enable or disable the management REST facilities
   * </code>
   ********************************************************************************************************************/
  public static final String MANAGEMENT_REST_ENABLED                                        = "management.rest.enabled";

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
   * Section :  OffHeap Settings
   * l2.offHeap.allocation.partial.disable.maps - disable the partial allocation of map storage
   * l2.offHeap.allocation.partial.disable.objects - disable the partial allocation of objectdb storage
   * l2.offHeap.allocation.partial.disable.map.hotset - disable caching map values in offheap
   * l2.offHeap.allocation.partial.enable.object.hotset - enable caching object values in offheap
   * 
   * </code>
   ********************************************************************************************************************/

  public static final String L2_OFFHEAP_ALLOCATION_SLOW                                     = "l2.offHeap.allocation.slow";
  public static final String L2_OFFHEAP_ALLOCATION_CRITICAL                                 = "l2.offHeap.allocation.critical";
  public static final String L2_OFFHEAP_ALLOCATION_CRITICAL_HALT                            = "l2.offHeap.allocation.critical.halt";
  public static final String L2_ALLOCATION_DISABLE_PARTIAL_MAPS                             = "l2.offHeap.allocation.partial.disable.maps";
  public static final String L2_ALLOCATION_DISABLE_PARTIAL_OBJECTS                          = "l2.offHeap.allocation.partial.disable.objects";
  public static final String L2_ALLOCATION_ENABLE_OBJECTS_HOTSET                            = "l2.offHeap.allocation.partial.enable.object.hotset";
  public static final String L2_ALLOCATION_DISABLE_MAPS_HOTSET                              = "l2.offHeap.allocation.partial.disable.map.hotset";

  public static final String L2_OFFHEAP_DISABLED                                            = "l2.offheap.disable";

  public static final String L2_OFFHEAP_MIN_PAGE_SIZE                                       = "l2.offHeap.min.page.size";
  public static final String L2_OFFHEAP_MAX_PAGE_SIZE                                       = "l2.offHeap.max.page.size";
  public static final String L2_OFFHEAP_MAX_PAGE_COUNT                                      = "l2.offHeap.max.page.count";
  public static final String L2_OFFHEAP_MAP_TABLESIZE                                       = "l2.offHeap.map.tableSize";
  public static final String L2_OFFHEAP_MAP_CONCURRENCY                                     = "l2.offHeap.map.concurrency";

  // we calculate the right values. user can still override them explicitly
  public static final String L2_OFFHEAP_MAX_CHUNK_SIZE                                      = "l2.offHeap.max.chunk.size";
  public static final String L2_OFFHEAP_MIN_CHUNK_SIZE                                      = "l2.offHeap.min.chunk.size";
  public static final String L2_OFFHEAP_OBJECTDB_INITIAL_DATASIZE                           = "l2.offHeap.object.initialDataSize";
  public static final String L2_OFFHEAP_OBJECTDB_TABLESIZE                                  = "l2.offHeap.object.tableSize";
  public static final String L2_OFFHEAP_OBJECTDB_CONCURRENCY                                = "l2.offHeap.object.concurrency";

  // for tests
  public static final String L2_OFFHEAP_SKIP_JVMARG_CHECK                                   = "l2.offHeapCache.skip.jvmarg.check";

  public static final String L1_SEARCH_MAX_OPEN_RESULT_SETS                                 = "l1.search.max.open.resultSets";
  public static final String L2_SEARCH_MAX_PAGED_RESULT_SETS                                = "l2.search.max.paged.resultSets";
  public static final String L2_SEARCH_MAX_RESULT_PAGE_SIZE                                 = "l2.search.max.result.pageSize";

  public static final String SEARCH_QUERY_WAIT_FOR_TXNS                                     = "search.query.wait.for.txns";
  public static final String SEARCH_USE_COMMIT_THREAD                                       = "search.use.commit.thread";
  public static final String SEARCH_PASSIVE_MAX_CHUNK                                       = "search.passive.max.chunk";
  public static final String SEARCH_PASSIVE_MAX_PENDING                                     = "search.passive.max.pending";
  public static final String SEARCH_LUCENE_USE_RAM_DIRECTORY                                = "search.lucene.use.ram.directory";
  public static final String SEARCH_LUCENE_USE_OFFHEAP_DIRECTORY                            = "search.lucene.use.offHeap.directory";
  public static final String SEARCH_LUCENE_MAX_BUFFER                                       = "search.lucene.max.buffer";
  public static final String SEARCH_LUCENE_MAX_BOOLEAN_CLAUSES                              = "search.lucene.max.boolean.clauses";
  public static final String SEARCH_LUCENE_MERGE_FACTOR                                     = "search.lucene.mergefactor";
  public static final String SEARCH_LUCENE_MAX_MERGE_THREADS                                = "search.lucene.maxMergeThreads";
  public static final String SEARCH_LUCENE_MAX_BUFFERED_DOCS                                = "search.lucene.maxBufferedDocs";
  public static final String SEARCH_LUCENE_MAX_MERGE_DOCS                                   = "search.lucene.maxMergeDocs";
  public static final String SEARCH_LUCENE_INDEXES_PER_CACHE                                = "search.lucene.indexes.per.cache";
  public static final String SEARCH_LUCENE_DISABLE_FIELD_COMPRESSION                        = "search.lucene.disableStoredFieldCompression";

  public static final String APP_GROUPS_DEBUG                                               = "appgroups.debug";

  /*********************************************************************************************************************
   * <code>
   * Section :  OffHeap File System Settings
   * offHeapFilesystem.chm.segments         - Number of segments of the CHM representing the OffHeapFile. This parameter controls the maximum size of an OffHeapFile.
   *                                          Maximum Size = 2GB * Segments
   * offHeapFilesystem.file.blockSize       - Controls the granularity at which space is allocated to each file. File size will increase in multiples of blockSize (bytes).
   * offHeapFileSystem.file.maxDataPageSize - Controls the maximum page size used by the underlying CHM for allocating memory (bytes).
   * </code>
   ********************************************************************************************************************/
  public static final String FILESYSTEM_CHM_SEGMENTS                                        = "offHeapFilesystem.chm.segments";
  public static final String FILESYSTEM_BLOCK_SIZE                                          = "offHeapFilesystem.file.blockSize";
  public static final String FILESYSTEM_MAX_DATA_PAGE_SIZE                                  = "offHeapFileSystem.file.maxDataPageSize";

  /*********************************************************************************************************************
   * <code>
   * Section :  Server Event settings
   * </code>
   ********************************************************************************************************************/
  String                     L2_SERVER_EVENT_BATCHER_INTERVAL_MS                            = "l2.serverEvent.batcher.intervalInMillis";
  String                     L2_SERVER_EVENT_BATCHER_QUEUE_SIZE                             = "l2.serverEvent.batcher.queueSize";
  String                     L1_SERVER_EVENT_DELIVERY_THREADS                               = "l1.serverEvent.delivery.threads";
  String                     L1_SERVER_EVENT_DELIVERY_QUEUE_SIZE                            = "l1.serverEvent.delivery.queueSize";

  /*********************************************************************************************************************
   * <code>
   * Section :  BulkLoad Settings
   * toolkit.bulkload.logging               - Enable logging of Bulkload
   * toolkit.bulkload.minbatchbytesize      - Minimum batch size send to L2
   * toolkit.bulkload.throttle.timeInmillis - Time in millis used for throttling
   * toolkit.bulkload.throttle.threshold    - Maxmium size of buffer after which throttling will happen
   * </code>
   ********************************************************************************************************************/
  public static final String TOOLKIT_BULKLOAD_LOGGING_ENABLED                               = "toolkit.bulkload.logging";
  public static final String TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_BYTE_SIZE                      = "toolkit.bulkload.minbatchbytesize";
  public static final String TOOLKIT_LOCAL_BUFFER_PUTS_BATCH_TIME_MILLIS                    = "toolkit.bulkload.throttle.timeInmillis";
  public static final String TOOLKIT_LOCAL_BUFFER_PUTS_THROTTLE_BYTE_SIZE                   = "toolkit.bulkload.throttle.threshold";

  /*
   * For enabling CAS logging
   */
  public static final String CAS_LOGGING_ENABLED                                            = "cas.logging.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section :  Version Settings
   * version.compatibility.check - check version compatibility for client<->server and server<-> connections
   * </code>
   ********************************************************************************************************************/
  public static final String VERSION_COMPATIBILITY_CHECK                                    = "version.compatibility.check";

}
