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

  static final String[]      OLD_PROPERTIES                                                  = {
      "l1.reconnect.enabled", "l1.reconnect.timeout.millis", "l2.nha.ooo.maxDelayedAcks", "l2.nha.ooo.sendWindow",
      "l2.objectmanager.loadObjectID.checkpoint.changes", "l2.objectmanager.loadObjectID.checkpoint.timeperiod",
      "l2.nha.groupcomm.type", "l2.nha.tribes.failuredetector.millis", "l2.nha.tribes.orderinterceptor.enabled",
      "l2.nha.tribes.mcast.mcastPort", "l2.nha.tribes.mcast.mcastAddress", "l2.nha.tribes.mcast.memberDropTime",
      "l2.nha.tribes.mcast.mcastFrequency", "l2.nha.tribes.mcast.tcpListenPort", "l2.nha.tribes.mcast.tcpListenHost",
      "l2.nha.mcast.enabled", "l2.nha.tcgroupcomm.response.timelimit", "net.core.recv.buffer", "net.core.send.buffer",
      "l2.objectmanager.loadObjectID.measure.performance", "console.showObjectID", "l2.lfu.debug.enabled",
      "l1.serverarray.objectCreationStrategy.roundRobin.coordinatorLoad", "l2.objectmanager.loadObjectID.fastLoad" };

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Cache Manager Properties
   * Description : This section contains the defaults for the cache manager for the L2
   * enabled             : Enable/disable L2's cache manager
   * logging.enabled     : Enable/disable L2's cache manager logging
   * leastCount          : Minimum increase in the % usage of memory for starting eviction
   *                       once the threshold value specified of memory used is reached
   * percentageToEvict   : % of memory to evict once it reaches threshold
   * sleepInterval       : Initial sleep time between each cycles of memory usage analysis
   * criticalThreshold   : % of memory used after which memory manager will evict aggressively
   * threshold           : % of memory used after which eviction may start
   * monitorOldGenOnly   : Only monitor old gen objects
   * criticalObjectThreshold : Number of objects that the cache can hold after which the eviction
   *                           may start, its highly recommended to not to set it as the size
   *                           of the objects is not generally known
   * </code>
   ********************************************************************************************************************/
  public static final String L2_CACHEMANAGER_ENABLED                                         = "l2.cachemanager.enabled";
  public static final String L2_CACHEMANAGER_LOGGING_ENABLED                                 = "l2.cachemanager.logging.enabled";
  public static final String L2_CACHEMANAGER_LEASTCOUNT                                      = "l2.cachemanager.leastCount";
  public static final String L2_CACHEMANAGER_PERCENTAGETOEVICT                               = "l2.cachemanager.percentageToEvict";
  public static final String L2_CACHEMANAGER_SLEEPINTERVAL                                   = "l2.cachemanager.sleepInterval";
  public static final String L2_CACHEMANAGER_CRITICALTHRESHOLD                               = "l2.cachemanager.criticalThreshold";
  public static final String L2_CACHEMANAGER_THRESHOLD                                       = "l2.cachemanager.threshold";
  public static final String L2_CACHEMANAGER_MONITOROLDGENONLY                               = "l2.cachemanager.monitorOldGenOnly";
  public static final String L2_CACHEMANAGER_CRITICALOBJECTTHRESHOLD                         = "l2.cachemanager.criticalObjectThreshold";

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
   * </code>
   ********************************************************************************************************************/
  public static final String L2_TRANSACTIONMANAGER_LOGGING_ENABLED                           = "l2.transactionmanager.logging.enabled";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_VERBOSE                           = "l2.transactionmanager.logging.verbose";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINTSTATS                        = "l2.transactionmanager.logging.printStats";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINTCOMMITS                      = "l2.transactionmanager.logging.printCommits";
  public static final String L2_TRANSACTIONMANAGER_LOGGING_PRINT_BROADCAST_STATS             = "l2.transactionmanager.logging.printBroadcastStats";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_ENABLED                  = "l2.transactionmanager.passive.throttle.enabled";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_THRESHOLD                = "l2.transactionmanager.passive.throttle.threshold";
  public static final String L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_MAXSLEEPSECONDS          = "l2.transactionmanager.passive.throttle.maxSleepSeconds";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Object Manager Properties Description :
   * This section contains the defaults for the object manager of the L2
   * cachePolicy : <lru>/<lfu>      - Least Recently Used or Least Frequenctly used
   * deleteBatchSize                - Max number of objects deleted in one batch when
   *                                  removing from the object store after a GC
   * maxObjectsToCommit             - Max number of Objects commited in one batch in
   *                                  the commit stage and flush stage
   * maxObjectsInTxnObjGrouping     - Max number of Objects allowed in the TransactionalObject
   *                                  grouping
   * maxTxnsInTxnObjectGrouping     - Max number of Transactions allowed in the
   *                                  TransactionalObject grouping
   * objectrequest.split.size       - The maximum number of objects that l2 will lookup in one shot
   * objectrequest.logging.enabled  - Turn on logging to see what object request cache saved
   * fault.logging.enabled          - Enables/Disables logging of ManagedObject Faults from
   *                                  disk. If enabled, it logs every 1000 faults.
   * request.logging.enabled        - Enables/Disables logging of ManagedObject requests from
   *                                  clients. If enabled, logs counts of requested instance types
   *                                  every 5 seconds.
   * flush.logging.enabled          - Enables/Disables logging of ManagedObject flush to
   *                                  disk. If enabled, it logs every 1000 faults.
   * persistor.logging.enabled      - Enables/Disables logging of commits to disk while running
   *                                  in persistent mode.
   * loadObjectID.longsPerDiskEntry - Size of long array entry to store object IDs
   *                                  in persistent store. One bit for each ID.
   * loadObjectID.mapsdatabase.longsPerDiskEntry - Size of long array entry to store existence of
   *                                  persistent state. One bit for each ID.
   * loadObjectID.checkpoint.maxlimit - Max number of changes to process in one run checkpoint.
   * loadObjectID.checkpoint.maxsleep - Max sleep time in milliseconds between checkpoints
   * passive.sync.batch.size        - Number of objects in each message that is sent from
   *                                  active to passive while synching
   * passive.sync.throttle.timeInMillis - Time to wait before sending the next batch of
   *                                  objects to the passive
   * dgc.throttle.timeInMillis     - Throttle time for dgc for each cycle for every requestsPerThrottle
   *                                 requests for references from object manager
   * dgc.throttle.requestsPerThrottle - Number of objects for which object references are requested
   *                                 from object manager after which dgc will throttle
   * dgc.faulting.optimization      - This property will not fault in objects that has no references during DGC mark stage
   *                                   0 - disable faulting optimization
   *                                   1 - enabled with standard implementation (continous oids)
   *                                   2 - enabled with compressed implementation (spare oids)
   * dgc.enterpriseMarkStageInterval - The time between tokens are sent to see if each L2 finished marking (collect/rescue).
   *                                   This is for enterprise only.
   * dgc.young.enabled              - Enables/Disables the young gen collector
   * dgc.young.frequencyInMillis    - The time in millis between each young gen collection.
   *                                  (default : 1 min, not advisable to run more frequently)
   * l2.data.backup.throttle.timeInMillis - time to sleep between copying of each file from the db while taking backup
   * </code>
   ********************************************************************************************************************/

  public static final String L2_OBJECTMANAGER_DELETEBATCHSIZE                                = "l2.objectmanager.deleteBatchSize";
  public static final String L2_OBJECTMANAGER_CACHEPOLICY                                    = "l2.objectmanager.cachePolicy";
  public static final String L2_OBJECTMANAGER_MAXOBJECTS_TO_COMMIT                           = "l2.objectmanager.maxObjectsToCommit";
  public static final String L2_OBJECTMANAGER_MAXOBJECTS_INTXNOBJ_GROUPING                   = "l2.objectmanager.maxObjectsInTxnObjGrouping";
  public static final String L2_OBJECTMANAGER_MAXTXNS_INTXNOBJECT_GROUPING                   = "l2.objectmanager.maxTxnsInTxnObjectGrouping";
  public static final String L2_OBJECTMANAGER_OBJECT_REQUEST_SPLIT_SIZE                      = "l2.objectmanager.objectrequest.split.size";
  public static final String L2_OBJECTMANAGER_OBJECT_REQUEST_LOGGING_ENABLED                 = "l2.objectmanager.objectrequest.logging.enabled";
  public static final String L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED                          = "l2.objectmanager.fault.logging.enabled";
  public static final String L2_OBJECTMANAGER_REQUEST_LOGGING_ENABLED                        = "l2.objectmanager.request.logging.enabled";
  public static final String L2_OBJECTMANAGER_FLUSH_LOGGING_ENABLED                          = "l2.objectmanager.flush.logging.enabled";
  public static final String L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED                      = "l2.objectmanager.persistor.logging.enabled";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_LONGS_PERDISKENTRY                = "l2.objectmanager.loadObjectID.longsPerDiskEntry";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_CHANGES                = "l2.objectmanager.loadObjectID.checkpoint.changes";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXLIMIT               = "l2.objectmanager.loadObjectID.checkpoint.maxlimit";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_TIMEPERIOD             = "l2.objectmanager.loadObjectID.checkpoint.timeperiod";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE                        = "l2.objectmanager.passive.sync.batch.size";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_MESSAGE_MAXSIZE_MB                = "l2.objectmanager.passive.sync.message.maxSizeInMegaBytes";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_TIME                     = "l2.objectmanager.passive.sync.throttle.timeInMillis";
  public static final String L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_PENDING_MSGS             = "l2.objectmanager.passive.sync.throttle.maxPendingMessages";
  public static final String L2_OBJECTMANAGER_DGC_THROTTLE_TIME                              = "l2.objectmanager.dgc.throttle.timeInMillis";
  public static final String L2_OBJECTMANAGER_DGC_REQUEST_PER_THROTTLE                       = "l2.objectmanager.dgc.throttle.requestsPerThrottle";
  public static final String L2_OBJECTMANAGER_DGC_FAULTING_OPTIMIZATION                      = "l2.objectmanager.dgc.faulting.optimization";
  public static final String L2_OBJECTMANAGER_DGC_YOUNG_ENABLED                              = "l2.objectmanager.dgc.young.enabled";
  public static final String L2_OBJECTMANAGER_DGC_YOUNG_FREQUENCY                            = "l2.objectmanager.dgc.young.frequencyInMillis";
  public static final String L2_OBJECTMANAGER_DGC_ENTERPRISE_MARK_STAGE_INTERVAL             = "l2.objectmanager.dgc.enterpriseMarkStageInterval";
  public static final String L2_DATA_BACKUP_THROTTLE_TIME                                    = "l2.data.backup.throttle.timeInMillis";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_MAPDB_LONGS_PERDISKENTRY          = "l2.objectmanager.loadObjectID.mapsdatabase.longsPerDiskEntry";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_MEASURE_PERF                      = "l2.objectmanager.loadObjectID.measure.performance";
  public static final String L2_OBJECTMANAGER_LOADOBJECTID_CHECKPOINT_MAXSLEEP               = "l2.objectmanager.loadObjectID.checkpoint.maxsleep";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Seda stage properties
   * Description : This section contains configuration for SEDA stages for L2
   * commitstage.threads                : Number of seda commit stage thread
   * faultstage.threads                 : Number of seda fault stage thread
   * managedobjectrequeststage.threads  : Number of threads for object request seda stage
   *                                      (experimental, do not change)
   * managedobjectresponsestage.threads : Number of threads for object response seda stage
   * flushstage.threads                 : Number of threads for flusing of objects to disk
   *                                      seda stage
   * gcdeletestage.threads              : Number of threads for deletetion of objects after
   *                                      dgc finishes marking
   * stage.sink.capacity                : Capacity of seda stage queue, Integer.MAX_VALUE if not set
   *                                      (experimental, do not change)
   * </code>
   ********************************************************************************************************************/
  public static final String L2_SEDA_COMMITSTAGE                                             = "l2.seda.commitstage.threads";
  public static final String L2_SEDA_FAULTSTAGE_THREADS                                      = "l2.seda.faultstage.threads";
  public static final String L2_SEDA_FLUSHSTAGE_THREADS                                      = "l2.seda.flushstage.threads";
  public static final String L2_SEDA_GCDELETESTAGE_THREADS                                   = "l2.seda.gcdeletestage.threads";
  public static final String L2_SEDA_MANAGEDOBJECTRESPONSESTAGE_THREADS                      = "l2.seda.managedobjectresponsestage.threads";
  public static final String L2_SEDA_MANAGEDOBJECTREQUESTSTAGE_THREADS                       = "l2.seda.managedobjectrequeststage.threads";
  public static final String L2_SEDA_STAGE_SINK_CAPACITY                                     = "l2.seda.stage.sink.capacity";
  public static final String L2_SEDA_EVICTION_PROCESSORSTAGE_SINK_SIZE                       = "l2.seda.evictionprocessorstage.sink.capacity";
  public static final String L2_SEDA_SEARCH_THREADS                                          = "l2.seda.search.threads";
  public static final String L2_SEDA_QUERY_THREADS                                           = "l2.seda.query.threads";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Seda stage properties
   * Description : This section contains configuration for SEDA stages for L1
   * stage.sink.capacity  : Capacity of L1's seda stage queue, Integer.MAX_VALUE if not set
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SEDA_STAGE_SINK_CAPACITY                                     = "l1.seda.stage.sink.capacity";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Berkeley DB Persistence Layer Properties
   * Description : This section contains the of Berkeley DB JE properties thats used in L2
   * For an explanation of these properties look at Berkeley DB documentation
   * (l2.berkeleydb is removed before giving to Berkeley DB JE)
   * </code>
   ********************************************************************************************************************/
  public static final String L2_BERKELEYDB_JE_LOCK_TIMEOUT                                   = "l2.berkeleydb.je.lock.timeout";
  public static final String L2_BERKELEYDB_JE_MAXMEMORYPERCENT                               = "l2.berkeleydb.je.maxMemoryPercent";
  public static final String L2_BERKELEYDB_JE_LOCK_NLOCK_TABLES                              = "l2.berkeleydb.je.lock.nLockTables";
  public static final String L2_BERKELEYDB_JE_CLEANER_BYTES_INTERVAL                         = "l2.berkeleydb.je.cleaner.bytesInterval";
  public static final String L2_BERKELEYDB_JE_CHECKPOINTER_BYTESINTERVAL                     = "l2.berkeleydb.je.checkpointer.bytesInterval";
  public static final String L2_BERKELEYDB_JE_CLEANER_DETAIL_MAXMEMORY_PERCENTAGE            = "l2.berkeleydb.je.cleaner.detailMaxMemoryPercentage";
  public static final String L2_BERKELEYDB_JE_CLEANER_LOOKAHEAD_CACHESIZE                    = "l2.berkeleydb.je.cleaner.lookAheadCacheSize";
  public static final String L2_BERKELEYDB_JE_CLEANER_MINAGE                                 = "l2.berkeleydb.je.cleaner.minAge";
  public static final String L2_DB_FACTORY_NAME                                              = "l2.db.factory.name";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Derby DB Persistence Layer Properties
   * Description : Section for properties related to tuning Derby. See description of tunable properties in
   * the Derby documentation.
   * pageCache.heapUsage (float) - Controls percentage of the heap to be given to Derby's page cache.
   * </code>
   ********************************************************************************************************************/
  public static final String DERBY_STORAGE_PAGESIZE                                          = "derby.storage.pageSize";
  public static final String DERBY_STORAGE_PAGECACHESIZE                                     = "derby.storage.pageCacheSize";
  public static final String DERBY_SYSTEM_DURABILITY                                         = "derby.system.durability";
  public static final String DERBY_STREAM_ERROR_METHOD                                       = "derby.stream.error.method";
  public static final String DERBY_MAXMEMORYPERCENT                                          = "derby.maxMemoryPercent";
  public static final String DERBY_LOG_BUFFER_SIZE                                           = "derby.storage.logBufferSize";
  public static final String DERBY_LOG_DEVICE                                                = "logDevice";
  public static final String L2_DERBYDB_DERBY_STORAGE_PAGESIZE                               = "l2.derbydb."
                                                                                               + DERBY_STORAGE_PAGESIZE;
  public static final String L2_DERBYDB_DERBY_STORAGE_PAGECACHESIZE                          = "l2.derbydb."
                                                                                               + DERBY_STORAGE_PAGECACHESIZE;
  public static final String L2_DERBYDB_DERBY_SYSTEM_DURABILITY                              = "l2.derbydb."
                                                                                               + DERBY_SYSTEM_DURABILITY;
  public static final String L2_DERBYDB_MAXMEMORYPERCENT                                     = "l2.derbydb."
                                                                                               + DERBY_MAXMEMORYPERCENT;
  public static final String L2_DERBYDB_LOG_DEVICE                                           = "l2.derbydb."
                                                                                               + DERBY_LOG_DEVICE;
  public static final String L2_DERBYDB_DERBY_STORAGE_CHECKPOINTINTERVAL                     = "l2.derbydb.derby.storage.checkpointInterval";
  public static final String L2_DERBYDB_DERBY_STORAGE_LOG_SWITCH_INTERVAL                    = "l2.derbydb.derby.storage.logSwitchInterval";
  public static final String L2_DERBYDB_DERBY_STORAGE_LOG_BUFFER_SIZE                        = "l2.derbydb."
                                                                                               + DERBY_LOG_BUFFER_SIZE;
  public static final String L2_DERBYDB_DERBY_LOCK_ESCALATION_THRESHOLD                      = "l2.derbydb.derby.locks.escalationThreshold";
  public static final String L2_DERBYDB_DERBY_LOCKS_DEADLOCK_TIMEOUT                         = "l2.derbydb.derby.locks.deadlockTimeout";
  public static final String L2_DERBYDB_DERBY_LOCKS_WAIT_TIMEOUT                             = "l2.derbydb.derby.locks.waitTimeout";
  public static final String L2_DERBYDB_DERBY_LOCKS_DEADLOCK_TRACE                           = "l2.derbydb.derby.locks.deadlockTrace";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 LFU cachepolicy defaults
   * Description : If cachePolicy is set to lfu, then these values take effect
   * agingFactor (float)                    - valid values 0 to 1
   * recentlyAccessedIgnorePercentage (int) - valid values 0 - 100
   * </code>
   ********************************************************************************************************************/
  public static final String L2_LFU_AGINGFACTOR                                              = "l2.lfu.agingFactor";
  public static final String L2_LFU_RECENTLY_ACCESSED_IGNORE_PERCENTAGE                      = "l2.lfu.recentlyAccessedIgnorePercentage";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Bean shell Properties
   * Description : Bean shell can be enabled in the server for debugging.
   * enabled     : Enables/disables Beanshell
   * port        : Port number for Beanshell
   * </code>
   ********************************************************************************************************************/
  public static final String L2_BEANSHELL_ENABLED                                            = "l2.beanshell.enabled";
  public static final String L2_BEANSHELL_PORT                                               = "l2.beanshell.port";

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
   * </code>
   ********************************************************************************************************************/
  public static final String L2_NHA_TCGROUPCOMM_HANDSHAKE_TIMEOUT                            = "l2.nha.tcgroupcomm.handshake.timeout";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_ENABLED                            = "l2.nha.tcgroupcomm.reconnect.enabled";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_TIMEOUT                            = "l2.nha.tcgroupcomm.reconnect.timeout";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_SENDQUEUE_CAP                      = "l2.nha.tcgroupcomm.reconnect.sendqueue.cap";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_MAX_DELAYEDACKS                    = "l2.nha.tcgroupcomm.reconnect.maxDelayedAcks";
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_SEND_WINDOW                        = "l2.nha.tcgroupcomm.reconnect.sendWindow";
  public static final String L2_NHA_TCGROUPCOMM_DISCOVERY_INTERVAL                           = "l2.nha.tcgroupcomm.discovery.interval";
  // a hidden tc.properties only used for l2 proxy testing purpose
  public static final String L2_NHA_TCGROUPCOMM_RECONNECT_L2PROXY_TO_PORT                    = "l2.nha.tcgroupcomm.l2proxytoport";
  public static final String L2_NHA_SEND_TIMEOUT_MILLS                                       = "l2.nha.send.timeout.millis";
  public static final String L2_NHA_DIRTYDB_AUTODELETE                                       = "l2.nha.dirtydb.autoDelete";
  public static final String L2_NHA_DIRTYDB_ROLLING                                          = "l2.nha.dirtydb.rolling";
  public static final String L2_NHA_AUTORESTART                                              = "l2.nha.autoRestart";

  /*********************************************************************************************************************
   * <code>
   * Section : Misc L2 Properties
   * Description : Other Miscellaneous L2 Properties
   * startuplock.retries.enabled  : If true then L2s will try to lock indefinitely on the data
   *                                directory while starting up
   * </code>
   ********************************************************************************************************************/
  public static final String L2_STARTUPLOCK_RETRIES_ENABLED                                  = "l2.startuplock.retries.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Server Array Properties
   * serverarray.2pc.enabled  :Enables/disables 2 phase commit for enterprise server array
   *                           (experimental, do not change)
   * </code>
   ********************************************************************************************************************/
  public static final String L2_SERVERARRAY_2PC_ENABLED                                      = "l2.serverarray.2pc.enabled";

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
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY                           = "l1.serverarray.objectCreationStrategy";
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY_GROUPAFFINITY_GROUPNAME   = "l1.serverarray.objectCreationStrategy.groupAffinity.groupName";
  public static final String L1_SERVERARRAY_OBJECTCREATIONSTRATEGY_ROUND_ROBIN_START_INDEX   = "l1.serverarray.objectCreationStrategy.roundRobin.startIndex";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 JVM Compatibility Properties
   * Description : This section contains the defaults for the JVM compatibility for the L1
   * jvm.check.compatibility : Makes sure that the boot jar with which L1 is running matches
   *                           the current VM version running the L1 application
   * </code>
   ********************************************************************************************************************/
  public static final String L1_JVM_CHECK_COMPATIBILITY                                      = "l1.jvm.check.compatibility";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 L2 Config match Property
   * Description : This property will check if the client has to match server config i.e. check cluster topology
   * </code>
   ********************************************************************************************************************/
  public static final String L1_L2_CONFIG_VALIDATION_ENABLED                                 = "l1.l2.config.validation.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Integration Modules
   * Description : This section contains the defaults for the L1 integration modules
   * repositories                 - Comma-separated list of additional module repositories URL's; if the tc.install-root system property is set, a default
   *                                repository of (tc.install-root)/modules will be injected default - comma-separated list of integration modules that are
   *                                implicitly loaded by the L1 in the form specified by the Required-Bundles OSGI
   * default                      - Comma-separated list of integration modules that are implicitly
   *                                loaded by the L1 in the form specified by the Required-Bundles
   *                                OSGI manifest header
   * manifest header additional   - List of additional integration modules to be started, in the form specified by the OSGI Required-Bundles manifest header
   * tc-version-check             - Off|warn|enforce|strict
   * </code>
   ********************************************************************************************************************/
  public static final String L1_MODULES_REPOSITORIES                                         = "l1.modules.repositories";
  public static final String L1_MODULES_DEFAULT                                              = "l1.modules.default";
  public static final String L1_MODULES_ADDITIONAL                                           = "l1.modules.additional";
  public static final String L1_MODULES_TC_VERSION_CHECK                                     = "l1.modules.tc-version-check";
  public static final String L1_MODULES_TOOLKIT_SEARCH_RANGE                                 = "l1.modules.toolkitSearchRange";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 Cache Manager Properties
   * Description : This section contains the defaults for the cache manager for the L1
   * enabled             : Enable/disable L2's cache manager
   * logging.enabled     : Enable/disable L2's cache manager logging
   * leastCount          : Minimum increase in the % usage of memory for starting eviction
   *                       once the threshold value specified of memory used is reached
   * percentageToEvict   : % of memory to evict once it reaches threshold
   * sleepInterval       : Initial sleep time between each cycles of memory usage analysis
   * criticalThreshold   : % of memory used after which memory manager will evict aggressively
   * threshold           : % of memory used after which eviction may start
   * monitorOldGenOnly   : Only monitor old gen objects
   * criticalObjectThreshold : Number of objects that the cache can hold after which the eviction
   *                           may start, its highly recommended to not to set it as the size
   *                           of the objects is not generally known
   * </code>
   ********************************************************************************************************************/
  public static final String L1_CACHEMANAGER_ENABLED                                         = "l1.cachemanager.enabled";
  public static final String L1_CACHEMANAGER_LOGGING_ENABLED                                 = "l1.cachemanager.logging.enabled";
  public static final String L1_CACHEMANAGER_LEASTCOUNT                                      = "l1.cachemanager.leastCount";
  public static final String L1_CACHEMANAGER_PERCENTAGE_TOEVICT                              = "l1.cachemanager.percentageToEvict";
  public static final String L1_CACHEMANAGER_SLEEPINTERVAL                                   = "l1.cachemanager.sleepInterval";
  public static final String L1_CACHEMANAGER_CRITICAL_THRESHOLD                              = "l1.cachemanager.criticalThreshold";
  public static final String L1_CACHEMANAGER_THRESHOLD                                       = "l1.cachemanager.threshold";
  public static final String L1_CACHEMANAGER_MONITOR_OLDGENONLY                              = "l1.cachemanager.monitorOldGenOnly";
  public static final String L1_CACHEMANAGER_CRITICAL_OBJECT_THRESHOLD                       = "l1.cachemanager.criticalObjectThreshold";

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
  public static final String L1_TRANSACTIONMANAGER_LOGGING_ENABLED                           = "l1.transactionmanager.logging.enabled";
  public static final String L1_TRANSACTIONMANAGER_MAXOUTSTANDING_BATCHSIZE                  = "l1.transactionmanager.maxOutstandingBatchSize";
  public static final String L1_TRANSACTIONMANAGER_MAXBATCHSIZE_INKILOBYTES                  = "l1.transactionmanager.maxBatchSizeInKiloBytes";
  public static final String L1_TRANSACTIONMANAGER_MAXPENDING_BATCHES                        = "l1.transactionmanager.maxPendingBatches";
  public static final String L1_TRANSACTIONMANAGER_MAXSLEEPTIME_BEFOREHALT                   = "l1.transactionmanager.maxSleepTimeBeforeHalt";
  public static final String L1_TRANSACTIONMANAGER_COMPLETED_ACK_FLUSH_TIMEOUT               = "l1.transactionmanager.completedAckFlushTimeout";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_ENABLED                  = "l1.transactionmanager.strings.compress.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_LOGGING_ENABLED          = "l1.transactionmanager.strings.compress.logging.enabled";
  public static final String L1_TRANSACTIONMANAGER_STRINGS_COMPRESS_MINSIZE                  = "l1.transactionmanager.strings.compress.minSize";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_ENABLED                           = "l1.transactionmanager.folding.enabled";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_OBJECT_LIMIT                      = "l1.transactionmanager.folding.object.limit";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_LOCK_LIMIT                        = "l1.transactionmanager.folding.lock.limit";
  public static final String L1_TRANSACTIONMANAGER_FOLDING_DEBUG                             = "l1.transactionmanager.folding.debug";
  public static final String L1_TRANSACTIONMANAGER_TIMEOUTFORACK_ONEXIT                      = "l1.transactionmanager.timeoutForAckOnExit";

  public static final String TC_TRANSPORT_HANDSHAKE_TIMEOUT                                  = "tc.transport.handshake.timeout";
  public static final String TC_CONFIG_SOURCEGET_TIMEOUT                                     = "tc.config.getFromSource.timeout";

  /*********************************************************************************************************************
   * <code>
   * Section: L1 Connect Properties
   * Description: This section contains properties controlling L1 connect feature
   * max.connect.retries               - Maximum L2 connection attempts
   * connect.versionMatchCheck.enabled - If true, connection is established only when
   *                                     L1 and L2 are of the same DSO version
   * socket.connect.timeout            - Socket timeout (ms) when connecting to server
   * reconnect.waitInterval            - Sleep time (ms) between trying connections to the server
   *                                     (values less than 10ms will be set to 10ms)
   * </code>
   ********************************************************************************************************************/
  public static final String L1_MAX_CONNECT_RETRIES                                          = "l1.max.connect.retries";
  public static final String L1_CONNECT_VERSION_MATCH_CHECK                                  = "l1.connect.versionMatchCheck.enabled";
  public static final String L1_SOCKET_CONNECT_TIMEOUT                                       = "l1.socket.connect.timeout";
  public static final String L1_SOCKET_RECONNECT_WAIT_INTERVAL                               = "l1.socket.reconnect.waitInterval";

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
   * </code>
   ********************************************************************************************************************/
  public static final String L2_L1RECONNECT_ENABLED                                          = "l2.l1reconnect.enabled";
  public static final String L2_L1RECONNECT_TIMEOUT_MILLS                                    = "l2.l1reconnect.timeout.millis";
  public static final String L2_L1RECONNECT_SENDQUEUE_CAP                                    = "l2.l1reconnect.sendqueue.cap";
  public static final String L2_L1RECONNECT_MAX_DELAYEDACKS                                  = "l2.l1reconnect.maxDelayedAcks";
  public static final String L2_L1RECONNECT_SEND_WINDOW                                      = "l2.l1reconnect.sendWindow";

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
   * </code>
   ********************************************************************************************************************/
  public static final String L1_OBJECTMANAGER_REMOTE_MAX_DNALRU_SIZE                         = "l1.objectmanager.remote.maxDNALRUSize";
  public static final String L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED                         = "l1.objectmanager.remote.logging.enabled";
  public static final String L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY            = "l1.objectmanager.remote.maxRequestSentImmediately";
  public static final String L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD                = "l1.objectmanager.remote.batchLookupTimePeriod";
  public static final String L1_OBJECTMANAGER_OBJECTID_REQUEST_SIZE                          = "l1.objectmanager.objectid.request.size";
  public static final String L1_OBJECTMANAGER_FLUSH_LOGGING_ENABLED                          = "l1.objectmanager.flush.logging.enabled";
  public static final String L1_OBJECTMANAGER_FAULT_LOGGING_ENABLED                          = "l1.objectmanager.fault.logging.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : L1 ServerMap Manager Properties
   * Description : This section contains the defaults for the ServerMap manager for the L1
   * remote.maxRequestSentImmediately
   *                         : Maximum number of requests send immediately after which it will be batched
   * remote.batchLookupTimePeriod
   *                         : Time to wait before sending batch requests
   * 
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SERVERMAPMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY         = "l1.servermapmanager.remote.maxRequestSentImmediately";
  public static final String L1_SERVERMAPMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD             = "l1.servermapmanager.remote.batchLookupTimePeriod";

  /*********************************************************************************************************************
   * <code>
   * Section : Common Logging properties for both L1 and L2
   * Description : Logging attributes that can be overridden.
   * maxBackups       - The maximum number of backup log files to keep maxLogFileSize - The maximum size of a log file in megabytes
   * longgc.threshold - DGC taking greater than the time mentioned will be logged
   * </code>
   ********************************************************************************************************************/
  public static final String LOGGING_MAXBACKUPS                                              = "logging.maxBackups";
  public static final String LOGGING_MAX_LOGFILE_SIZE                                        = "logging.maxLogFileSize";
  public static final String LOGGING_LONG_GC_THRESHOLD                                       = "logging.longgc.threshold";

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
  public static final String TC_STAGE_MONITOR_ENABLED                                        = "tc.stage.monitor.enabled";
  public static final String TC_STAGE_MONITOR_DELAY                                          = "tc.stage.monitor.delay";
  public static final String TC_BYTEBUFFER_POOLING_ENABLED                                   = "tc.bytebuffer.pooling.enabled";
  public static final String TC_BYTEBUFFER_COMMON_POOL_MAXCOUNT                              = "tc.bytebuffer.common.pool.maxcount";
  public static final String TC_BYTEBUFFER_THREADLOCAL_POOL_MAXCOUNT                         = "tc.bytebuffer.threadlocal.pool.maxcount";
  public static final String TC_MESSAGE_GROUPING_ENABLED                                     = "tc.messages.grouping.enabled";
  public static final String TC_MESSAGE_GROUPING_MAXSIZE_KB                                  = "tc.messages.grouping.maxSizeKiloBytes";
  public static final String TC_MESSAGE_PACKUP_ENABLED                                       = "tc.messages.packup.enabled";

  /*********************************************************************************************************************
   * <code>
   * Section : Common property for TC Management MBean
   * Description : TC Management MBeans can be enabled/disabled
   * mbeans.enabled : <true/false> - All mbeans enabled/disabled test.mbeans.enabled : <true/false> - Test mode mbeans
   * enabled/disabled
   * </code>
   ********************************************************************************************************************/
  public static final String TC_MANAGEMENT_MBEANS_ENABLED                                    = "tc.management.mbeans.enabled";
  public static final String TC_MANAGEMENT_TEST_MBEANS_ENABLED                               = "tc.management.test.mbeans.enabled";

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
   * debug.sessions                  : output additional debug information when sessions are looked up, created, etc
   * verify.set.attribute            : Snapshot attributes on access/mutation and report if the attribute has changed at the end of the request
   * </code>
   ********************************************************************************************************************/
  public static final String SESSION_INVALIDATOR_SLEEP                                       = "session.invalidator.sleep";
  public static final String SESSION_INVALIDATOR_BENCH_ENABLED                               = "session.invalidator.bench.enabled";
  public static final String SESSION_REQUEST_BENCH_ENABLED                                   = "session.request.bench.enabled";
  public static final String SESSION_REQUEST_TRACKING                                        = "session.request.tracking";
  public static final String SESSION_REQUEST_TRACKING_DUMP                                   = "session.request.tracking.dump";
  public static final String SESSION_REQUEST_TRACKING_INTERVAL                               = "session.request.tracking.interval";
  public static final String SESSION_REQUEST_TRACKING_THRESHOLD                              = "session.request.tracking.threshold";
  public static final String SESSION_DEBUG_HOPS                                              = "session.debug.hops";
  public static final String SESSION_DEBUG_HOPS_INTERVAL                                     = "session.debug.hops.interval";
  public static final String SESSION_DEBUG_INVALIDATE                                        = "session.debug.invalidate";
  public static final String SESSION_DEBUG_SESSIONS                                          = "session.debug.sessions";
  public static final String SESSION_VHOSTS_EXCLUDED                                         = "session.vhosts.excluded";
  public static final String SESSION_STATISTICS_ENABLED                                      = "session.statistics.enabled";
  public static final String SESSION_VERIFY_SET_ATTRIBUTE                                    = "session.verify.set.attribute";

  /*********************************************************************************************************************
   * <code>
   * Section : Memory Monitor
   * forcebasic : enable/disable only basic memory monitoring
   * </code>
   ********************************************************************************************************************/
  public static final String MEMORY_MONITOR_FORCEBASIC                                       = "memory.monitor.forcebasic";

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
   * 
   * </code>
   ********************************************************************************************************************/
  public static final String EHCACHE_CLUSTER_ALL_CACHE_MANAGERS                              = "ehcache.clusterAllCacheManagers";
  public static final String EHCACHE_LOGGING_ENABLED                                         = "ehcache.logging.enabled";
  public static final String EHCACHE_EVICTOR_LOGGING_ENABLED                                 = "ehcache.evictor.logging.enabled";
  public static final String EHCACHE_EVICTOR_POOL_SIZE                                       = "ehcache.evictor.pool.size";
  public static final String EHCACHE_CONCURRENCY                                             = "ehcache.concurrency";
  public static final String EHCACHE_GLOBAL_EVICTION_ENABLE                                  = "ehcache.global.eviction.enable";
  public static final String EHCACHE_GLOBAL_EVICTION_FREQUENCY                               = "ehcache.global.eviction.frequency";
  public static final String EHCACHE_GLOBAL_EVICTION_SEGMENTS                                = "ehcache.global.eviction.segments";
  public static final String EHCACHE_GLOBAL_EVICTION_REST_TIMEMILLS                          = "ehcache.global.eviction.rest.timeMillis";
  public static final String EHCACHE_LOCK_READLEVEL                                          = "ehcache.lock.readLevel";
  public static final String EHCACHE_LOCK_WRITELEVEL                                         = "ehcache.lock.writeLevel";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_ENABLED                 = "ehcache.storageStrategy.dcv2.localcache.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_LOCALCACHE_INCOHERENT_READ_TIMEOUT = "ehcache.storageStrategy.dcv2.localcache.incoherentReadTimeout";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_PERELEMENT_TTI_TTL_ENABLED         = "ehcache.storageStrategy.dcv2.perElementTTITTL.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_EVICT_UNEXPIRED_ENTRIES_ENABLED    = "ehcache.storageStrategy.dcv2.evictUnexpiredEntries.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_PERIODICEVICTION_ENABLED           = "ehcache.storageStrategy.dcv2.periodicEviction.enabled";
  public static final String EHCACHE_STORAGESTRATEGY_DCV2_PINSEGMENTS_ENABLED                = "ehcache.storageStrategy.dcv2.pinSegments.enabled";
  public static final String   EHCACHE_STORAGESTRATEGY_DCV2_EVICTION_OVERSHOOT                 = "ehcache.storageStrategy.dcv2.eviction.overshoot";
  /*********************************************************************************************************************
   * <code>
   * Section : L1 Lock Manager Properties
   * Description       : This section contains the defaults for the client lock manager for the L1
   * striped.count     : striping count for l1 lock manager
   * timeout.interval  : time after which an unused lock will be a candidate for lock GC
   * </code>
   ********************************************************************************************************************/
  public static final String L1_LOCKMANAGER_STRIPED_COUNT                                    = "l1.lockmanager.striped.count";
  public static final String L1_LOCKMANAGER_TIMEOUT_INTERVAL                                 = "l1.lockmanager.timeout.interval";

  /*********************************************************************************************************************
   * <code>
   * Section : Lock statistics
   * lock.statistics.enabled            : Enables/disables lock statistics
   * l1.lock.statistics.traceDepth      : Depth of locks given to L1s for gathering the statistics
   * l1.lock.statistics.gatherInterval  : Poll interval for gathering lock statistics
   * </code>
   ********************************************************************************************************************/
  public static final String LOCK_STATISTICS_ENABLED                                         = "lock.statistics.enabled";
  public static final String L1_LOCK_STATISTICS_TRACEDEPTH                                   = "l1.lock.statistics.traceDepth";
  public static final String L1_LOCK_STATISTICS_GATHERINTERVAL                               = "l1.lock.statistics.gatherInterval";

  /*********************************************************************************************************************
   * <code>
   * Section : L2 Lock Manager
   * enabled            : Enable/disable greedy locks grant from L2
   * leaseTimeInMillis  : Time for which greedy locks are given to L1 if more than one of them
   *                      are contending for them
   * </code>
   ********************************************************************************************************************/
  public static final String L2_LOCKMANAGER_GREEDY_LOCKS_ENABLED                             = "l2.lockmanager.greedy.locks.enabled";
  public static final String L2_LOCKMANAGER_GREEDY_LEASE_ENABLED                             = "l2.lockmanager.greedy.lease.enabled";
  public static final String L2_LOCKMANAGER_GREEDY_LEASE_LEASETIME_INMILLS                   = "l2.lockmanager.greedy.lease.leaseTimeInMillis";

  /*********************************************************************************************************************
   * <code>
   * Section : TCP Settings
   * tcpnodelay : Enable/disable tcp packet batching
   * keepalive  : Enable/disable tcp probe for running/broken connections
   * </code>
   ********************************************************************************************************************/
  public static final String NET_CORE_KEEPALIVE                                              = "net.core.keepalive";
  public static final String NET_CORE_TCP_NO_DELAY                                           = "net.core.tcpnodelay";

  /*********************************************************************************************************************
   * <code> Section :
   * CVT cvt.retriever.notification.interval  - Interval between log file messages when the CVT
   *                                            retriever is running (in seconds)
   * cvt.statistics.logging.interval          - Interval between logging of statistics data (in seconds).
   * cvt.buffer.randomsuffix.enabled          - If true, add a random suffix when a buffer is created
   * cvt.store.randomsuffix.enabled           - If true, add a random suffix when a store is created
   * cvt.rest.interface.enabled               - If false, the REST interface for the CVT will be disabled. True by default
   * cvt.client.fail.buffer.open              - If true, always fail the open of the CVT statistics buffer on a client. This is
   *                                            supposed to be used by tests. False by default
   * </code>
   ********************************************************************************************************************/
  public static final String CVT_RETRIEVER_NOTIFICATION_INTERVAL                             = "cvt.retriever.notification.interval";
  public static final String CVT_STATISTICS_LOGGING_INTERVAL                                 = "cvt.statistics.logging.interval";
  public static final String CVT_BUFFER_RANDOM_SUFFIX_ENABLED                                = "cvt.buffer.randomsuffix.enabled";
  public static final String CVT_STORE_RANDOM_SUFFIX_ENABLED                                 = "cvt.store.randomsuffix.enabled";
  public static final String CVT_REST_INTERFACE_ENABLED                                      = "cvt.rest.interface.enabled";
  public static final String CVT_CLIENT_FAIL_BUFFER_OPEN                                     = "cvt.client.fail.buffer.open";

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
  public static final String L2_HEALTHCHECK_L1_PING_ENABLED                                  = "l2.healthcheck.l1.ping.enabled";
  public static final String L2_HEALTHCHECK_L1_PING_IDLETIME                                 = "l2.healthcheck.l1.ping.idletime";
  public static final String L2_HEALTHCHECK_L1_PING_INTERVAL                                 = "l2.healthcheck.l1.ping.interval";
  public static final String L2_HEALTHCHECK_L1_PING_PROBES                                   = "l2.healthcheck.l1.ping.probes";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT                               = "l2.healthcheck.l1.socketConnect";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT_TIMEOUT                       = "l2.healthcheck.l1.socketConnectTimeout";
  public static final String L2_HEALTHCHECK_L1_SOCKECT_CONNECT_COUNT                         = "l2.healthcheck.l1.socketConnectCount";

  public static final String L2_HEALTHCHECK_L2_PING_ENABLED                                  = "l2.healthcheck.l2.ping.enabled";
  public static final String L2_HEALTHCHECK_L2_PING_IDLETIME                                 = "l2.healthcheck.l2.ping.idletime";
  public static final String L2_HEALTHCHECK_L2_PING_INTERVAL                                 = "l2.healthcheck.l2.ping.interval";
  public static final String L2_HEALTHCHECK_L2_PING_PROBES                                   = "l2.healthcheck.l2.ping.probes";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT                               = "l2.healthcheck.l2.socketConnect";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT                       = "l2.healthcheck.l2.socketConnectTimeout";
  public static final String L2_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                         = "l2.healthcheck.l2.socketConnectCount";

  public static final String L1_HEALTHCHECK_L2_BIND_ADDRESS                                  = "l1.healthcheck.l2.bindAddress";
  public static final String L1_HEALTHCHECK_L2_BIND_PORT                                     = "l1.healthcheck.l2.bindPort";
  public static final String L1_HEALTHCHECK_L2_PING_ENABLED                                  = "l1.healthcheck.l2.ping.enabled";
  public static final String L1_HEALTHCHECK_L2_PING_IDLETIME                                 = "l1.healthcheck.l2.ping.idletime";
  public static final String L1_HEALTHCHECK_L2_PING_INTERVAL                                 = "l1.healthcheck.l2.ping.interval";
  public static final String L1_HEALTHCHECK_L2_PING_PROBES                                   = "l1.healthcheck.l2.ping.probes";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT                               = "l1.healthcheck.l2.socketConnect";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT_TIMEOUT                       = "l1.healthcheck.l2.socketConnectTimeout";
  public static final String L1_HEALTHCHECK_L2_SOCKECT_CONNECT_COUNT                         = "l1.healthcheck.l2.socketConnectCount";

  /*********************************************************************************************************************
   * <code>
   * Section : TCMessage debug monitoring
   * tcm.monitor.enabled - If enabled the count and size of TC messages will be collected and logged
   * tcm.monitor.delay   - The delay (in seconds) between reporting to the log
   * </code>
   ********************************************************************************************************************/
  public static final String TCM_MONITOR_ENABLED                                             = "tcm.monitor.enabled";
  public static final String TCM_MONITOR_DELAY                                               = "tcm.monitor.delay";

  /*********************************************************************************************************************
   * <code>
   * Section : HTTP
   * http.defaultservlet.enabled                - If true, will serve files through embedded HTTP server
   * http.defaultservlet.attribute.aliases      - If true, allows aliases like symlinks to be followed while serving files
   * http.defaultservlet.attribute.dirallowed   - If true, directory listings are returned if no welcome file is found
   * </code>
   ********************************************************************************************************************/
  public static final String HTTP_DEFAULT_SERVLET_ENABLED                                    = "http.defaultservlet.enabled";
  public static final String HTTP_DEFAULT_SERVLET_ATTRIBUTE_ALIASES                          = "http.defaultservlet.attribute.aliases";
  public static final String HTTP_DEFAULT_SERVLET_ATTRIBUTE_DIR_ALLOWED                      = "http.defaultservlet.attribute.dirallowed";

  /*********************************************************************************************************************
   * <code>
   * Section : Remote JMX
   * l2.remotejmx.maxthreads                     - Maximum number of concurrent remote jmx operations permitted
   * l2.remotejmx.idletime                       - Idle timeout (in seconds) for remote jmx processing threads
   * </code>
   ********************************************************************************************************************/
  public static final String L2_REMOTEJMX_MAXTHREADS                                         = "l2.remotejmx.maxthreads";
  public static final String L2_REMOTEJMX_IDLETIME                                           = "l2.remotejmx.idletime";

  /*********************************************************************************************************************
   * <code>
   * Section :  Stats Printer
   * stats.printer.intervalInMillis              - Interval at which gathered stats are printed
   * </code>
   ********************************************************************************************************************/
  public static final String STATS_PRINTER_INTERVAL                                          = "stats.printer.intervalInMillis";

  /*********************************************************************************************************************
   * <code>
   * Section :  EnterpriseLicenseResovler
   * license.path                                - path to license key
   * </code>
   ********************************************************************************************************************/
  public static final String PRODUCTKEY_RESOURCE_PATH                                        = "productkey.resource.path";
  public static final String PRODUCTKEY_PATH                                                 = "productkey.path";
  public static final String LICENSE_PATH                                                    = "license.path";

  /*********************************************************************************************************************
   * <code>
   * Section :  Instrumentation Settings
   * instrumentation.finalField.fastRead         - Enable/disable `dirty' reading of final fields
   * </code>
   ********************************************************************************************************************/
  public static final String INSTRUMENTATION_FINAL_FIELD_FAST_READ                           = "instrumentation.finalField.fastRead";

  /*********************************************************************************************************************
   * <code>
   * l2.dump.on.exception.timeout - After get an uncaught exception, the server takes a dump. If the dump doesn't
   * happen within this timeout the server will exit (in seconds).
   * </code>
   ********************************************************************************************************************/
  public static final String L2_DUMP_ON_EXCEPTION_TIMEOUT                                    = "l2.dump.on.exception.timeout";

  /*********************************************************************************************************************
   * <code>
   * Dev console Settings
   *  console.max.operator.events   -   Number of operator events dev console will show
   *                                    in the panel before it starts recycling
   *  l2.operator.events.store      -   Number of operator events L2s will store to keep the history of the events
   *  tc.time.sync.threshold        -   Number of second of tolerable system time difference between
   *                                    two nodes of cluster beyond which and operator event will be thrown 
   * </code>
   ********************************************************************************************************************/
  public static final String DEV_CONSOLE_MAX_OPERATOR_EVENTS                                 = "dev.console.max.operator.events";
  public static final String L2_OPERATOR_EVENTS_STORE                                        = "l2.operator.events.store";
  public static final String TC_TIME_SYNC_THRESHOLD                                          = "tc.time.sync.threshold";

  /*********************************************************************************************************************
   * <code>
   * Section :  L1 Shutdown Settings
   * l1.shutdown.threadgroup.gracetime - time allowed for termination of all threads in the TC thread group (in milliseconds).
   * </code>
   ********************************************************************************************************************/
  public static final String L1_SHUTDOWN_THREADGROUP_GRACETIME                               = "l1.shutdown.threadgroup.gracetime";

  /*********************************************************************************************************************
   * <code>
   * Section :  OffHeap Cache Settings
   * </code>
   ********************************************************************************************************************/

  public static final String L2_OFFHEAP_MAP_CACHE_INITIAL_DATASIZE                           = "l2.offHeapCache.map.initialDataSize";
  public static final String L2_OFFHEAP_MAP_CACHE_TABLESIZE                                  = "l2.offHeapCache.map.tableSize";
  public static final String L2_OFFHEAP_MAP_CACHE_CONCURRENCY                                = "l2.offHeapCache.map.concurrency";
  public static final String L2_OFFHEAP_EVENT_GENERATOR_THRESHOLD                            = "l2.offHeapCache.operator.event.generator.threshold";
  public static final String L2_OFFHEAP_EVENT_GENERATOR_SLEEP_INTERVAL                       = "l2.offHeapCache.operator.event.generator.sleepInterval";

  // we calculate the right values. user can still override them explicitly
  public static final String L2_OFFHEAP_CACHE_MAX_CHUNK_SIZE                                 = "l2.offHeapCache.max.chunk.size";
  public static final String L2_OFFHEAP_CACHE_MIN_CHUNK_SIZE                                 = "l2.offHeapCache.min.chunk.size";
  public static final String L2_OFFHEAP_OBJECT_CACHE_INITIAL_DATASIZE                        = "l2.offHeapCache.object.initialDataSize";
  public static final String L2_OFFHEAP_OBJECT_CACHE_TABLESIZE                               = "l2.offHeapCache.object.tableSize";
  public static final String L2_OFFHEAP_OBJECT_CACHE_CONCURRENCY                             = "l2.offHeapCache.object.concurrency";
  public static final String L2_OFFHEAP_TEMP_SWAP_FLUSH_TO_DISK_COUNT                        = "l2.offHeapCache.temp.swap.flush.to.disk.count";
  public static final String L2_OFFHEAP_TEMP_SWAP_THROTTLE_SIZE                              = "l2.offHeapCache.temp.swap.throttle.size";

  // for tests
  public static final String L2_OFFHEAP_SKIP_JVMARG_CHECK                                    = "l2.offHeapCache.skip.jvmarg.check";

  public static final String AW_ASMCLASSINFO_IGNORE_ERRORS                                   = "aw.asmclassinfo.ignore.errors";

  public static final String SEARCH_QUERY_WAIT_FOR_TXNS                                      = "search.query.wait.for.txns";
  public static final String SEARCH_USE_COMMIT_THREAD                                        = "search.use.commit.thread";
  public static final String SEARCH_PASSIVE_MAX_CHUNK                                        = "search.passive.max.chunk";
  public static final String SEARCH_PASSIVE_MAX_PENDING                                      = "search.passive.max.pending";

  public static final String SIGAR_ENABLED                                                   = "sigar.enabled";
}
