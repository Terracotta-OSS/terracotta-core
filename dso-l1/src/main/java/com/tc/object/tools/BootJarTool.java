/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleException;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.commons.SerialVersionUIDAdder;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.InnerClassNode;
import com.tc.asm.tree.MethodNode;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo;
import com.tc.async.api.EventContext;
import com.tc.bytes.BufferPool;
import com.tc.bytes.TCByteBuffer;
import com.tc.cache.ExpirableEntry;
import com.tc.cluster.DsoCluster;
import com.tc.cluster.DsoClusterEvent;
import com.tc.cluster.DsoClusterListener;
import com.tc.cluster.DsoClusterTopology;
import com.tc.cluster.exceptions.UnclusteredObjectException;
import com.tc.config.Directories;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCError;
import com.tc.exception.TCNonPortableObjectError;
import com.tc.exception.TCNotRunningException;
import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCObjectNotFoundException;
import com.tc.exception.TCObjectNotSharableException;
import com.tc.exception.TCRuntimeException;
import com.tc.injection.annotations.InjectedDsoInstance;
import com.tc.injection.exceptions.UnsupportedInjectedDsoInstanceTypeException;
import com.tc.io.TCByteArrayOutputStream;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferInput.Mark;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;
import com.tc.lang.Recyclable;
import com.tc.logging.CustomerLogging;
import com.tc.logging.LogLevel;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TerracottaMBean;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NIOWorkarounds;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TCObjectExternal;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfImpl;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.TCObjectSelfStoreValue;
import com.tc.object.TCObjectServerMap;
import com.tc.object.TraversedReferences;
import com.tc.object.appevent.ApplicationEvent;
import com.tc.object.appevent.ApplicationEventContext;
import com.tc.object.applicator.ApplicatorObjectManager;
import com.tc.object.bytecode.AAFairDistributionPolicyMarker;
import com.tc.object.bytecode.AbstractStringBuilderAdapter;
import com.tc.object.bytecode.AccessibleObjectAdapter;
import com.tc.object.bytecode.AddInterfacesAdapter;
import com.tc.object.bytecode.ArrayListAdapter;
import com.tc.object.bytecode.AtomicIntegerAdapter;
import com.tc.object.bytecode.AtomicLongAdapter;
import com.tc.object.bytecode.BufferedWriterAdapter;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ChangeClassNameHierarchyAdapter;
import com.tc.object.bytecode.ChangeClassNameRootAdapter;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.CloneUtil;
import com.tc.object.bytecode.CopyOnWriteArrayListAdapter;
import com.tc.object.bytecode.DataOutputStreamAdapter;
import com.tc.object.bytecode.DuplicateMethodAdapter;
import com.tc.object.bytecode.HashMapClassAdapter;
import com.tc.object.bytecode.HashtableClassAdapter;
import com.tc.object.bytecode.JavaLangReflectArrayAdapter;
import com.tc.object.bytecode.JavaLangReflectFieldAdapter;
import com.tc.object.bytecode.JavaLangReflectProxyClassAdapter;
import com.tc.object.bytecode.JavaLangStringAdapter;
import com.tc.object.bytecode.JavaLangStringTC;
import com.tc.object.bytecode.JavaLangThrowableDebugClassAdapter;
import com.tc.object.bytecode.JavaNetURLAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentCyclicBarrierClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapEntryIteratorAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapHashEntryAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapSegmentAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapValueIteratorAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapWriteThroughEntryAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter;
import com.tc.object.bytecode.JavaUtilTreeMapAdapter;
import com.tc.object.bytecode.LinkedHashMapClassAdapter;
import com.tc.object.bytecode.LinkedListAdapter;
import com.tc.object.bytecode.LogicalClassSerializationAdapter;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerInternal;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.ManagerUtilInternal;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.NullManagerInternal;
import com.tc.object.bytecode.NullTCObject;
import com.tc.object.bytecode.OverridesHashCode;
import com.tc.object.bytecode.ReentrantLockClassAdapter;
import com.tc.object.bytecode.ReentrantReadWriteLockClassAdapter;
import com.tc.object.bytecode.SessionConfiguration;
import com.tc.object.bytecode.SetRemoveMethodAdapter;
import com.tc.object.bytecode.StringBufferAdapter;
import com.tc.object.bytecode.StringGetCharsAdapter;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.bytecode.VectorAdapter;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.ClassProcessor;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelperJDK15;
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;
import com.tc.object.bytecode.hook.impl.Util;
import com.tc.object.cache.Cacheable;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.dmi.DmiClassSpec;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.DNA.DNAType;
import com.tc.object.dna.api.DNACursor;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAWriter;
import com.tc.object.dna.api.LogicalAction;
import com.tc.object.dna.api.PhysicalAction;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ProxyInstance;
import com.tc.object.field.TCField;
import com.tc.object.ibm.SystemInitializationAdapter;
import com.tc.object.loaders.BytecodeProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.LoaderDescription;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.NamedLoaderAdapter;
import com.tc.object.loaders.Namespace;
import com.tc.object.loaders.StandardClassLoaderAdapter;
import com.tc.object.locks.LongLockID;
import com.tc.object.locks.Notify;
import com.tc.object.locks.ThreadID;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.InstrumentationLoggerImpl;
import com.tc.object.logging.NullInstrumentationLogger;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.metadata.MetaDataDescriptorInternal;
import com.tc.object.metadata.NVPair;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheLockProvider;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.servermap.localcache.impl.TCObjectSelfWrapper;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.object.tx.UnlockedSharedObjectException;
import com.tc.object.util.OverrideCheck;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.plugins.ModulesLoader;
import com.tc.properties.TCProperties;
import com.tc.search.AggregatorOperations;
import com.tc.search.IndexQueryResult;
import com.tc.search.SearchQueryResults;
import com.tc.search.SortOperations;
import com.tc.search.StackOperations;
import com.tc.statistics.LazilyInitializedSRA;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticDataCSVParser;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.statistics.StatisticType;
import com.tc.text.Banner;
import com.tc.util.AbstractIdentifier;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.EnumerationWrapper;
import com.tc.util.FieldUtils;
import com.tc.util.HashtableKeySetWrapper;
import com.tc.util.HashtableValuesWrapper;
import com.tc.util.ListIteratorWrapper;
import com.tc.util.ObjectCloneUtil;
import com.tc.util.SequenceID;
import com.tc.util.SequenceID.SequenceIDComparator;
import com.tc.util.SetIteratorWrapper;
import com.tc.util.THashMapCollectionWrapper;
import com.tc.util.UnsafeUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.UnknownJvmVersionException;
import com.tc.util.runtime.UnknownRuntimeVersionException;
import com.tc.util.runtime.Vm;
import com.tc.util.runtime.VmVersion;
import com.tcclient.cluster.DsoClusterEventsNotifier;
import com.tcclient.cluster.DsoClusterInternal;
import com.tcclient.cluster.DsoClusterInternalEventsGun;
import com.tcclient.cluster.DsoNode;
import com.tcclient.cluster.DsoNodeImpl;
import com.tcclient.cluster.DsoNodeInternal;
import com.tcclient.cluster.DsoNodeMetaData;
import com.tcclient.cluster.OutOfBandDsoClusterListener;
import com.tcclient.util.HashtableEntrySetWrapper;
import com.tcclient.util.MapEntrySetWrapper;

import gnu.trove.TLinkable;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.AccessibleObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tool for creating the DSO boot jar
 */
public class BootJarTool {
  public static final String          TC_DEBUG_THROWABLE_CONSTRUCTION  = "tc.debug.throwable.construction";

  private static final String         EXCESS_CLASSES                   = "excess";
  private static final String         MISSING_CLASSES                  = "missing";

  private final static String         TARGET_FILE_OPTION               = "o";
  private final static boolean        WRITE_OUT_TEMP_FILE              = true;

  private static final String         DEFAULT_CONFIG_SPEC              = "tc-config.xml";

  private final ClassLoader           tcLoader;
  private final ClassLoader           systemLoader;
  private final DSOClientConfigHelper configHelper;
  private final File                  outputFile;
  private final Portability           portability;

  // various sets that are populated while massaging user defined boot jar specs
  private final Set                   notBootstrapClasses              = new HashSet();
  private final Set                   notAdaptableClasses              = new HashSet();
  private final Set                   logicalSubclasses                = new HashSet();
  private final Set                   autoIncludedBootstrapClasses     = new HashSet();
  private final Set                   nonExistingClasses               = new HashSet();

  private InstrumentationLogger       instrumentationLogger;
  private BootJar                     bootJar;
  private final BootJarHandler        bootJarHandler;
  private final boolean               quiet;

  public static final String          SYSTEM_CLASSLOADER_NAME_PROPERTY = "com.tc.loader.system.name";
  public static final String          EXT_CLASSLOADER_NAME_PROPERTY    = "com.tc.loader.ext.name";

  private static final TCLogger       consoleLogger                    = CustomerLogging.getConsoleLogger();

  public BootJarTool(final DSOClientConfigHelper configuration, final File outputFile,
                     final ClassLoader systemProvider, final boolean quiet) throws Exception {
    this.configHelper = configuration;
    this.outputFile = outputFile;
    this.systemLoader = systemProvider;
    this.tcLoader = getClass().getClassLoader();
    this.bootJarHandler = new BootJarHandler(WRITE_OUT_TEMP_FILE, this.outputFile);
    this.quiet = quiet;
    this.portability = new PortabilityImpl(this.configHelper);
    loadModules();
  }

  private void loadModules() throws Exception {
    // remove the user defined specs already load from config while modules are running so that specs created take
    // precedence from user defined specs
    final List userSpecs = new ArrayList();
    for (final Iterator i = this.configHelper.getAllUserDefinedBootSpecs(); i.hasNext();) {
      userSpecs.add(i.next());
    }

    for (final Iterator i = userSpecs.iterator(); i.hasNext();) {
      this.configHelper.removeSpec(((TransparencyClassSpec) i.next()).getClassName());
    }

    // load the modules
    try {
      ModulesLoader.initModules(this.configHelper, null, null, true);
    } catch (final BundleException e) {
      exit("Error during module initialization.", e);
    }

    // put the user defined specs back not already included by modules
    for (final Iterator i = userSpecs.iterator(); i.hasNext();) {
      final TransparencyClassSpec userSpec = (TransparencyClassSpec) i.next();

      if (this.configHelper.getSpec(userSpec.getClassName()) == null) {
        this.configHelper.addUserDefinedBootSpec(userSpec.getClassName(), userSpec);
      }
    }
  }

  public BootJarTool(final DSOClientConfigHelper configuration, final File outputFile, final ClassLoader systemProvider)
      throws Exception {
    this(configuration, outputFile, systemProvider, false);
  }

  private final void addJdk15SpecificPreInstrumentedClasses() {
    if (Vm.isJDK15Compliant()) {
      final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec("java.math.MathContext");
      spec.markPreInstrumented();

      addInstrumentedJavaUtilConcurrentLocks();

      addInstrumentedJavaUtilConcurrentLinkedBlockingQueue();
      addInstrumentedJavaUtilConcurrentHashMap();
      addInstrumentedJavaUtilConcurrentCyclicBarrier();
      addInstrumentedJavaUtilConcurrentFutureTask();
    }
  }

