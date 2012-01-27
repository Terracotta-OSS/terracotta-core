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
import com.tc.management.TunneledDomainUpdater;
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
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfCompilationHelper;
import com.tc.object.TCObjectSelfImpl;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.TCObjectServerMap;
import com.tc.object.TraversedReferences;
import com.tc.object.bytecode.AAFairDistributionPolicyMarker;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.CloneUtil;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.NullTCObject;
import com.tc.object.bytecode.TCMap;
import com.tc.object.bytecode.TCServerMap;
import com.tc.object.bytecode.TransparentAccess;
import com.tc.object.bytecode.hook.ClassLoaderPreProcessorImpl;
import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.ClassProcessor;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelperJDK15;
import com.tc.object.bytecode.hook.impl.Util;
import com.tc.object.cache.Cacheable;
import com.tc.object.compression.CompressedData;
import com.tc.object.compression.StringCompressionUtil;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.ModuleConfiguration;
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
import com.tc.object.loaders.BytecodeProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.Namespace;
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
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStoreListener;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreFullException;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.session.SessionID;
import com.tc.object.tx.ClientTransaction;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionContext;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnBatchID;
import com.tc.object.tx.TxnType;
import com.tc.object.tx.UnlockedSharedObjectException;
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
import com.tc.util.SequenceID;
import com.tc.util.SequenceID.SequenceIDComparator;
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

import gnu.trove.TLinkable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
      ModulesLoader.initModules(this.configHelper, null, true);
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

      loadTerracottaClass(DebugUtil.class.getName());
      loadTerracottaClass(TCMap.class.getName());
      loadTerracottaClass(com.tc.util.Stack.class.getName());
      loadTerracottaClass(TCObjectNotSharableException.class.getName());
      loadTerracottaClass(TCObjectNotFoundException.class.getName());
      loadTerracottaClass(TCNonPortableObjectError.class.getName());
      loadTerracottaClass(TCError.class.getName());
      loadTerracottaClass(TCNotRunningException.class.getName());
      loadTerracottaClass(TerracottaOperatorEvent.EventType.class.getName());
      loadTerracottaClass(TerracottaOperatorEvent.EventSubsystem.class.getName());

      loadTerracottaClass(TransparentAccess.class.getName());
      loadTerracottaClass(BytecodeProvider.class.getName());

      loadTerracottaClass(ModuleConfiguration.class.getName());
      loadTerracottaClass(Manageable.class.getName());
      loadTerracottaClass(AAFairDistributionPolicyMarker.class.getName());
      loadTerracottaClass(Clearable.class.getName());
      loadTerracottaClass(NotClearable.class.getName());
      loadTerracottaClass(TCServerMap.class.getName());
      loadTerracottaClass(IndexQueryResult.class.getName());
      loadTerracottaClass(SearchQueryResults.class.getName());
      loadTerracottaClass(ExpirableEntry.class.getName());
      loadTerracottaClass(Manager.class.getName());
      loadTerracottaClass(TunneledDomainUpdater.class.getName());
      loadTerracottaClass(InstrumentationLogger.class.getName());
      loadTerracottaClass(NullInstrumentationLogger.class.getName());
      loadTerracottaClass(NullManager.class.getName());
      loadTerracottaClass(NullTCLogger.class.getName());
      loadTerracottaClass(ManagerUtil.class.getName());
      loadTerracottaClass(TCObject.class.getName());
      loadTerracottaClassesReachableFromTCObject();
      loadTerracottaClassesForTCObjectSelf();
      loadTerracottaClass(TCObjectServerMap.class.getName());
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
      loadTerracottaClass(TCNotSupportedMethodException.class.getName());
      loadTerracottaClass(ExceptionWrapper.class.getName());
      loadTerracottaClass(ExceptionWrapperImpl.class.getName());
      loadTerracottaClass(Os.class.getName());
      loadTerracottaClass(Util.class.getName());
      loadTerracottaClass(NIOWorkarounds.class.getName());
      loadTerracottaClass(TCProperties.class.getName());
      loadTerracottaClass(StringCompressionUtil.class.getName());
      loadTerracottaClass(CompressedData.class.getName());
      loadTerracottaClass(TCByteArrayOutputStream.class.getName());
      loadTerracottaClass(MetaDataDescriptor.class.getName());

      loadTerracottaClass("com.tc.object.bytecode.hook.impl.ArrayManager");
      loadTerracottaClass(ProxyInstance.class.getName());

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
      loadTerracottaClass(com.tc.io.TCSerializable.class.getName());

      addManagementClasses();

      addRuntimeClasses();

      // local cache store classes
      loadTerracottaClass(LocalCacheStoreFullException.class.getName());
      loadTerracottaClass(ServerMapLocalCache.class.getName());
      loadTerracottaClass(L1ServerMapLocalCacheStore.class.getName());
      loadTerracottaClass(AbstractLocalCacheStoreValue.class.getName());
      loadTerracottaClass(TCObjectSelfStore.class.getName());
      loadTerracottaClass(LocalCacheStoreEventualValue.class.getName());
      loadTerracottaClass(LocalCacheStoreStrongValue.class.getName());

      loadTerracottaClass(L1ServerMapLocalCacheStoreListener.class.getName());

      addInstrumentedClassLoader();

      addClusterEventsAndMetaDataClasses();
      loadTerracottaClass(StatisticRetrievalAction.class.getName());
      loadTerracottaClass(StatisticType.class.getName());
      loadTerracottaClass(StatisticData.class.getName());
      loadTerracottaClass(StatisticDataCSVParser.class.getName());
      loadTerracottaClass(LazilyInitializedSRA.class.getName());

      final Map internalSpecs = getTCSpecs();
      loadBootJarClasses(removeAlreadyLoaded(massageSpecs(internalSpecs, true)));

      final Map userSpecs = massageSpecs(getUserDefinedSpecs(internalSpecs), false);
      issueWarningsAndErrors();

      // user defined specs should ALWAYS be after internal specs
      loadBootJarClasses(removeAlreadyLoaded(userSpecs), true);
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
    loadTerracottaClass(TCObjectSelfCompilationHelper.class.getName());
  }

  private void loadTerracottaClassesReachableFromTCObject() {
    // the following classes are referenced from TCObject
    // commented ones are those which are already added elsewhere

    // loadTerracottaClass(AbstractIdentifier.class.getName());
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

  private final void addManagementClasses() {
    loadTerracottaClass(TerracottaMBean.class.getName());
  }

  private final void addRuntimeClasses() {
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

  private final void addInstrumentedClassLoader() {
    // patch the java.lang.ClassLoader
    final ClassLoaderPreProcessorImpl adapter = new ClassLoaderPreProcessorImpl();
    final byte[] patched = adapter.preProcess(getSystemBytes("java.lang.ClassLoader"));

    loadClassIntoJar("java.lang.ClassLoader", patched, false);
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