  /**
   * Scans the boot JAR file to determine if it is complete and contains only the user-classes that are specified in the
   * tc-config's <additional-boot-jar-classes/> section. Program execution halts if it fails these checks.
   */
  private final void scanJar(final File bootJarFile) {
    final String MESSAGE0 = "\nYour boot JAR file might be out of date or invalid.";
    final String MESSAGE1 = "\nThe following classes was declared in the <additional-boot-jar-classes/> section "
                            + "of your tc-config file but is not a part of your boot JAR file:";
    final String MESSAGE2 = "\nThe following user-classes were found in the boot JAR but was not declared in the "
                            + "<additional-boot-jar-classes/> section of your tc-config file:";
    final String MESSAGE3 = "\nUse the make-boot-jar tool to re-create your boot JAR.";
    try {
      final Map result = compareBootJarContentsToUserSpec(bootJarFile);
      final Set missing = (Set) result.get(MISSING_CLASSES);
      final Set excess = (Set) result.get(EXCESS_CLASSES);

      if (!missing.isEmpty() || !excess.isEmpty()) {
        consoleLogger.fatal(MESSAGE0);

        if (!missing.isEmpty()) {
          consoleLogger.error(MESSAGE1);
          for (final Iterator i = missing.iterator(); i.hasNext();) {
            consoleLogger.error("- " + i.next());
          }
        }

        if (!excess.isEmpty()) {
          consoleLogger.error(MESSAGE2);
          for (final Iterator i = missing.iterator(); i.hasNext();) {
            consoleLogger.error("- " + i.next());
          }
        }
        consoleLogger.error(MESSAGE3);
        System.exit(1);
      }
    } catch (final InvalidBootJarMetaDataException e) {
      consoleLogger.fatal(e.getMessage());
      consoleLogger.fatal(MESSAGE3);
      System.exit(1);
    }
    // catch (IOException e) {
    // consoleLogger.fatal("\nUnable to read DSO boot JAR file: '" + bootJarFile
    // + "'; you can specify the boot JAR file to scan using the -o or --bootjar-file option.");
    // System.exit(1);
    // }
  }

  /**
   * Checks if the given bootJarFile is complete; meaning: - All the classes declared in the configurations
   * <additional-boot-jar-classes/> section is present in the boot jar. - And there are no user-classes present in the
   * boot jar that is not declared in the <additional-boot-jar-classes/> section
   * 
   * @return <code>true</code> if the boot jar is complete.
   */
  private final boolean isBootJarComplete(final File bootJarFile) {
    try {
      final Map result = compareBootJarContentsToUserSpec(bootJarFile);
      final Set missing = (Set) result.get(MISSING_CLASSES);
      final Set excess = (Set) result.get(EXCESS_CLASSES);
      return missing.isEmpty() && excess.isEmpty();
    } catch (final Exception e) {
      return false;
    }
  }

  private final Map compareBootJarContentsToUserSpec(final File bootJarFile) throws InvalidBootJarMetaDataException {
    try {
      // verify that userspec is valid, eg: no non-existent classes declared
      massageSpecs(getUserDefinedSpecs(getTCSpecs()), false);
      issueWarningsAndErrors();

      // check that everything listed in userspec is in the bootjar
      final Map result = new HashMap();
      final Map userSpecs = massageSpecs(getUserDefinedSpecs(getTCSpecs()), false);
      final BootJar bootJarLocal = BootJar.getBootJarForReading(bootJarFile);
      final Set preInstrumented = bootJarLocal.getAllPreInstrumentedClasses();
      final Set unInstrumented = bootJarLocal.getAllUninstrumentedClasses();
      final Set missing = new HashSet();
      for (final Iterator i = userSpecs.keySet().iterator(); i.hasNext();) {
        final String cn = (String) i.next();
        if (!preInstrumented.contains(cn) && !unInstrumented.contains(cn)) {
          missing.add(cn);
        }
      }

      // check that everything marked as foreign in the bootjar is also
      // listed in the userspec
      result.put(MISSING_CLASSES, missing);
      final Set foreign = bootJarLocal.getAllForeignClasses();
      final Set excess = new HashSet();
      for (final Iterator i = foreign.iterator(); i.hasNext();) {
        final String cn = (String) i.next();
        if (!userSpecs.keySet().contains(cn)) {
          excess.add(cn);
        }
      }
      result.put(EXCESS_CLASSES, excess);
      return result;
    } catch (final InvalidBootJarMetaDataException e) {
      throw e;
    } catch (final Exception e) {
      exit(e.getMessage());
    }
    return null;
  }

  /**
   * verify that all of the reference TC classes are in the bootjar
   */
  private final void verifyJar(final File bootJarFile) {
    try {
      final BootJar bootJarLocal = BootJar.getBootJarForReading(bootJarFile);
      final Set bootJarClassNames = bootJarLocal.getAllClasses();
      final Map offendingClasses = new HashMap();
      for (final Iterator i = bootJarClassNames.iterator(); i.hasNext();) {
        final String className = (String) i.next();
        final byte[] bytes = bootJarLocal.getBytesForClass(className);
        final ClassReader cr = new ClassReader(bytes);
        final ClassVisitor cv = new BootJarClassDependencyVisitor(bootJarClassNames, offendingClasses);
        cr.accept(cv, ClassReader.SKIP_FRAMES);
      }

      final String newline = System.getProperty("line.separator");
      final StringBuffer msg = new StringBuffer();
      msg.append(newline).append(newline);
      msg.append("The following Terracotta classes needs to be included in the boot jar:");
      msg.append(newline).append(newline);

      for (final Iterator i = offendingClasses.entrySet().iterator(); i.hasNext();) {
        final Map.Entry entry = (Map.Entry) i.next();
        msg.append("  - " + entry.getKey() + " [" + entry.getValue() + "]" + newline);
      }
      Assert.assertTrue(msg, offendingClasses.isEmpty());
    } catch (final Exception e) {
      exit(e.getMessage());
    }
  }

  public final void generateJar() {
    this.instrumentationLogger = new InstrumentationLoggerImpl(this.configHelper.getInstrumentationLoggingOptions());
    try {
      this.bootJarHandler.validateDirectoryExists();
    } catch (final BootJarHandlerException e) {
      exit(e.getMessage(), e);
    }

    if (!this.quiet) {
      this.bootJarHandler.announceCreationStart();
    }

    try {
      this.bootJar = this.bootJarHandler.getBootJar();

      addInstrumentedHashMap();
      addInstrumentedHashtable();
      addInstrumentedJavaUtilCollection();
      addReflectionInstrumentation();

      addJdk15SpecificPreInstrumentedClasses();

      loadTerracottaClass(DebugUtil.class.getName());
      loadTerracottaClass(TCMap.class.getName());
      if (Vm.isJDK15Compliant()) {
        loadTerracottaClass("com.tc.util.concurrent.locks.TCLock");
      }
      if (Vm.isJDK16Compliant()) {
        loadTerracottaClass("com.tc.util.concurrent.locks.CopyOnWriteArrayListLock");
      }
      loadTerracottaClass(com.tc.util.Stack.class.getName());
      loadTerracottaClass(TCObjectNotSharableException.class.getName());
      loadTerracottaClass(TCObjectNotFoundException.class.getName());
      loadTerracottaClass(TCNonPortableObjectError.class.getName());
      loadTerracottaClass(TCError.class.getName());
      loadTerracottaClass(TCNotRunningException.class.getName());
      loadTerracottaClass(TerracottaOperatorEvent.EventType.class.getName());
      loadTerracottaClass(TerracottaOperatorEvent.EventSubsystem.class.getName());

      loadTerracottaClass(THashMapCollectionWrapper.class.getName());
      loadTerracottaClass(THashMapCollectionWrapper.class.getName() + "$IteratorWrapper");
      loadTerracottaClass(ListIteratorWrapper.class.getName());
      loadTerracottaClass(MapEntrySetWrapper.class.getName());
      loadTerracottaClass(MapEntrySetWrapper.class.getName() + "$IteratorWrapper");
      loadTerracottaClass(HashtableEntrySetWrapper.class.getName());
      loadTerracottaClass(HashtableEntrySetWrapper.class.getName() + "$HashtableIteratorWrapper");
      loadTerracottaClass(HashtableKeySetWrapper.class.getName());
      loadTerracottaClass(HashtableKeySetWrapper.class.getName() + "$IteratorWrapper");
      loadTerracottaClass(HashtableValuesWrapper.class.getName());
      loadTerracottaClass(HashtableValuesWrapper.class.getName() + "$IteratorWrapper");
      loadTerracottaClass(SetIteratorWrapper.class.getName());
      loadTerracottaClass(EnumerationWrapper.class.getName());
      loadTerracottaClass(NamedClassLoader.class.getName());
      loadTerracottaClass(TransparentAccess.class.getName());
      loadTerracottaClass(BytecodeProvider.class.getName());

      loadTerracottaClass(Manageable.class.getName());
      loadTerracottaClass(AAFairDistributionPolicyMarker.class.getName());
      loadTerracottaClass(Clearable.class.getName());
      loadTerracottaClass(NotClearable.class.getName());
      loadTerracottaClass(TCServerMap.class.getName());
      loadTerracottaClass(IndexQueryResult.class.getName());
      loadTerracottaClass(SearchQueryResults.class.getName());
      loadTerracottaClass(ExpirableEntry.class.getName());
      loadTerracottaClass(OverridesHashCode.class.getName());
      loadTerracottaClass(Manager.class.getName());
      loadTerracottaClass(ManagerInternal.class.getName());
      loadTerracottaClass(InstrumentationLogger.class.getName());
      loadTerracottaClass(NullInstrumentationLogger.class.getName());
      loadTerracottaClass(NullManager.class.getName());
      loadTerracottaClass(NullManagerInternal.class.getName());
      loadTerracottaClass(NullTCLogger.class.getName());
      loadTerracottaClass(ManagerUtil.class.getName());
      loadTerracottaClass(ManagerUtilInternal.class.getName());
      loadTerracottaClass(SessionConfiguration.class.getName());
      loadTerracottaClass(ManagerUtil.class.getName() + "$GlobalManagerHolder");
      loadTerracottaClass(TCObject.class.getName());
      loadTerracottaClassesReachableFromTCObject();
      loadTerracottaClassesForTCObjectSelf();
      loadTerracottaClass(TCObjectServerMap.class.getName());
      loadTerracottaClass(TCObjectExternal.class.getName());
      loadTerracottaClass(CloneUtil.class.getName());
      loadTerracottaClass(ToggleableStrongReference.class.getName());
      loadTerracottaClass(TCClass.class.getName());
      loadTerracottaClass(TCField.class.getName());
      loadTerracottaClass(NullTCObject.class.getName());
      loadTerracottaClass(Cacheable.class.getName());
      loadTerracottaClass(ObjectID.class.getName());
      loadTerracottaClass(AbstractIdentifier.class.getName());
      loadTerracottaClass(TLinkable.class.getName());
      loadTerracottaClass(TCLogger.class.getName());
      loadTerracottaClass(LogLevel.class.getName());
      loadTerracottaClass(Banner.class.getName());
      loadTerracottaClass(Namespace.class.getName());
      loadTerracottaClass(NVPair.class.getName());
      loadTerracottaClass(StackOperations.class.getName());
      loadTerracottaClass(AggregatorOperations.class.getName());
      loadTerracottaClass(SortOperations.class.getName());
      loadTerracottaClass(ClassProcessorHelper.class.getName());
      loadTerracottaClass(ClassProcessorHelperJDK15.class.getName());
      loadTerracottaClass(ClassProcessorHelper.State.class.getName());
      loadTerracottaClass(ClassProcessorHelper.TcCommonLibQualifier.class.getName());
      loadTerracottaClass(ClassProcessor.class.getName());
      loadTerracottaClass(ClassPreProcessor.class.getName());
      loadTerracottaClass(ClassPostProcessor.class.getName());
      loadTerracottaClass(DSOContext.class.getName());
      loadTerracottaClass(ClassProvider.class.getName());
      loadTerracottaClass(TCRuntimeException.class.getName());
      loadTerracottaClass(FieldUtils.class.getName());
      loadTerracottaClass(UnsafeUtil.class.getName());
      loadTerracottaClass(TCNotSupportedMethodException.class.getName());
      loadTerracottaClass(ExceptionWrapper.class.getName());
      loadTerracottaClass(ExceptionWrapperImpl.class.getName());
      loadTerracottaClass(Os.class.getName());
      loadTerracottaClass(Util.class.getName());
      loadTerracottaClass(NIOWorkarounds.class.getName());
      loadTerracottaClass(TCProperties.class.getName());
      loadTerracottaClass(OverrideCheck.class.getName());
      loadTerracottaClass(JavaLangStringTC.class.getName());
      loadTerracottaClass(StringCompressionUtil.class.getName());
      loadTerracottaClass(CompressedData.class.getName());
      loadTerracottaClass(TCByteArrayOutputStream.class.getName());
      loadTerracottaClass(MetaDataDescriptor.class.getName());

      loadTerracottaClass("com.tc.object.bytecode.hook.impl.ArrayManager");
      loadTerracottaClass(ProxyInstance.class.getName());
      loadTerracottaClass(JavaLangArrayHelpers.class.getName());

      loadTerracottaClass(Vm.class.getName());
      loadTerracottaClass(VmVersion.class.getName());

      loadTerracottaClass(UnknownJvmVersionException.class.getName());
      loadTerracottaClass(UnknownRuntimeVersionException.class.getName());

      // Locking System Classes
      loadTerracottaClass(com.tc.object.locks.LockID.class.getName());
      loadTerracottaClass(LongLockID.class.getName());
      loadTerracottaClass(com.tc.object.locks.LockID.LockIDType.class.getName());
      loadTerracottaClass(com.tc.object.locks.UnclusteredLockID.class.getName());
      loadTerracottaClass(com.tc.object.locks.LockLevel.class.getName());
      loadTerracottaClass(com.tc.object.locks.LockLevel.class.getName() + "$1");
      loadTerracottaClass(com.tc.object.locks.TerracottaLocking.class.getName());
      loadTerracottaClass(com.tc.object.locks.TerracottaLockingInternal.class.getName());
      loadTerracottaClass(com.tc.io.TCSerializable.class.getName());

      addManagementClasses();

      addRuntimeClasses();

      addLiterals();

      // local cache store classes
      loadTerracottaClass(ServerMapLocalCache.class.getName());
      loadTerracottaClass(L1ServerMapLocalCacheStore.class.getName());
      loadTerracottaClass(L1ServerMapLocalCacheLockProvider.class.getName());
      loadTerracottaClass(AbstractLocalCacheStoreValue.class.getName());
      loadTerracottaClass(TCObjectSelfStore.class.getName());
      loadTerracottaClass(TCObjectSelfStoreValue.class.getName());
      loadTerracottaClass(TCObjectSelfWrapper.class.getName());
      loadTerracottaClass(LocalCacheStoreEventualValue.class.getName());
      loadTerracottaClass(LocalCacheStoreStrongValue.class.getName());
      loadTerracottaClass(LocalCacheStoreIncoherentValue.class.getName());
      loadTerracottaClass(ObjectCloneUtil.class.getName());

      loadTerracottaClass(L1ServerMapLocalCacheStoreListener.class.getName());
      loadTerracottaClass(PutType.class.getName());
      for (int i = 1; i <= PutType.values().length; i++) {
        loadTerracottaClass(PutType.class.getName() + "$" + i);
      }
      loadTerracottaClass(RemoveType.class.getName());
      for (int i = 1; i <= RemoveType.values().length; i++) {
        loadTerracottaClass(RemoveType.class.getName() + "$" + i);
      }

      addSunStandardLoaders();
      addInstrumentedAccessibleObject();
      addInstrumentedJavaLangThrowable();
      addInstrumentedJavaLangStringBuffer();
      addInstrumentedClassLoader();
      addInstrumentedJavaLangString();
      addInstrumentedJavaNetURL();
      addInstrumentedProxy();
      addTreeMap();
      addObjectStreamClass();

      addClusterEventsAndMetaDataClasses();
      loadTerracottaClass(StatisticRetrievalAction.class.getName());
      loadTerracottaClass(StatisticType.class.getName());
      loadTerracottaClass(StatisticData.class.getName());
      loadTerracottaClass(StatisticDataCSVParser.class.getName());
      loadTerracottaClass(LazilyInitializedSRA.class.getName());

      addIBMSpecific();

      final Map internalSpecs = getTCSpecs();
      loadBootJarClasses(removeAlreadyLoaded(massageSpecs(internalSpecs, true)));

      final Map userSpecs = massageSpecs(getUserDefinedSpecs(internalSpecs), false);
      issueWarningsAndErrors();

      // user defined specs should ALWAYS be after internal specs
      loadBootJarClasses(removeAlreadyLoaded(userSpecs), true);

      // classes adapted/included after the user defined specs are not portable
      // if you want to make it portable, you will still need to declare
      // in the <additiona-boot-jar-classes/> section of your tc-config
      adaptClassIfNotAlreadyIncluded(BufferedWriter.class.getName(), BufferedWriterAdapter.class);
      adaptClassIfNotAlreadyIncluded(DataOutputStream.class.getName(), DataOutputStreamAdapter.class);
    } catch (final Throwable e) {
      exit(this.bootJarHandler.getCreationErrorMessage(), e);
    }

    try {
      BootJar.closeQuietly(this.bootJar);
      // bootJar.close();
      this.bootJarHandler.announceCreationEnd();
      // } catch (IOException e) {
      // exit(bootJarHandler.getCloseErrorMessage(), e);
    } catch (final BootJarHandlerException e) {
      exit(e.getMessage(), e.getCause());
    }
  }

  private void loadTerracottaClassesForTCObjectSelf() {
    loadTerracottaClass(TCObjectSelf.class.getName());
    loadTerracottaClass(TCObjectSelfImpl.class.getName());
    loadTerracottaClass(TCObjectSelfCallback.class.getName());
  }

  private void loadTerracottaClassesReachableFromTCObject() {
    // the following classes are referenced from TCObject
    // commented ones are those which are already added elsewhere

    // loadTerracottaClass(AbstractIdentifier.class.getName());
    loadTerracottaClass(ApplicationEvent.class.getName());
    loadTerracottaClass(ApplicationEventContext.class.getName());
    loadTerracottaClass(ApplicatorObjectManager.class.getName());
    loadTerracottaClass(BufferPool.class.getName());
    // loadTerracottaClass(Cacheable.class.getName());
    loadTerracottaClass(ChannelID.class.getName());
    loadTerracottaClass(ClientID.class.getName());
    loadTerracottaClass(ClientIDProvider.class.getName());
    loadTerracottaClass(ClientObjectManager.class.getName());
    loadTerracottaClass(ClientTransaction.class.getName());
    loadTerracottaClass(ClientTransactionManager.class.getName());
    loadTerracottaClass(DNA.class.getName());
    loadTerracottaClass(DNACursor.class.getName());
    loadTerracottaClass(DNAEncoding.class.getName());
    // loadTerracottaClass(DNAException.class.getName());
    loadTerracottaClass(DNAType.class.getName());
    loadTerracottaClass(DNAWriter.class.getName());
    loadTerracottaClass(DmiClassSpec.class.getName());
    loadTerracottaClass(DmiDescriptor.class.getName());
    loadTerracottaClass(EventContext.class.getName());
    loadTerracottaClass(GroupID.class.getName());
    loadTerracottaClass(LoaderDescription.class.getName());
    // loadTerracottaClass(LockID.class.getName());
    // loadTerracottaClass(LockIDType.class.getName());
    // loadTerracottaClass(LockLevel.class.getName());
    loadTerracottaClass(LogicalAction.class.getName());
    loadTerracottaClass(Mark.class.getName());
    // loadTerracottaClass(MetaDataDescriptor.class.getName());
    loadTerracottaClass(MetaDataDescriptorInternal.class.getName());
    loadTerracottaClass(NodeID.class.getName());
    loadTerracottaClass(Notify.class.getName());
    // loadTerracottaClass(ObjectID.class.getName());
    loadTerracottaClass(ObjectStringSerializer.class.getName());
    loadTerracottaClass(PhysicalAction.class.getName());
    loadTerracottaClass(Recyclable.class.getName());
    loadTerracottaClass(SequenceID.class.getName());
    loadTerracottaClass(SequenceIDComparator.class.getName());
    loadTerracottaClass(SessionID.class.getName());
    loadTerracottaClass(TCByteBuffer.class.getName());
    loadTerracottaClass(TCByteBufferInput.class.getName());
    loadTerracottaClass(TCByteBufferOutput.class.getName());
    // loadTerracottaClass(TCClass.class.getName());
    loadTerracottaClass(TCDataInput.class.getName());
    loadTerracottaClass(TCDataOutput.class.getName());
    // loadTerracottaClass(TCError.class.getName());
    // loadTerracottaClass(TCField.class.getName());
    // loadTerracottaClass(TCNonPortableObjectError.class.getName());
    // loadTerracottaClass(TCObject.class.getName());
    // loadTerracottaClass(TCObjectExternal.class.getName());
    // loadTerracottaClass(TCRuntimeException.class.getName());
    // loadTerracottaClass(TCSerializable.class.getName());
    // loadTerracottaClass(TLinkable.class.getName());
    loadTerracottaClass(ThreadID.class.getName());
    // loadTerracottaClass(ToggleableStrongReference.class.getName());
    loadTerracottaClass(TransactionCompleteListener.class.getName());
    loadTerracottaClass(TransactionContext.class.getName());
    loadTerracottaClass(TransactionID.class.getName());
    loadTerracottaClass(TraversedReferences.class.getName());
    loadTerracottaClass(TxnBatchID.class.getName());
    loadTerracottaClass(TxnType.class.getName());
    loadTerracottaClass(UnlockedSharedObjectException.class.getName());
  }

  private void addClusterEventsAndMetaDataClasses() {
    loadTerracottaClass(DsoCluster.class.getName());
    loadTerracottaClass(DsoClusterInternal.class.getName());
    loadTerracottaClass(DsoClusterEvent.class.getName());
    loadTerracottaClass(DsoClusterListener.class.getName());
    loadTerracottaClass(DsoClusterTopology.class.getName());
    loadTerracottaClass(DsoNode.class.getName());
    loadTerracottaClass(DsoNodeInternal.class.getName());
    loadTerracottaClass(DsoClusterInternalEventsGun.class.getName());
    loadTerracottaClass(DsoClusterEventsNotifier.class.getName());
    loadTerracottaClass(DsoClusterInternal.DsoClusterEventType.class.getName());
    loadTerracottaClass(OutOfBandDsoClusterListener.class.getName());
    {
      final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(DsoNodeImpl.class.getName());
      spec.markPreInstrumented();
      spec.setHonorTransient(true);
      spec.setHonorVolatile(true);
      byte[] bytes = getTerracottaBytes(spec.getClassName());
      bytes = doDSOTransform(spec.getClassName(), bytes);
      loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
    }
    {
      final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(DsoNodeMetaData.class.getName());
      spec.markPreInstrumented();
      spec.setHonorTransient(true);
      byte[] bytes = getTerracottaBytes(spec.getClassName());
      bytes = doDSOTransform(spec.getClassName(), bytes);
      loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
    }
    loadTerracottaClass(InjectedDsoInstance.class.getName());
    loadTerracottaClass(UnclusteredObjectException.class.getName());
    loadTerracottaClass(UnsupportedInjectedDsoInstanceTypeException.class.getName());
  }

  private void addObjectStreamClass() {
    final String jClassNameDots = "java.io.ObjectStreamClass";
    final String tcClassNameDots = "java.io.ObjectStreamClassTC";

    final byte[] tcData = getSystemBytes(tcClassNameDots);
    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    byte[] jData = getSystemBytes(jClassNameDots);

    final ClassReader jCR = new ClassReader(jData);
    final ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

    final Map instrumentedContext = new HashMap();
    final ClassVisitor cv = new FixedMergeTCToJavaClassAdapter(cw, null, jClassNameDots, tcClassNameDots, tcCN,
                                                               instrumentedContext);
    jCR.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();
    loadClassIntoJar(jClassNameDots, jData, true);
  }

  private void addLiterals() {
    this.bootJar.loadClassIntoJar("java.lang.Boolean", getSystemBytes("java.lang.Boolean"), false);
    this.bootJar.loadClassIntoJar("java.lang.Byte", getSystemBytes("java.lang.Byte"), false);
    this.bootJar.loadClassIntoJar("java.lang.Character", getSystemBytes("java.lang.Character"), false);
    this.bootJar.loadClassIntoJar("java.lang.Double", getSystemBytes("java.lang.Double"), false);
    this.bootJar.loadClassIntoJar("java.lang.Float", getSystemBytes("java.lang.Float"), false);
    this.bootJar.loadClassIntoJar("java.lang.Integer", getSystemBytes("java.lang.Integer"), false);
    this.bootJar.loadClassIntoJar("java.lang.Long", getSystemBytes("java.lang.Long"), false);
    this.bootJar.loadClassIntoJar("java.lang.Short", getSystemBytes("java.lang.Short"), false);

    this.bootJar.loadClassIntoJar("java.math.BigInteger", getSystemBytes("java.math.BigInteger"), false);
    this.bootJar.loadClassIntoJar("java.math.BigDecimal", getSystemBytes("java.math.BigDecimal"), false);

    this.bootJar.loadClassIntoJar("java.lang.StackTraceElement", getSystemBytes("java.lang.StackTraceElement"), false);
  }

  private void addIBMSpecific() {
    if (Vm.isIBM()) {
      // Yes, the class name is misspelled
      adaptAndLoad("com.ibm.misc.SystemIntialization", new SystemInitializationAdapter());

      addIbmInstrumentedAtomicInteger();
      addIbmInstrumentedAtomicLong();
    }
  }

  private void addReflectionInstrumentation() {
    if (this.configHelper.reflectionEnabled()) {
      adaptAndLoad("java.lang.reflect.Field", new JavaLangReflectFieldAdapter());
      adaptAndLoad("java.lang.reflect.Array", new JavaLangReflectArrayAdapter());
    }
  }

  private final Map getAllSpecs() {
    final Map map = new HashMap();
    final TransparencyClassSpec[] allSpecs = this.configHelper.getAllSpecs();
    for (final TransparencyClassSpec spec : allSpecs) {
      map.put(spec.getClassName(), spec);
    }
    return Collections.unmodifiableMap(map);
  }

  private void loadClassIntoJar(final String className, final byte[] data, final boolean isPreinstrumented) {
    loadClassIntoJar(className, data, isPreinstrumented, false);
  }

  private void loadClassIntoJar(final String className, final byte[] data, final boolean isPreinstrumented,
                                final boolean isForeign) {
    final Map userSpecs = getUserDefinedSpecs(getAllSpecs());
    if (!isForeign && userSpecs.containsKey(className)) {
      consoleLogger.warn(className + " already belongs in the bootjar by default.");
    }
    this.bootJar.loadClassIntoJar(className, data, isPreinstrumented, isForeign);
  }

  private void adaptAndLoad(final String name, final ClassAdapterFactory factory) {
    byte[] bytes = getSystemBytes(name);

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    final ClassVisitor cv = factory.create(cw, null);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    bytes = cw.toByteArray();

    loadClassIntoJar(name, bytes, false);
  }

  private final void addManagementClasses() {
    loadTerracottaClass(TerracottaMBean.class.getName());
  }

  private final boolean shouldIncludeStringBufferAndFriends() {
    final Map userSpecs = getUserDefinedSpecs(getTCSpecs());
    return userSpecs.containsKey("java.lang.StringBuffer") || userSpecs.containsKey("java.lang.AbstractStringBuilder")
           || userSpecs.containsKey("java.lang.StringBuilder");

  }

  private final void addRuntimeClasses() {
    loadTerracottaClass("com.tc.object.applicator.TCURL");
    loadTerracottaClass("com.tc.object.bytecode.TCMapEntry");

    // DEV-116; Some of these probably should'nt be in the boot jar
    loadTerracottaClass("com.tc.exception.ImplementMe");
    loadTerracottaClass("com.tc.exception.TCClassNotFoundException");
    loadTerracottaClass("com.tc.object.dna.api.DNAException");
    loadTerracottaClass("com.tc.util.Assert");
    loadTerracottaClass("com.tc.util.StringUtil");
    loadTerracottaClass("com.tc.util.TCAssertionError");

    // this class needed for ibm-jdk-15 branch
    loadTerracottaClass("com.tc.object.bytecode.ClassAdapterFactory");
  }

  private final void addTreeMap() {
    final String className = "java.util.TreeMap";
    final byte[] orig = getSystemBytes(className);

    final TransparencyClassSpec spec = this.configHelper.getSpec(className);

    final byte[] transformed = doDSOTransform(className, orig);

    final ClassReader cr = new ClassReader(transformed);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    final ClassVisitor cv = new JavaUtilTreeMapAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    loadClassIntoJar(className, cw.toByteArray(), spec.isPreInstrumented());
  }

  private final void issueWarningsAndErrors() {
    issueErrors(this.nonExistingClasses, "could not be found", "remove or correct",
                "Attempt to add classes that cannot be found: ");
    issueErrors(this.notBootstrapClasses,
                "are not loaded by the bootstap classloader and have not been included in the boot jar", "remove",
                "Attempt to add classes that are not loaded by bootstrap classloader: ");
    issueErrors(this.notAdaptableClasses, "are non-adaptable types and have not been included in the boot jar",
                "remove", "Attempt to add non-adaptable classes: ");
    issueErrors(this.logicalSubclasses,
                "are subclasses of logically managed types and have not been included in the boot jar", "remove",
                "Attempt to add subclasses of logically manages classes: ");
    issueWarnings(this.autoIncludedBootstrapClasses,
                  "were automatically included in the boot jar since they are required super classes", "add");
  }

  private final void issueErrors(final Set classes, final String desc, final String verb, final String shortDesc) {
    if (!classes.isEmpty()) {
      Banner.errorBanner("Boot jar creation failed.  The following set of classes " + desc + ". Please " + verb
                         + " them in the <additional-boot-jar-classes> section of the terracotta config: " + classes);
      exit(shortDesc + classes);
    }
  }

  private final void issueWarnings(final Set classes, final String desc, final String verb) {
    if (!classes.isEmpty()) {
      Banner.warnBanner("The following set of classes " + desc + ". Please " + verb
                        + " them in the <additional-boot-jar-classes> section of the terracotta config: " + classes);
    }
  }

  private final Map removeAlreadyLoaded(final Map specs) {
    final Map rv = new HashMap(specs);
    for (final Iterator i = rv.keySet().iterator(); i.hasNext();) {
      final String className = (String) i.next();
      if (this.bootJar.classLoaded(className)) {
        i.remove();
      }
    }

    return Collections.unmodifiableMap(rv);
  }

  private final Map massageSpecs(final Map specs, final boolean tcSpecs) {
    final Map rv = new HashMap();

    for (final Iterator i = specs.values().iterator(); i.hasNext();) {
      final TransparencyClassSpec spec = (TransparencyClassSpec) i.next();

      final Class topClass = getBootstrapClass(spec.getClassName());
      if (topClass == null) {
        if (tcSpecs && !spec.isHonorJDKSubVersionSpecific()) { throw new AssertionError("Class not found: "
                                                                                        + spec.getClassName()); }
        if (!tcSpecs) {
          this.nonExistingClasses.add(spec.getClassName());
        }
        continue;
      } else if (topClass.getClassLoader() != null) {
        if (!tcSpecs) {
          this.notBootstrapClasses.add(topClass.getName());
          continue;
        }
      }

      final Set supers = new HashSet();
      boolean add = true;
      if (!this.configHelper.isLogical(topClass.getName())) {
        Class clazz = topClass;
        while ((clazz != null) && (!this.portability.isInstrumentationNotNeeded(clazz.getName()))) {
          if (this.configHelper.isNeverAdaptable(JavaClassInfo.getClassInfo(clazz))) {
            if (tcSpecs) { throw new AssertionError("Not adaptable: " + clazz); }

            add = false;
            this.notAdaptableClasses.add(topClass.getName());
            break;
          }

          if ((clazz != topClass) && this.configHelper.isLogical(clazz.getName())) {
            if (tcSpecs) { throw new AssertionError(topClass + " is subclass of logical type " + clazz.getName()); }

            add = false;
            this.logicalSubclasses.add(topClass.getName());
            break;
          }

          if (!specs.containsKey(clazz.getName())
              && ((this.bootJar == null) || !this.bootJar.classLoaded(clazz.getName()))) {
            if (tcSpecs) { throw new AssertionError("Missing super class " + clazz.getName() + " for type "
                                                    + spec.getClassName()); }
            supers.add(clazz.getName());
          }

          clazz = clazz.getSuperclass();
        }
      }

      if (add) {
        // include orignal class
        rv.put(topClass.getName(), spec);

        // include supers (if found)
        for (final Iterator supes = supers.iterator(); supes.hasNext();) {
          final String name = (String) supes.next();
          this.autoIncludedBootstrapClasses.add(name);
          final TransparencyClassSpec superSpec = this.configHelper.getOrCreateSpec(name);
          superSpec.markPreInstrumented();
          rv.put(name, superSpec);
        }
      }
    }

    return Collections.unmodifiableMap(rv);
  }

  private final static Class getBootstrapClass(final String className) {
    try {
      return Class.forName(className, false, ClassLoader.getSystemClassLoader());
    } catch (final ClassNotFoundException e) {
      return null;
    }
  }

  private final void loadBootJarClasses(final Map specs, final boolean foreignClass) {
    for (final Iterator iter = specs.values().iterator(); iter.hasNext();) {
      final TransparencyClassSpec spec = (TransparencyClassSpec) iter.next();
      if (foreignClass) {
        spec.markForeign();
      }
      announce("Adapting: " + spec.getClassName());
      final byte[] classBytes = doDSOTransform(spec.getClassName(), getSystemBytes(spec.getClassName()));
      loadClassIntoJar(spec.getClassName(), classBytes, spec.isPreInstrumented(), foreignClass);
    }
  }

  private final void loadBootJarClasses(final Map specs) {
    loadBootJarClasses(specs, false);
  }

  private final Map getTCSpecs() {
    final Map map = new HashMap();

    final TransparencyClassSpec[] allSpecs = this.configHelper.getAllSpecs();
    for (final TransparencyClassSpec spec : allSpecs) {
      if (spec.isPreInstrumented()) {
        map.put(spec.getClassName(), spec);
      }
    }

    return Collections.unmodifiableMap(map);
  }

  private final Map getUserDefinedSpecs(final Map internalSpecs) {
    final Map rv = new HashMap();
    for (final Iterator i = this.configHelper.getAllUserDefinedBootSpecs(); i.hasNext();) {
      final TransparencyClassSpec spec = (TransparencyClassSpec) i.next();
      Assert.assertTrue(spec.isPreInstrumented());

      // Take out classes that don't need instrumentation (but the user included anyway)
      if (!this.portability.isInstrumentationNotNeeded(spec.getClassName())) {
        rv.put(spec.getClassName(), spec);
      }
    }
    // substract TC specs from the user set (overlaps are bad)
    rv.keySet().removeAll(internalSpecs.keySet());
    return Collections.unmodifiableMap(rv);
  }

  private final void loadTerracottaClass(final String className) {
    loadClassIntoJar(className, getTerracottaBytes(className), false);
  }

  private final byte[] getTerracottaBytes(final String className) {
    return getBytes(className, this.tcLoader);
  }

  private final byte[] getSystemBytes(final String className) {
    return getBytes(className, this.systemLoader);
  }

  private final byte[] getBytes(final String className, final ClassLoader provider) {
    try {
      return getBytesForClass(className, provider);
    } catch (final Exception e) {
      exit("Error sourcing bytes for class " + className, e);
    }
    return null;
  }

  public static final byte[] getBytesForClass(final String className, final ClassLoader loader)
      throws ClassNotFoundException {
    InputStream input = null;
    String resource = null;
    try {
      resource = BootJar.classNameToFileName(className);
      input = loader.getResourceAsStream(resource);
      if (input == null) { throw new ClassNotFoundException("No resource found for class: " + className); }
      return IOUtils.toByteArray(input);
    } catch (final IOException e) {
      throw new ClassNotFoundException("Error reading bytes from " + resource, e);
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  /**
   * Locates the root most cause of an Exception and returns its error message.
   * 
   * @param throwable The exception whose root cause message is extracted.
   * @return The message of the root cause of an exception.
   */
  private static String rootCauseMessage(final Throwable throwable) {
    if (throwable == null) { return ""; }

    Throwable rootCause = throwable;
    while (rootCause.getCause() != null) {
      rootCause = rootCause.getCause();
    }
    return rootCause.getMessage();
  }

  /**
   * Convenience method. Will delegate to exit(msg, null)
   * 
   * @param msg The custom message to print
   */
  private final void exit(final String msg) {
    exit(msg, null);
  }

  /**
   * Print custom error message and abort the application. The exit code is set to a non-zero value.
   * 
   * @param msg The custom message to print
   * @param throwable The exception that caused the application to abort. If this parameter is not null then the message
   *        from the exception is also printed.
   */
  private final void exit(final String msg, final Throwable throwable) {
    if (!WRITE_OUT_TEMP_FILE) {
      this.bootJar.setCreationErrorOccurred(true);
    }
    BootJar.closeQuietly(this.bootJar);

    final String newline = System.getProperty("line.separator");
    final String cause = rootCauseMessage(throwable);
    final StringBuffer errmsg = new StringBuffer();
    errmsg.append(newline).append(StringUtils.center(" ERROR DETAILS ", 77, "-")).append(newline);
    if (!StringUtils.isEmpty(msg)) {
      errmsg.append("+ ").append(msg).append(newline);
    }
    if (!StringUtils.isEmpty(cause)) {
      errmsg.append("+ ").append(cause).append(newline);
    }
    errmsg.append(StringUtils.repeat("-", 77));

    System.err.println(errmsg.toString());
    System.exit(1);
  }

  private final void addInstrumentedJavaLangStringBuffer() {
    final boolean makePortable = shouldIncludeStringBufferAndFriends();
    if (makePortable) {
      addPortableStringBuffer();
    } else {
      addNonPortableStringBuffer();
    }
  }

  private void addInstrumentedAccessibleObject() {
    final String classname = AccessibleObject.class.getName();
    byte[] bytes = getSystemBytes(classname);

    // instrument the state changing methods in AccessibleObject
    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new AccessibleObjectAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // regular DSO instrumentation
    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(classname);
    spec.markPreInstrumented();

    loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private void addIbmInstrumentedAtomicInteger() {
    Vm.assertIsIbm();
    if (!Vm.isJDK15Compliant()) { return; }

    final String classname = "java.util.concurrent.atomic.AtomicInteger";
    byte[] bytes = getSystemBytes(classname);

    // instrument the state changing methods in AtomicInteger
    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new AtomicIntegerAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // regular DSO instrumentation
    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(classname);
    spec.markPreInstrumented();

    loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private void addIbmInstrumentedAtomicLong() {
    Vm.assertIsIbm();
    if (!Vm.isJDK15Compliant()) { return; }

    final String classname = "java.util.concurrent.atomic.AtomicLong";
    byte[] bytes = getSystemBytes(classname);

    // instrument the state changing methods in AtomicLong
    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new AtomicLongAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // regular DSO instrumentation
    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(classname);
    spec.markPreInstrumented();

    loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private final void addPortableStringBuffer() {
    final boolean isJDK15 = Vm.isJDK15Compliant();
    if (isJDK15) {
      addAbstractStringBuilder();
    }

    byte[] bytes = getSystemBytes("java.lang.StringBuffer");

    // 1st pass
    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new StringBufferAdapter(cw, Vm.VERSION);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // 2nd pass
    cr = new ClassReader(bytes);
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new DuplicateMethodAdapter(cw, Collections.singleton("getChars(II[CI)V"));
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // 3rd pass (regular DSO instrumentation)
    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec("java.lang.StringBuffer");
    spec.markPreInstrumented();
    this.configHelper.addWriteAutolock("* java.lang.StringBuffer.*(..)");
    bytes = doDSOTransform(spec.getClassName(), bytes);

    // 4th pass (String.getChars(..) calls)
    cr = new ClassReader(bytes);
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new StringGetCharsAdapter(cw, new String[] { "^" + DuplicateMethodAdapter.UNMANAGED_PREFIX + ".*" });
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    // 5th pass (fixups)
    cr = new ClassReader(bytes);
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new StringBufferAdapter.FixUp(cw, Vm.VERSION);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private final void addNonPortableStringBuffer() {
    // even if we aren't making StringBu[ild|ff]er portable, we still need to make
    // sure it calls the fast getChars() methods on String

    final boolean isJDK15 = Vm.isJDK15Compliant();

    if (isJDK15) {
      if (Vm.isIBM()) {
        addNonPortableStringBuffer("java.lang.StringBuilder");
      } else {
        addNonPortableStringBuffer("java.lang.AbstractStringBuilder");
      }
    }

    addNonPortableStringBuffer("java.lang.StringBuffer");
  }

  private void addNonPortableStringBuffer(final String className) {
    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(className);
    spec.markPreInstrumented();

    byte[] bytes = getSystemBytes(className);

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new StringGetCharsAdapter(cw, new String[] { ".*" });
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    bytes = cw.toByteArray();

    loadClassIntoJar(className, bytes, spec.isPreInstrumented());
  }

  private final void addAbstractStringBuilder() {
    String className;
    if (Vm.isIBM()) {
      className = "java.lang.StringBuilder";
    } else {
      className = "java.lang.AbstractStringBuilder";
    }

    byte[] classBytes = getSystemBytes(className);

    ClassReader cr = new ClassReader(classBytes);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new DuplicateMethodAdapter(cw, Collections.singleton("getChars(II[CI)V"));
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    classBytes = cw.toByteArray();

    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(className);
    spec.markPreInstrumented();

    classBytes = doDSOTransform(className, classBytes);

    cr = new ClassReader(classBytes);
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new AbstractStringBuilderAdapter(cw, className);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    cr = new ClassReader(cw.toByteArray());
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new StringGetCharsAdapter(cw, new String[] { "^" + DuplicateMethodAdapter.UNMANAGED_PREFIX + ".*" });
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    loadClassIntoJar(className, cw.toByteArray(), spec.isPreInstrumented());
  }

  private final void addInstrumentedProxy() {
    final String className = "java.lang.reflect.Proxy";
    byte[] bytes = getSystemBytes(className);

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    final ClassVisitor cv = new JavaLangReflectProxyClassAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    bytes = cw.toByteArray();

    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(className);
    bytes = doDSOTransform(spec.getClassName(), bytes);
    loadClassIntoJar(className, bytes, true);
  }

  private final void addInstrumentedJavaLangString() {
    final byte[] orig = getSystemBytes("java.lang.String");

    final ClassReader cr = new ClassReader(orig);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    final ClassVisitor cv = new JavaLangStringAdapter(cw, Vm.VERSION, shouldIncludeStringBufferAndFriends(),
                                                      Vm.isAzul(), Vm.isIBM());
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    loadClassIntoJar("java.lang.String", cw.toByteArray(), false);
  }

  private final void addInstrumentedJavaNetURL() {
    final String className = "java.net.URL";
    byte[] bytes = getSystemBytes(className);

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    final ClassVisitor cv = new JavaNetURLAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(className,
                                                                         "com.tc.object.applicator.URLApplicator");
    spec.markPreInstrumented();
    spec.setHonorTransient(true);
    spec.addAlwaysLogSpec(SerializationUtil.URL_SET_SIGNATURE);
    // note that there's another set method, that is actually never referenced
    // from URLStreamHandler, so it's not accessible from classes that extend
    // URLStreamHandler, so I'm not supporting it here

    bytes = doDSOTransform(className, cw.toByteArray());

    loadClassIntoJar(className, bytes, spec.isPreInstrumented());
  }

  private final void addSunStandardLoaders() {
    byte[] orig = getSystemBytes("sun.misc.Launcher$AppClassLoader");

    ClassReader cr = new ClassReader(orig);
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ClassVisitor cv = new StandardClassLoaderAdapter(cw, Namespace.getStandardSystemLoaderName(),
                                                     SYSTEM_CLASSLOADER_NAME_PROPERTY);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    final byte[] tcData = getSystemBytes("sun.misc.AppClassLoaderTC");
    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    cr = new ClassReader(cw.toByteArray());
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new FixedMergeTCToJavaClassAdapter(cw, null, "sun.misc.Launcher$AppClassLoader", "sun.misc.AppClassLoaderTC",
                                            tcCN, new HashMap(), ByteCodeUtil.TC_METHOD_PREFIX, false);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    loadClassIntoJar("sun.misc.Launcher$AppClassLoader", cw.toByteArray(), false);

    orig = getSystemBytes("sun.misc.Launcher$ExtClassLoader");
    cr = new ClassReader(orig);
    cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    cv = new StandardClassLoaderAdapter(cw, Namespace.getStandardExtensionsLoaderName(), EXT_CLASSLOADER_NAME_PROPERTY);
    cr.accept(cv, ClassReader.SKIP_FRAMES);
    loadClassIntoJar("sun.misc.Launcher$ExtClassLoader", cw.toByteArray(), false);
  }

  private final void addInstrumentedJavaLangThrowable() {
    final String className = "java.lang.Throwable";
    byte[] bytes = getSystemBytes(className);

    if (System.getProperty(TC_DEBUG_THROWABLE_CONSTRUCTION) != null) {
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaLangThrowableDebugClassAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      bytes = cw.toByteArray();
    }

    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(className);
    spec.markPreInstrumented();
    spec.setHonorTransient(true);

    final byte[] instrumented = doDSOTransform(className, bytes);

    loadClassIntoJar(className, instrumented, spec.isPreInstrumented());
  }

  /**
   * This instrumentation is temporary to add debug statements to the CyclicBarrier class.
   */
  private final void addInstrumentedJavaUtilConcurrentCyclicBarrier() {
    if (!Vm.isJDK15Compliant()) { return; }

    byte[] bytes = getSystemBytes("java.util.concurrent.CyclicBarrier");

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new JavaUtilConcurrentCyclicBarrierClassAdapter(cw);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    bytes = cw.toByteArray();

    final TransparencyClassSpec spec = this.configHelper.getOrCreateSpec("java.util.concurrent.CyclicBarrier");
    bytes = doDSOTransform(spec.getClassName(), bytes);
    loadClassIntoJar("java.util.concurrent.CyclicBarrier", bytes, true);
  }

  private final void addInstrumentedJavaUtilConcurrentHashMap() {
    if (Vm.isJDK17Compliant()) {
      // DEV-6105
      Banner.warnBanner("Not including instrumented ConcurrentHashMap in boot jar");
      return;
    }

    if (!Vm.isJDK15Compliant()) { return; }

    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapEntrySetWrapper");
    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$IteratorWrapper");
    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapKeySetWrapper");
    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapKeySetWrapper$IteratorWrapper");

    // java.util.concurrent.ConcurrentHashMap
    {
      final String jClassNameDots = "java.util.concurrent.ConcurrentHashMap";
      final String tcClassNameDots = "java.util.concurrent.ConcurrentHashMapTC";

      final byte[] tcData = getSystemBytes(tcClassNameDots);
      final ClassReader tcCR = new ClassReader(tcData);
      final ClassNode tcCN = new ClassNode();
      tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

      byte[] jData = getSystemBytes(jClassNameDots);
      ClassReader jCR = new ClassReader(jData);
      ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv1 = new JavaUtilConcurrentHashMapAdapter(cw);

      jCR.accept(cv1, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
      jData = cw.toByteArray();

      jCR = new ClassReader(jData);
      cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

      final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jClassNameDots, this.systemLoader);
      final TransparencyClassAdapter dsoAdapter = this.configHelper
          .createDsoClassAdapterFor(cw, jClassInfo, this.instrumentationLogger, getClass().getClassLoader(), true, true);
      final Map instrumentedContext = new HashMap();
      final ClassVisitor cv = new SerialVersionUIDAdder(new FixedMergeTCToJavaClassAdapter(cw, dsoAdapter,
                                                                                           jClassNameDots,
                                                                                           tcClassNameDots, tcCN,
                                                                                           instrumentedContext));
      jCR.accept(cv, ClassReader.SKIP_FRAMES);
      jData = cw.toByteArray();
      jData = doDSOTransform(jClassNameDots, jData);
      loadClassIntoJar(jClassNameDots, jData, true);
    }

    // java.util.concurrent.ConcurrentHashMap$HashEntry
    {
      byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$HashEntry");
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentHashMapHashEntryAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      bytes = cw.toByteArray();
      loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$HashEntry", bytes, false);
    }

    // java.util.concurrent.ConcurrentHashMap$Segment
    {
      byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$Segment");
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentHashMapSegmentAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);

      bytes = cw.toByteArray();

      final TransparencyClassSpec spec = this.configHelper
          .getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$Segment");
      bytes = doDSOTransform(spec.getClassName(), bytes);
      bytes = addNotClearableInterface(bytes);
      loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$Segment", bytes, spec.isPreInstrumented());
    }

    // java.util.concurrent.ConcurrentHashMap$ValueIterator
    {
      byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$ValueIterator");
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentHashMapValueIteratorAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      bytes = cw.toByteArray();
      loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$ValueIterator", bytes, false);
    }

    // java.util.concurrent.ConcurrentHashMap$EntryIterator
    {
      byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$EntryIterator");
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentHashMapEntryIteratorAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      bytes = cw.toByteArray();
      loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$EntryIterator", bytes, false);
    }

    if (Vm.isJDK16Compliant()) {
      // java.util.concurrent.ConcurrentHashMap$EntryIterator
      byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$WriteThroughEntry");
      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentHashMapWriteThroughEntryAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);

      bytes = cw.toByteArray();

      TransparencyClassSpec spec = this.configHelper
          .getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$WriteThroughEntry");
      spec.setHonorTransient(true);
      spec.markPreInstrumented();
      bytes = doDSOTransform(spec.getClassName(), bytes);
      loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$WriteThroughEntry", bytes, spec.isPreInstrumented());

      // java.util.AbstractMap$SimpleEntry
      bytes = getTerracottaBytes("java.util.AbstractMap$SimpleEntry");
      spec = this.configHelper.getOrCreateSpec("java.util.AbstractMap$SimpleEntry");
      bytes = doDSOTransform(spec.getClassName(), bytes);
      loadClassIntoJar("java.util.AbstractMap$SimpleEntry", bytes, spec.isPreInstrumented());
    }

    // com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper
    {
      byte[] bytes = getTerracottaBytes("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper");
      final TransparencyClassSpec spec = this.configHelper
          .getOrCreateSpec("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper");
      spec.markPreInstrumented();
      bytes = doDSOTransform(spec.getClassName(), bytes);
      loadClassIntoJar("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper", bytes,
                       spec.isPreInstrumented());
    }
  }

  private byte[] addNotClearableInterface(final byte[] bytes) {
    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, 0);
    cr.accept(new AddInterfacesAdapter(cw, new String[] { NotClearable.class.getName().replace('.', '/') }),
              ClassReader.SKIP_FRAMES);
    return cw.toByteArray();
  }

  private final void addInstrumentedJavaUtilConcurrentLinkedBlockingQueue() {
    if (!Vm.isJDK15Compliant()) { return; }

    { // Instrumentation for Itr inner class
      byte[] bytes = getSystemBytes("java.util.concurrent.LinkedBlockingQueue$Itr");

      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);

      bytes = cw.toByteArray();
      loadClassIntoJar("java.util.concurrent.LinkedBlockingQueue$Itr", bytes, true);
    }

    { // Instrumentation for Node inner class
      byte[] bytes = getSystemBytes("java.util.concurrent.LinkedBlockingQueue$Node");

      final ClassReader cr = new ClassReader(bytes);
      final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      final ClassVisitor cv = new JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter(cw);
      cr.accept(cv, ClassReader.SKIP_FRAMES);

      bytes = cw.toByteArray();
      loadClassIntoJar("java.util.concurrent.LinkedBlockingQueue$Node", bytes, true);
    }

    { // Instrumentation for LinkedBlockingQueue class
      final String jClassNameDots = "java.util.concurrent.LinkedBlockingQueue";
      final String tcClassNameDots = "java.util.concurrent.LinkedBlockingQueueTC";

      final byte[] tcData = getSystemBytes(tcClassNameDots);
      final ClassReader tcCR = new ClassReader(tcData);
      final ClassNode tcCN = new ClassNode();
      tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

      byte[] jData = getSystemBytes(jClassNameDots);
      final ClassReader jCR = new ClassReader(jData);
      final ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

      final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jClassNameDots, this.systemLoader);
      final TransparencyClassAdapter dsoAdapter = this.configHelper
          .createDsoClassAdapterFor(cw, jClassInfo, this.instrumentationLogger, getClass().getClassLoader(), true, true);
      final Map instrumentedContext = new HashMap();
      final ClassVisitor cv = new SerialVersionUIDAdder(
                                                        new JavaUtilConcurrentLinkedBlockingQueueClassAdapter(
                                                                                                              new FixedMergeTCToJavaClassAdapter(
                                                                                                                                                 cw,
                                                                                                                                                 dsoAdapter,
                                                                                                                                                 jClassNameDots,
                                                                                                                                                 tcClassNameDots,
                                                                                                                                                 tcCN,
                                                                                                                                                 instrumentedContext)));
      jCR.accept(cv, ClassReader.SKIP_FRAMES);
      jData = cw.toByteArray();

      final TransparencyClassSpec spec = this.configHelper
          .getOrCreateSpec(jClassNameDots, "com.tc.object.applicator.LinkedBlockingQueueApplicator");
      spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
      spec.markPreInstrumented();
      jData = doDSOTransform(spec.getClassName(), jData);
      loadClassIntoJar(spec.getClassName(), jData, true);
    }
  }

  private final void addInstrumentedJavaUtilConcurrentFutureTask() {

    if (!Vm.isJDK15Compliant()) { return; }
    final Map instrumentedContext = new HashMap();

    TransparencyClassSpec spec = this.configHelper.getOrCreateSpec("java.util.concurrent.FutureTask");
    spec.setHonorTransient(true);
    spec.setCallConstructorOnLoad(true);
    spec.markPreInstrumented();
    changeClassName("java.util.concurrent.FutureTaskTC", "java.util.concurrent.FutureTaskTC",
                    "java.util.concurrent.FutureTask", instrumentedContext, true);

    this.configHelper.addWriteAutolock("* java.util.concurrent.FutureTask$Sync.*(..)");

    spec = this.configHelper.getOrCreateSpec("java.util.concurrent.FutureTask$Sync");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    spec.addDistributedMethodCall("managedInnerCancel", "()V", true);
    changeClassName("java.util.concurrent.FutureTaskTC$Sync", "java.util.concurrent.FutureTaskTC",
                    "java.util.concurrent.FutureTask", instrumentedContext, true);
  }

  private final void addInstrumentedJavaUtilCollection() {
    TransparencyClassSpec spec = this.configHelper.getOrCreateSpec("java.util.HashSet",
                                                                   "com.tc.object.applicator.HashSetApplicator");
    spec.addIfTrueLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addMethodAdapter(SerializationUtil.REMOVE_SIGNATURE, new SetRemoveMethodAdapter("java/util/HashSet",
                                                                                         "java/util/HashMap", "map",
                                                                                         "java/util/HashMap"));
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addSetIteratorWrapperSpec(SerializationUtil.ITERATOR_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.LinkedHashSet", "com.tc.object.applicator.HashSetApplicator");
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.TreeSet", "com.tc.object.applicator.TreeSetApplicator");
    spec.addIfTrueLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addMethodAdapter(SerializationUtil.REMOVE_SIGNATURE,
                          new SetRemoveMethodAdapter("java/util/TreeSet", "java/util/TreeMap", "m", Vm
                              .getMajorVersion() >= 6 ? "java/util/NavigableMap" : "java/util/SortedMap"));

    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addSetIteratorWrapperSpec(SerializationUtil.ITERATOR_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.SUBSET_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.HEADSET_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.TAILSET_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.LinkedList", "com.tc.object.applicator.ListApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.ADD_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_FIRST_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_LAST_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_FIRST_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_LAST_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_RANGE_SIGNATURE);
    spec.addMethodAdapter("listIterator(I)Ljava/util/ListIterator;", new LinkedListAdapter.ListIteratorAdapter());
    spec.addMethodAdapter(SerializationUtil.REMOVE_SIGNATURE, new LinkedListAdapter.RemoveAdapter());
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.addSupportMethodCreator(new LinkedListAdapter.RemoveMethodCreator());
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.Vector", "com.tc.object.applicator.ListApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.INSERT_ELEMENT_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_AT_SIGNATURE);
    // the Vector.addAll(Collection) implementation in the IBM JDK simply delegates
    // to Vector.addAllAt(int, Collection), if addAll is instrumented as well, the
    // vector elements are added twice to the collection
    if (!Vm.isIBM()) {
      spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_SIGNATURE);
    }
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ELEMENT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_ALL_ELEMENTS_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_ELEMENT_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_RANGE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_ELEMENT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.TRIM_TO_SIZE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_SIZE_SIGNATURE);
    spec.addMethodAdapter(SerializationUtil.ELEMENTS_SIGNATURE, new VectorAdapter.ElementsAdapter());
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.COPY_INTO_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.Stack", "com.tc.object.applicator.ListApplicator");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.ArrayList", "com.tc.object.applicator.ListApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.ADD_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_RANGE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    if (Vm.isJDK15Compliant()) {
      spec.addMethodAdapter(SerializationUtil.REMOVE_SIGNATURE, new ArrayListAdapter.RemoveAdaptor());
      spec.addSupportMethodCreator(new ArrayListAdapter.FastRemoveMethodCreator());
    }
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.concurrent.CopyOnWriteArrayList",
                                             "com.tc.object.applicator.ListApplicator");
    spec.addMethodAdapter(SerializationUtil.ADD_SIGNATURE, new CopyOnWriteArrayListAdapter.AddAdaptor());
    spec.addMethodAdapter(SerializationUtil.ADD_AT_SIGNATURE, new CopyOnWriteArrayListAdapter.AddAtAdaptor());
    spec.addMethodAdapter(SerializationUtil.ADD_ALL_SIGNATURE, new CopyOnWriteArrayListAdapter.AddAllAdaptor());
    spec.addMethodAdapter(SerializationUtil.ADD_ALL_AT_SIGNATURE, new CopyOnWriteArrayListAdapter.AddAllAtAdaptor());
    spec.addMethodAdapter(CopyOnWriteArrayListAdapter.CONSTRUCTOR1_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.Jdk16LockAdaptor());
    spec.addMethodAdapter(CopyOnWriteArrayListAdapter.CONSTRUCTOR2_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.Jdk16LockAdaptor());
    spec.addMethodAdapter(CopyOnWriteArrayListAdapter.CONSTRUCTOR3_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.Jdk16LockAdaptor());
    spec.addMethodAdapter(SerializationUtil.ADD_IF_ABSENT_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.AddIfAbsentAdaptor());
    spec.addMethodAdapter(SerializationUtil.ADD_ALL_ABSENT_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.AddAllAbsentAdaptor());
    spec.addMethodAdapter(SerializationUtil.REMOVE_SIGNATURE, new CopyOnWriteArrayListAdapter.RemoveAdaptor());
    spec.addMethodAdapter(SerializationUtil.REMOVE_ALL_SIGNATURE, new CopyOnWriteArrayListAdapter.RemoveAllAdaptor());
    spec.addMethodAdapter(SerializationUtil.RETAIN_ALL_SIGNATURE, new CopyOnWriteArrayListAdapter.RetainAllAdaptor());
    spec.addMethodAdapter(SerializationUtil.REMOVE_AT_SIGNATURE, new CopyOnWriteArrayListAdapter.RemoveAtAdaptor());
    spec.addMethodAdapter(SerializationUtil.REMOVE_RANGE_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.RemoveRangeAdaptor());
    spec.addMethodAdapter(SerializationUtil.SET_SIGNATURE, new CopyOnWriteArrayListAdapter.SetAdaptor());
    spec.addMethodAdapter(SerializationUtil.CLEAR_SIGNATURE, new CopyOnWriteArrayListAdapter.ClearAdaptor());
    spec.addMethodAdapter(CopyOnWriteArrayListAdapter.RESET_LOCK_SIGNATURE,
                          new CopyOnWriteArrayListAdapter.ResetLockAdaptor());
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = this.configHelper.getOrCreateSpec("java.util.concurrent.CopyOnWriteArraySet");
    addSerializationInstrumentedCode(spec);
  }

  private final void addSerializationInstrumentedCode(final TransparencyClassSpec spec) {
    byte[] bytes = getSystemBytes(spec.getClassName());
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);

    final ClassReader cr = new ClassReader(bytes);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new LogicalClassSerializationAdapter.LogicalClassSerializationClassAdapter(
                                                                                                       cw,
                                                                                                       spec.getClassName());
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    bytes = cw.toByteArray();
    loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private final void addInstrumentedHashtable() {
    final String jMapClassNameDots = "java.util.Hashtable";
    final String tcMapClassNameDots = "java.util.HashtableTC";
    final Map instrumentedContext = new HashMap();
    mergeClass(tcMapClassNameDots, jMapClassNameDots, instrumentedContext, HashtableClassAdapter.getMethods(), null);
  }

  private final void addInstrumentedLinkedHashMap(final Map instrumentedContext) {
    final String jMapClassNameDots = "java.util.LinkedHashMap";
    final String tcMapClassNameDots = "java.util.LinkedHashMapTC";

    mergeClass(tcMapClassNameDots, jMapClassNameDots, instrumentedContext, null,
               new ClassAdapterFactory[] { LinkedHashMapClassAdapter.FACTORY });
  }

  private void addInstrumentedReentrantReadWriteLock() {
    final String methodPrefix = "__RWL" + ByteCodeUtil.TC_METHOD_PREFIX;

    final String jClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLock";
    final String tcClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLockTC";
    Map instrumentedContext = new HashMap();
    mergeReentrantReadWriteLock(tcClassNameDots, jClassNameDots, instrumentedContext, methodPrefix);

    String jInnerClassNameDots;
    String tcInnerClassNameDots;

    jInnerClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock";
    tcInnerClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLockTC$ReadLock";
    instrumentedContext = new HashMap();
    mergeReadWriteLockInnerClass(tcInnerClassNameDots, jInnerClassNameDots, tcClassNameDots, jClassNameDots,
                                 "ReadLock", "ReadLock", instrumentedContext, methodPrefix);

    jInnerClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock";
    tcInnerClassNameDots = "java.util.concurrent.locks.ReentrantReadWriteLockTC$WriteLock";
    instrumentedContext = new HashMap();
    mergeReadWriteLockInnerClass(tcInnerClassNameDots, jInnerClassNameDots, tcClassNameDots, jClassNameDots,
                                 "WriteLock", "WriteLock", instrumentedContext, methodPrefix);

  }

  private void mergeReadWriteLockInnerClass(final String tcInnerClassNameDots, final String jInnerClassNameDots,
                                            final String tcClassNameDots, final String jClassNameDots,
                                            final String srcInnerClassName, final String targetInnerClassName,
                                            final Map instrumentedContext, final String methodPrefix) {
    final String tcInnerClassNameSlashes = tcInnerClassNameDots
        .replace(ChangeClassNameHierarchyAdapter.DOT_DELIMITER, ChangeClassNameHierarchyAdapter.SLASH_DELIMITER);
    final byte[] tcData = getSystemBytes(tcInnerClassNameDots);
    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    byte[] jData = getSystemBytes(jInnerClassNameDots);

    // jData = doDSOTransform(jInnerClassNameDots, jData);

    final ClassReader jCR = new ClassReader(jData);
    final ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

    final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jInnerClassNameDots, this.systemLoader);
    final TransparencyClassAdapter dsoAdapter = this.configHelper.createDsoClassAdapterFor(cw, jClassInfo,
                                                                                           this.instrumentationLogger,
                                                                                           getClass().getClassLoader(),
                                                                                           true, false);
    final ClassVisitor cv = new SerialVersionUIDAdder(new FixedMergeTCToJavaClassAdapter(cw, dsoAdapter,
                                                                                         jInnerClassNameDots,
                                                                                         tcInnerClassNameDots, tcCN,
                                                                                         instrumentedContext,
                                                                                         methodPrefix, false));
    jCR.accept(cv, ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();

    jData = changeClassNameAndGetBytes(jData, tcInnerClassNameSlashes, tcClassNameDots, jClassNameDots,
                                       srcInnerClassName, targetInnerClassName, instrumentedContext);

    jData = doDSOTransform(jInnerClassNameDots, jData);
    loadClassIntoJar(jInnerClassNameDots, jData, true);
  }

  private void mergeReentrantReadWriteLock(final String tcClassNameDots, final String jClassNameDots,
                                           final Map instrumentedContext, final String methodPrefix) {
    final byte[] tcData = getSystemBytes(tcClassNameDots);
    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    byte[] jData = getSystemBytes(jClassNameDots);

    // jData = doDSOTransform(jClassNameDots, jData);

    ClassReader jCR = new ClassReader(jData);
    ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv1 = new ReentrantReadWriteLockClassAdapter(cw);

    jCR.accept(cv1, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();

    jCR = new ClassReader(jData);
    cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

    final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jClassNameDots, this.systemLoader);
    final TransparencyClassAdapter dsoAdapter = this.configHelper.createDsoClassAdapterFor(cw, jClassInfo,
                                                                                           this.instrumentationLogger,
                                                                                           getClass().getClassLoader(),
                                                                                           true, true);
    final ClassVisitor cv = new SerialVersionUIDAdder(new FixedMergeTCToJavaClassAdapter(cw, dsoAdapter,
                                                                                         jClassNameDots,
                                                                                         tcClassNameDots, tcCN,
                                                                                         instrumentedContext,
                                                                                         methodPrefix, true));
    jCR.accept(cv, ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();
    jData = doDSOTransform(jClassNameDots, jData);
    loadClassIntoJar(jClassNameDots, jData, true);

    final String innerClassName = "java/util/concurrent/locks/ReentrantReadWriteLockTC$DsoLock";
    changeClassNameAndGetBytes(innerClassName, tcClassNameDots, jClassNameDots, instrumentedContext);
    changeClassName(innerClassName, tcClassNameDots, jClassNameDots, instrumentedContext, true);
  }

  private void addInstrumentedHashMap() {
    final Map instrumentedContext = new HashMap();
    mergeClass(HashMapClassAdapter.TC_MAP_CLASSNAME_DOTS, HashMapClassAdapter.J_MAP_CLASSNAME_DOTS,
               instrumentedContext, null, new ClassAdapterFactory[] { HashMapClassAdapter.FACTORY });

    addInstrumentedLinkedHashMap(instrumentedContext);
  }

  private final void mergeClass(final String tcClassNameDots, final String jClassNameDots,
                                final Map instrumentedContext, final MethodNode[] replacedMethods,
                                final ClassAdapterFactory[] addlAdapters) {
    final byte[] tcData = getSystemBytes(tcClassNameDots);

    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode() {
      @Override
      public MethodVisitor visitMethod(final int maccess, final String mname, final String mdesc,
                                       final String msignature, final String[] mexceptions) {
        if (replacedMethods != null) {
          for (final MethodNode replacedMethod : replacedMethods) {
            if (mname.equals(replacedMethod.name) && mdesc.equals(replacedMethod.desc)) {
              this.methods.add(replacedMethod);
              return null;
            }
          }
        }
        return super.visitMethod(maccess, mname, mdesc, msignature, mexceptions);
      }
    };
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    byte[] jData = getSystemBytes(jClassNameDots);

    if (addlAdapters != null) {
      for (final ClassAdapterFactory addlAdapter : addlAdapters) {
        final ClassReader cr = new ClassReader(jData);
        final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

        final ClassVisitor cv = addlAdapter.create(cw, null);
        cr.accept(cv, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        jData = cw.toByteArray();
      }
    }

    final ClassReader jCR = new ClassReader(jData);
    final ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);
    final ClassNode jCN = new ClassNode();
    jCR.accept(jCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jClassNameDots, this.systemLoader);
    final TransparencyClassAdapter dsoAdapter = this.configHelper.createDsoClassAdapterFor(cw, jClassInfo,
                                                                                           this.instrumentationLogger,
                                                                                           getClass().getClassLoader(),
                                                                                           true, false);
    final ClassVisitor cv = new SerialVersionUIDAdder(new FixedMergeTCToJavaClassAdapter(cw, dsoAdapter,
                                                                                         jClassNameDots,
                                                                                         tcClassNameDots, tcCN,
                                                                                         instrumentedContext));
    jCR.accept(cv, ClassReader.SKIP_FRAMES);
    loadClassIntoJar(jClassNameDots, cw.toByteArray(), true);

    final List innerClasses = tcCN.innerClasses;
    // load ClassInfo for all inner classes
    for (final Iterator i = innerClasses.iterator(); i.hasNext();) {
      final InnerClassNode innerClass = (InnerClassNode) i.next();

      if (innerClass.outerName.equals(tcClassNameDots.replace(ChangeClassNameHierarchyAdapter.DOT_DELIMITER,
                                                              ChangeClassNameHierarchyAdapter.SLASH_DELIMITER))) {
        changeClassNameAndGetBytes(innerClass.name, tcClassNameDots, jClassNameDots, instrumentedContext);
      }
    }
    // transform and add inner classes to the boot jar
    for (final Iterator i = innerClasses.iterator(); i.hasNext();) {
      final InnerClassNode innerClass = (InnerClassNode) i.next();
      if (innerClass.outerName.equals(tcClassNameDots.replace(ChangeClassNameHierarchyAdapter.DOT_DELIMITER,
                                                              ChangeClassNameHierarchyAdapter.SLASH_DELIMITER))) {
        changeClassName(innerClass.name, tcClassNameDots, jClassNameDots, instrumentedContext,
                        mergedInnerClassesNeedInstrumentation(jClassNameDots));
      }
    }
  }

  private boolean mergedInnerClassesNeedInstrumentation(final String classNameDots) {
    return classNameDots.equals("java.util.Hashtable");
  }

  private void changeClassName(final String fullClassNameDots, final String classNameDotsToBeChanged,
                               final String classNameDotsReplaced, final Map instrumentedContext,
                               final boolean doDSOTransform) {
    byte[] data = changeClassNameAndGetBytes(fullClassNameDots, classNameDotsToBeChanged, classNameDotsReplaced,
                                             instrumentedContext);

    final String replacedClassName = ChangeClassNameRootAdapter.replaceClassName(fullClassNameDots,
                                                                                 classNameDotsToBeChanged,
                                                                                 classNameDotsReplaced, null, null);

    if (doDSOTransform) {
      data = doDSOTransform(replacedClassName, data);
    }

    loadClassIntoJar(replacedClassName, data, true);
  }

  private final byte[] changeClassNameAndGetBytes(final String fullClassNameDots,
                                                  final String classNameDotsToBeChanged,
                                                  final String classNameDotsReplaced, final Map instrumentedContext) {
    return changeClassNameAndGetBytes(fullClassNameDots, classNameDotsToBeChanged, classNameDotsReplaced, null, null,
                                      instrumentedContext);
  }

  private final byte[] changeClassNameAndGetBytes(final String fullClassNameDots,
                                                  final String classNameDotsToBeChanged,
                                                  final String classNameDotsReplaced, final String srcInnerClassName,
                                                  final String targetInnerClassName, final Map instrumentedContext) {
    return changeClassNameAndGetBytes(getSystemBytes(fullClassNameDots), fullClassNameDots, classNameDotsToBeChanged,
                                      classNameDotsReplaced, srcInnerClassName, targetInnerClassName,
                                      instrumentedContext);
  }

  private final byte[] changeClassNameAndGetBytes(byte[] data, final String fullClassNameDots,
                                                  final String classNameDotsToBeChanged,
                                                  final String classNameDotsReplaced, final String srcInnerClassName,
                                                  final String targetInnerClassName, final Map instrumentedContext) {
    final ClassReader cr = new ClassReader(data);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new ChangeClassNameRootAdapter(cw, fullClassNameDots, classNameDotsToBeChanged,
                                                           classNameDotsReplaced, srcInnerClassName,
                                                           targetInnerClassName, instrumentedContext, null);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    data = cw.toByteArray();

    AsmClassInfo.getClassInfo(classNameDotsReplaced, data, this.systemLoader);

    return data;
  }

  private void addInstrumentedJavaUtilConcurrentLocks() {
    if (!Vm.isJDK15Compliant()) { return; }
    addInstrumentedReentrantReadWriteLock();
    addInstrumentedReentrantLock();
    addInstrumentedConditionObject();
  }

  private void addInstrumentedConditionObject() {
    String classNameDots = "com.tcclient.util.concurrent.locks.ConditionObject";
    byte[] bytes = getSystemBytes(classNameDots);
    TransparencyClassSpec spec = this.configHelper.getOrCreateSpec(classNameDots);
    spec.disableWaitNotifyCodeSpec("signal()V");
    spec.disableWaitNotifyCodeSpec("signalAll()V");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    bytes = doDSOTransform(classNameDots, bytes);
    loadClassIntoJar(classNameDots, bytes, spec.isPreInstrumented());
    this.configHelper.removeSpec(classNameDots);

    classNameDots = "com.tcclient.util.concurrent.locks.ConditionObject$SyncCondition";
    bytes = getSystemBytes(classNameDots);
    spec = this.configHelper.getOrCreateSpec(classNameDots);
    spec.markPreInstrumented();
    bytes = doDSOTransform(classNameDots, bytes);
    loadClassIntoJar(classNameDots, bytes, spec.isPreInstrumented());
    this.configHelper.removeSpec(classNameDots);

  }

  private void addInstrumentedReentrantLock() {
    final String jClassNameDots = "java.util.concurrent.locks.ReentrantLock";
    final String tcClassNameDots = "java.util.concurrent.locks.ReentrantLockTC";
    final Map instrumentedContext = new HashMap();
    mergeReentrantLock(tcClassNameDots, jClassNameDots, instrumentedContext);
  }

  private void mergeReentrantLock(final String tcClassNameDots, final String jClassNameDots,
                                  final Map instrumentedContext) {
    final String methodPrefix = "__RL" + ByteCodeUtil.TC_METHOD_PREFIX;

    final byte[] tcData = getSystemBytes(tcClassNameDots);
    final ClassReader tcCR = new ClassReader(tcData);
    final ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

    byte[] jData = getSystemBytes(jClassNameDots);
    ClassReader jCR = new ClassReader(jData);
    ClassWriter cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv1 = new ReentrantLockClassAdapter(cw);

    jCR.accept(cv1, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();

    jCR = new ClassReader(jData);
    cw = new ClassWriter(jCR, ClassWriter.COMPUTE_MAXS);

    final ClassInfo jClassInfo = AsmClassInfo.getClassInfo(jClassNameDots, this.systemLoader);
    final TransparencyClassAdapter dsoAdapter = this.configHelper.createDsoClassAdapterFor(cw, jClassInfo,
                                                                                           this.instrumentationLogger,
                                                                                           getClass().getClassLoader(),
                                                                                           true, true);
    final ClassVisitor cv = new SerialVersionUIDAdder(new FixedMergeTCToJavaClassAdapter(cw, dsoAdapter,
                                                                                         jClassNameDots,
                                                                                         tcClassNameDots, tcCN,
                                                                                         instrumentedContext,
                                                                                         methodPrefix, true));
    jCR.accept(cv, ClassReader.SKIP_FRAMES);
    jData = cw.toByteArray();
    jData = doDSOTransform(jClassNameDots, jData);
    loadClassIntoJar(jClassNameDots, jData, true);
  }

  private final void addInstrumentedClassLoader() {
    // patch the java.lang.ClassLoader
    final ClassLoaderPreProcessorImpl adapter = new ClassLoaderPreProcessorImpl();
    final byte[] patched = adapter.preProcess(getSystemBytes("java.lang.ClassLoader"));

    final ClassReader cr = new ClassReader(patched);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassVisitor cv = new NamedLoaderAdapter().create(cw, null);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    loadClassIntoJar("java.lang.ClassLoader", cw.toByteArray(), false);
  }

  protected final byte[] doDSOTransform(final String name, final byte[] data) {
    // adapts the class on the fly
    final ClassReader cr = new ClassReader(data);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    final ClassInfo classInfo = AsmClassInfo.getClassInfo(name, data, this.tcLoader);
    final ClassVisitor cv = this.configHelper.createClassAdapterFor(cw, classInfo, this.instrumentationLogger,
                                                                    getClass().getClassLoader(), true);
    cr.accept(cv, ClassReader.SKIP_FRAMES);

    return cw.toByteArray();
  }

  private final void adaptClassIfNotAlreadyIncluded(final String className, final Class adapter) {
    if (this.bootJar.classLoaded(className)) { return; }

    final byte[] orig = getSystemBytes(className);

    final ClassReader cr = new ClassReader(orig);
    final ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);

    try {
      final ClassVisitor cv = (ClassVisitor) adapter.getConstructor(new Class[] { ClassVisitor.class })
          .newInstance(new Object[] { cw });
      cr.accept(cv, ClassReader.SKIP_FRAMES);
      loadClassIntoJar(className, cw.toByteArray(), false);
    } catch (final Exception e) {
      exit("Can't instaniate class apapter using class " + adapter, e);
    }
  }

  protected void announce(final String msg) {
    if (!this.quiet) {
      consoleLogger.info(msg);
    }
  }

  private final static File getInstallationDir() {
    try {
      return Directories.getInstallationRoot();
    } catch (final FileNotFoundException fnfe) {
      return null;
    }
  }

  private final static void showHelpAndExit(final Options options, final int code) {
    new HelpFormatter().printHelp("java " + BootJarTool.class.getName() + " " + MAKE_OR_SCAN_MODE, options);
    System.exit(code);
  }

  private static final String MAKE_MODE         = "make";
  private static final String SCAN_MODE         = "scan";
  private static final String MAKE_OR_SCAN_MODE = "<" + MAKE_MODE + "|" + SCAN_MODE + ">";

  public final static void main(final String[] args) {
    final File installDir = getInstallationDir();
    final String outputFileOptionMsg = "path to boot JAR file"
                                       + (installDir != null ? "\ndefault: [TC_INSTALL_DIR]/lib/dso-boot" : "");
    final Option targetFileOption = new Option(TARGET_FILE_OPTION, true, outputFileOptionMsg);
    targetFileOption.setArgName("file");
    targetFileOption.setLongOpt("bootjar-file");
    targetFileOption.setArgs(1);
    targetFileOption.setRequired(installDir == null);
    targetFileOption.setType(String.class);

    final Option configFileOption = new Option("f", "config", true, "configuration file (optional)");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);
    configFileOption.setRequired(false);

    final Option overwriteOption = new Option("w", "overwrite", false, "always make the boot JAR file");
    overwriteOption.setType(String.class);
    overwriteOption.setRequired(false);

    final Option verboseOption = new Option("v", "verbose");
    verboseOption.setType(String.class);
    verboseOption.setRequired(false);

    final Option helpOption = new Option("h", "help");
    helpOption.setType(String.class);
    helpOption.setRequired(false);

    final Options options = new Options();
    options.addOption(targetFileOption);
    options.addOption(configFileOption);
    options.addOption(overwriteOption);
    options.addOption(verboseOption);
    options.addOption(helpOption);

    String mode = MAKE_MODE;
    CommandLine cmdLine = null;
    try {
      cmdLine = new PosixParser().parse(options, args);
      mode = (cmdLine.getArgList().size() > 0) ? cmdLine.getArgList().get(0).toString().toLowerCase() : mode;
    } catch (final ParseException pe) {
      showHelpAndExit(options, 1);
    }

    if (cmdLine.hasOption("h") || (!mode.equals(MAKE_MODE) && !mode.equals(SCAN_MODE))) {
      showHelpAndExit(options, 1);
    }

    try {
      if (!cmdLine.hasOption("f")
          && System.getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME) == null) {
        final String cwd = System.getProperty("user.dir");
        final File localConfig = new File(cwd, DEFAULT_CONFIG_SPEC);
        final String configSpec = localConfig.exists() ? localConfig.getAbsolutePath()
            : StandardConfigurationSetupManagerFactory.DEFAULT_CONFIG_URI;
        final String[] newArgs = new String[args.length + 2];
        System.arraycopy(args, 0, newArgs, 0, args.length);
        newArgs[newArgs.length - 2] = "-f";
        newArgs[newArgs.length - 1] = configSpec;
        cmdLine = new PosixParser().parse(options, newArgs);
      }

      StandardConfigurationSetupManagerFactory factory;
      factory = new StandardConfigurationSetupManagerFactory(
                                                             cmdLine,
                                                             StandardConfigurationSetupManagerFactory.ConfigMode.CUSTOM_L1,
                                                             new FatalIllegalConfigurationChangeHandler());
      final boolean verbose = cmdLine.hasOption("v");
      final TCLogger logger = verbose ? CustomerLogging.getConsoleLogger() : new NullTCLogger();
      final L1ConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager(logger);

      File targetFile;
      if (cmdLine.hasOption(TARGET_FILE_OPTION)) {
        targetFile = new File(cmdLine.getOptionValue(TARGET_FILE_OPTION)).getAbsoluteFile();
      } else {
        targetFile = new File(new File(installDir, "lib"), "dso-boot");
        FileUtils.forceMkdir(targetFile);
      }

      if (targetFile.isDirectory()) {
        targetFile = new File(targetFile, BootJarSignature.getBootJarNameForThisVM());
      }

      // This used to be a provider that read from a specified rt.jar (to let us create boot jars for other platforms).
      // That requirement is no more, but might come back, so I'm leaving at least this much scaffolding in place
      // WAS: systemProvider = new RuntimeJarBytesProvider(...)
      final ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
      BootJarTool bjTool = new BootJarTool(new StandardDSOClientConfigHelperImpl(config, false), targetFile,
                                           systemLoader, !verbose);
      if (mode.equals(MAKE_MODE)) {
        boolean validating = false;
        final boolean makeItAnyway = cmdLine.hasOption("w");
        if (targetFile.exists()) {
          if (makeItAnyway) {
            consoleLogger.info("Overwrite mode specified, existing boot JAR file at '" + targetFile.getCanonicalPath()
                               + "' will be overwritten.");
          } else {
            consoleLogger.info("Found boot JAR file at '" + targetFile.getCanonicalPath() + "'; validating...");
            validating = true;
          }
        }
        if (makeItAnyway || !targetFile.exists() || (targetFile.exists() && !bjTool.isBootJarComplete(targetFile))) {
          // Don't reuse boot jar tool instance since its config might have been mutated by isBootJarComplete()
          bjTool = new BootJarTool(new StandardDSOClientConfigHelperImpl(config, false), targetFile, systemLoader,
                                   !verbose);
          bjTool.generateJar();
        }
        bjTool.verifyJar(targetFile);
        if (validating) {
          consoleLogger.info("Valid.");
        }
      } else if (mode.equals(SCAN_MODE)) {
        consoleLogger.info("Scanning boot JAR file at '" + targetFile.getCanonicalPath() + "'...");
        bjTool.scanJar(targetFile);
        consoleLogger.info("Done.");
      } else {
        consoleLogger.fatal("\nInvalid mode specified, valid modes are: '" + MAKE_MODE + "' and '" + SCAN_MODE + "';"
                            + "use the -h option to view the options for this tool.");
        System.exit(1);
      }
    } catch (final Exception e) {
      // See CDV-835, DEV-2792: for some messages it would be better to print e.getMessage() but
      // for others the message is not sufficient and we need the entire stack trace.
      consoleLogger.fatal("BootJarTool: ", e);
      System.exit(1);
    }
  }
}
