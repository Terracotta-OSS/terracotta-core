/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.knopflerfish.framework.BundleClassLoader;
import org.osgi.framework.Bundle;
import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerInfo;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;
import org.terracotta.license.LicenseException;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.backport175.bytecode.AnnotationElement.Annotation;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.injection.DsoClusterInjectionInstrumentation;
import com.tc.injection.InjectionInstrumentation;
import com.tc.injection.InjectionInstrumentationRegistry;
import com.tc.injection.exceptions.UnsupportedInjectedDsoInstanceTypeException;
import com.tc.jam.transform.ReflectClassBuilderAdapter;
import com.tc.license.LicenseManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.ConnectionInfo;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.AQSSubclassStrongReferenceAdapter;
import com.tc.object.bytecode.AbstractListMethodCreator;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterBase;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.JavaUtilConcurrentLocksAQSAdapter;
import com.tc.object.bytecode.OverridesHashCodeAdapter;
import com.tc.object.bytecode.SafeSerialVersionUIDAdder;
import com.tc.object.bytecode.SessionConfiguration;
import com.tc.object.bytecode.THashMapAdapter;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TreeMapAdapter;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
import com.tc.object.bytecode.hook.impl.ClassProcessorHelper;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.schema.DSOApplicationConfig;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.tools.BootJar;
import com.tc.object.tools.BootJarException;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ProductInfo;
import com.tc.util.UUID;
import com.tc.util.ClassUtils.ClassSpec;
import com.tc.util.runtime.Vm;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.L1ReconnectPropertiesDocument;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StandardDSOClientConfigHelperImpl implements StandardDSOClientConfigHelper, DSOClientConfigHelper {

  private static final String                                CGLIB_PATTERN                      = "$$EnhancerByCGLIB$$";

  private static final TCLogger                              logger                             = CustomerLogging
                                                                                                    .getDSOGenericLogger();
  private static final TCLogger                              consoleLogger                      = CustomerLogging
                                                                                                    .getConsoleLogger();
  private static final InstrumentationDescriptor             DEFAULT_INSTRUMENTATION_DESCRIPTOR = new NullInstrumentationDescriptor();

  private final DSOClientConfigHelperLogger                  helperLogger;
  private final L1ConfigurationSetupManager                  configSetupManager;
  private final UUID                                         id;
  private final Map                                          classLoaderNameToAppGroup          = new ConcurrentHashMap();
  private final Map                                          webAppNameToAppGroup               = new ConcurrentHashMap();
  private final List                                         locks                              = new CopyOnWriteArrayList();
  private final List                                         roots                              = new CopyOnWriteArrayList();
  private final Set                                          transients                         = Collections
                                                                                                    .synchronizedSet(new HashSet());
  private final Map<String, String>                          injectedFields                     = new ConcurrentHashMap<String, String>();
  private final Map<String, SessionConfiguration>            webApplications                    = Collections
                                                                                                    .synchronizedMap(new HashMap());
  private final CompoundExpressionMatcher                    permanentExcludesMatcher;
  private final CompoundExpressionMatcher                    nonportablesMatcher;
  private final List                                         autoLockExcludes                   = new CopyOnWriteArrayList();
  private final List                                         distributedMethods                 = new CopyOnWriteArrayList();
  private final ExpressionHelper                             expressionHelper;
  private final Map                                          adaptableCache                     = Collections
                                                                                                    .synchronizedMap(new HashMap());
  private final Set<TimCapability>                           timCapabilities                    = Collections
                                                                                                    .synchronizedSet(EnumSet
                                                                                                        .noneOf(TimCapability.class));

  /**
   * A list of InstrumentationDescriptor representing include/exclude patterns
   */
  private final List                                         instrumentationDescriptors         = new CopyOnWriteArrayList();

  // ====================================================================================================================
  /**
   * The lock for both {@link #userDefinedBootSpecs} and {@link #classSpecs} Maps
   */
  private final Object                                       specLock                           = new Object();

  /**
   * A map of class names to TransparencyClassSpec
   * 
   * @GuardedBy {@link #specLock}
   */
  private final Map                                          userDefinedBootSpecs               = new HashMap();

  /**
   * A map of class names to TransparencyClassSpec for individual classes
   * 
   * @GuardedBy {@link #specLock}
   */
  private final Map                                          classSpecs                         = new HashMap();
  // ====================================================================================================================

  private final Map<String, Collection<ClassAdapterFactory>> customAdapters                     = new HashMap<String, Collection<ClassAdapterFactory>>();
  private final ClassReplacementMapping                      classReplacements                  = new ClassReplacementMappingImpl();
  private final Map<String, Resource>                        classResources                     = new ConcurrentHashMap<String, Resource>();
  private final Map                                          aspectModules                      = new ConcurrentHashMap();
  private final boolean                                      supportSharingThroughReflection;
  private final Portability                                  portability;
  private int                                                faultCount                         = -1;
  private final Collection<ModuleSpec>                       moduleSpecs                        = Collections
                                                                                                    .synchronizedList(new ArrayList<ModuleSpec>());
  private MBeanSpec[]                                        mbeanSpecs                         = null;
  private SRASpec[]                                          sraSpecs                           = null;
  private final Set<String>                                  tunneledMBeanDomains               = Collections
                                                                                                    .synchronizedSet(new HashSet<String>());
  private final ModulesContext                               modulesContext                     = new ModulesContext();
  private ReconnectConfig                                    l1ReconnectConfig                  = null;
  private final InjectionInstrumentationRegistry             injectionRegistry                  = new InjectionInstrumentationRegistry();
  private final boolean                                      hasBootJar;
  private final Map<Bundle, URL>                             bundleURLs                         = new ConcurrentHashMap<Bundle, URL>();

  public StandardDSOClientConfigHelperImpl(final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
  }

  public StandardDSOClientConfigHelperImpl(final boolean initializedModulesOnlyOnce,
                                           final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
    if (initializedModulesOnlyOnce) {
      modulesContext.initializedModulesOnlyOnce();
    }
  }

  public StandardDSOClientConfigHelperImpl(final L1ConfigurationSetupManager configSetupManager,
                                           final boolean hasBootJar) throws ConfigurationSetupException {
    this.hasBootJar = hasBootJar;
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;
    this.id = UUID.getUUID();
    helperLogger = new DSOClientConfigHelperLogger(logger);
    // this.classInfoFactory = new ClassInfoFactory();
    this.expressionHelper = new ExpressionHelper();
    modulesContext.setModules(configSetupManager.commonL1Config().modules() != null ? configSetupManager
        .commonL1Config().modules() : Modules.Factory.newInstance());

    permanentExcludesMatcher = new CompoundExpressionMatcher();

    injectionRegistry.registerInstrumentation("com.tc.cluster.DsoCluster", new DsoClusterInjectionInstrumentation());

    // CDV-441 -- This exclude should come before any patterns that do matching that might
    // mandate more class loading (e.g. a superclass/interface match (Foo+))
    addPermanentExcludePattern("org.jboss.net.protocol..*");

    // TODO:: come back and add all possible non-portable/non-adaptable classes here. This is by no means exhaustive !

    // XXX:: There is a bug in aspectwerkz com.tc..* matches both com.tc and com.tctest classes. As a work around
    // this is commented and isTCPatternMatchingHack() method is added instead. When that bug is fixed, uncomment
    // this and remove isTCPatternMatchingHack();
    // addPermanentExcludePattern("com.tc..*");
    // addPermanentExcludePattern("com.terracottatech..*");

    nonportablesMatcher = new CompoundExpressionMatcher();

    DSOApplicationConfig appConfig = configSetupManager
        .dsoApplicationConfigFor(ConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);

    supportSharingThroughReflection = appConfig.supportSharingThroughReflection();
    try {
      doPreInstrumentedAutoconfig();
      doAutoconfig();
      doLegacyDefaultModuleConfig();
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    ConfigLoader loader = new ConfigLoader(this, logger);
    loader.loadDsoConfig((DsoApplication) appConfig.getBean());

    logger.debug("web-applications: " + this.webApplications);
    logger.debug("roots: " + this.roots);
    logger.debug("locks: " + this.locks);
    logger.debug("distributed-methods: " + this.distributedMethods);

    rewriteHashtableAutoLockSpecIfNecessary();
  }

  private void doLegacyDefaultModuleConfig() {
    new ExcludesConfiguration(this).apply();
    new GUIModelsConfiguration(this).apply();
    new Jdk15PreInstrumentedConfiguration(this).apply();
    new StandardConfiguration(this).apply();
  }

  public String rawConfigText() {
    return configSetupManager.rawConfigText();
  }

  public boolean reflectionEnabled() {
    return this.supportSharingThroughReflection;
  }

  public Portability getPortability() {
    return this.portability;
  }

  public void addAutoLockExcludePattern(final String expression) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    autoLockExcludes.add(visitor);
  }

  public void addPermanentExcludePattern(final String pattern) {
    permanentExcludesMatcher.add(new ClassExpressionMatcherImpl(expressionHelper, pattern));
  }

  public LockDefinition createLockDefinition(final String name, final ConfigLockLevel level) {
    return new LockDefinitionImpl(name, level);
  }

  public void addNonportablePattern(final String pattern) {
    nonportablesMatcher.add(new ClassExpressionMatcherImpl(expressionHelper, pattern));
  }

  private InstrumentationDescriptor newInstrumentationDescriptor(final InstrumentedClass classDesc) {
    return new InstrumentationDescriptorImpl(classDesc, //
                                             new ClassExpressionMatcherImpl(expressionHelper, //
                                                                            classDesc.classExpression()));
  }

  // This is used only for tests right now
  public void addIncludePattern(final String expression) {
    addIncludePattern(expression, false, false, false);
  }

  public CommonL1Config getNewCommonL1Config() {
    return configSetupManager.commonL1Config();
  }

  // This is used only for tests right now
  public void addIncludePattern(final String expression, final boolean honorTransient) {
    addIncludePattern(expression, honorTransient, false, false);
  }

  public void addIncludePattern(final String expression, final boolean honorTransient,
                                final boolean oldStyleCallConstructorOnLoad, final boolean honorVolatile) {
    IncludeOnLoad onLoad = new IncludeOnLoad();
    if (oldStyleCallConstructorOnLoad) {
      onLoad.setToCallConstructorOnLoad(true);
    }
    addInstrumentationDescriptor(new IncludedInstrumentedClass(expression, honorTransient, honorVolatile, onLoad));

    clearAdaptableCache();
  }

  public void addIncludePattern(String expression, boolean honorTransient, String methodToCallOnLoad,
                                boolean honorVolatile) {
    IncludeOnLoad onLoad = new IncludeOnLoad(IncludeOnLoad.METHOD, methodToCallOnLoad);
    addInstrumentationDescriptor(new IncludedInstrumentedClass(expression, honorTransient, honorVolatile, onLoad));

    clearAdaptableCache();
  }

  public void addIncludeAndLockIfRequired(final String expression, final boolean honorTransient,
                                          final boolean oldStyleCallConstructorOnLoad, final boolean honorVolatile,
                                          final String lockExpression, final ClassInfo classInfo) {
    if (hasSpec(classInfo)) { return; }

    // The addition of the lock expression and the include need to be atomic -- see LKC-2616
    // TODO see LKC-1893. Need to check for primitive types, logically managed classes, etc.
    if (!hasIncludeExcludePattern(classInfo)) {
      // only add include if not specified in tc-config
      addIncludePattern(expression, honorTransient, oldStyleCallConstructorOnLoad, honorVolatile);
      addWriteAutolock(lockExpression);
    }
  }

  // This is used only for tests right now
  public void addExcludePattern(final String expression) {
    addInstrumentationDescriptor(new ExcludedInstrumentedClass(expression));
  }

  public void addInstrumentationDescriptor(final InstrumentedClass classDesc) {
    this.instrumentationDescriptors.add(0, newInstrumentationDescriptor(classDesc));
  }

  public boolean hasIncludeExcludePatterns() {
    return !this.instrumentationDescriptors.isEmpty();
  }

  public boolean hasIncludeExcludePattern(final ClassInfo classInfo) {
    return getInstrumentationDescriptorFor(classInfo) != DEFAULT_INSTRUMENTATION_DESCRIPTOR;
  }

  public DSORuntimeLoggingOptions runtimeLoggingOptions() {
    return this.configSetupManager.dsoL1Config().runtimeLoggingOptions();
  }

  public DSORuntimeOutputOptions runtimeOutputOptions() {
    return this.configSetupManager.dsoL1Config().runtimeOutputOptions();
  }

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
    return this.configSetupManager.dsoL1Config().instrumentationLoggingOptions();
  }

  private void doPreInstrumentedAutoconfig() {
    TransparencyClassSpec spec = null;

    spec = getOrCreateSpec("java.util.TreeMap", "com.tc.object.applicator.TreeMapApplicator");
    spec.setUseNonDefaultConstructor(true);
    spec.addMethodAdapter(SerializationUtil.PUT_SIGNATURE, new TreeMapAdapter.PutAdapter());
    spec.addMethodAdapter("deleteEntry(Ljava/util/TreeMap$Entry;)V", new TreeMapAdapter.DeleteEntryAdapter());
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addEntrySetWrapperSpec(SerializationUtil.ENTRY_SET_SIGNATURE);

    spec = getOrCreateSpec("java.util.HashMap", "com.tc.object.applicator.PartialHashMapApplicator");

    spec = getOrCreateSpec("java.util.LinkedHashMap", "com.tc.object.applicator.LinkedHashMapApplicator");
    spec.setUseNonDefaultConstructor(true);

    spec = getOrCreateSpec("java.util.Hashtable", "com.tc.object.applicator.PartialHashMapApplicator");

    /**
     * spec.addSupportMethodCreator(new HashtableMethodCreator());
     * spec.addHashtablePutLogSpec(SerializationUtil.PUT_SIGNATURE);
     * spec.addHashtableRemoveLogSpec(SerializationUtil.REMOVE_KEY_SIGNATURE);
     * spec.addHashtableClearLogSpec(SerializationUtil.CLEAR_SIGNATURE);
     * spec.addMethodAdapter("entrySet()Ljava/util/Set;", new HashtableAdapter.EntrySetAdapter());
     * spec.addMethodAdapter("keySet()Ljava/util/Set;", new HashtableAdapter.KeySetAdapter());
     * spec.addMethodAdapter("values()Ljava/util/Collection;", new HashtableAdapter.ValuesAdapter());
     */

    /**
     * addWriteAutolock("synchronized * java.util.Hashtable.*(..)"); addReadAutolock(new String[] { "synchronized *
     * java.util.Hashtable.get(..)", "synchronized * java.util.Hashtable.hashCode(..)", "synchronized *
     * java.util.Hashtable.contains*(..)", "synchronized * java.util.Hashtable.elements(..)", "synchronized *
     * java.util.Hashtable.equals(..)", "synchronized * java.util.Hashtable.isEmpty(..)", "synchronized *
     * java.util.Hashtable.keys(..)", "synchronized * java.util.Hashtable.size(..)", "synchronized *
     * java.util.Hashtable.toString(..)" });
     */
    spec = getOrCreateSpec("java.util.Properties", "com.tc.object.applicator.PartialHashMapApplicator");
    addWriteAutolock("synchronized * java.util.Properties.*(..)");

    spec = getOrCreateSpec("com.tcclient.util.MapEntrySetWrapper$EntryWrapper");

    spec = getOrCreateSpec("java.util.IdentityHashMap", "com.tc.object.applicator.HashMapApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.PUT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_KEY_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);

    spec = getOrCreateSpec("java.util.BitSet");
    spec.setHonorTransient(false);

    if (Vm.isJDK15Compliant()) {
      spec = getOrCreateSpec("java.util.EnumMap");
      spec.setHonorTransient(false);
      spec = getOrCreateSpec("java.util.EnumSet");
      if (!Vm.isIBM() || !Vm.isJDK16Compliant()) {
        spec = getOrCreateSpec("java.util.RegularEnumSet");
        spec = getOrCreateSpec("java.util.RegularEnumSet$EnumSetIterator");
      }
    }

    spec = getOrCreateSpec("java.util.Collections");
    spec = getOrCreateSpec("java.util.Collections$EmptyList");
    spec = getOrCreateSpec("java.util.Collections$EmptyMap");
    spec = getOrCreateSpec("java.util.Collections$EmptySet");

    spec = getOrCreateSpec("java.util.Collections$UnmodifiableCollection");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableCollection$1");
    spec = getOrCreateSpec("java.util.Collections$1");
    spec.setHonorJDKSubVersionSpecific(true);
    spec = getOrCreateSpec("java.util.Collections$2");
    spec.setHonorJDKSubVersionSpecific(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableList$1");
    spec.setHonorJDKSubVersionSpecific(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableList");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableMap");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet");
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableMap$UnmodifiableEntrySet$1");
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableRandomAccessList");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableSet");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableSortedMap");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$UnmodifiableSortedSet");
    spec.setHonorTransient(true);

    spec = getOrCreateSpec("java.util.Collections$SingletonSet");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SingletonList");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SingletonMap");
    spec.setHonorTransient(true);

    spec = getOrCreateSpec("java.util.Collections$SynchronizedSet");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedCollection");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedList");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedSortedMap");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedSortedSet");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedMap");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("java.util.Collections$SynchronizedRandomAccessList");
    // autoLockAllMethods(spec, ConfigLockLevel.WRITE);
    spec.setHonorTransient(true);

    addJavaUtilCollectionPreInstrumentedSpec();

    spec = getOrCreateSpec("com.tcclient.util.SortedViewSetWrapper");
    spec.setHonorTransient(true);

    // These classes are not PORTABLE by themselves, but logical classes subclasses them.
    // We dont want them to get tc fields, TransparentAccess interfaces etc. but we do want them
    // to be instrumented for Array manipulations, clone(), wait(), notify() calls etc.
    spec = getOrCreateSpec("java.util.AbstractCollection");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec = getOrCreateSpec("java.util.AbstractList");
    spec.setHonorTransient(true);
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec.addSupportMethodCreator(new AbstractListMethodCreator());
    spec = getOrCreateSpec("java.util.AbstractSet");
    spec = getOrCreateSpec("java.util.AbstractSequentialList");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec = getOrCreateSpec("java.util.Dictionary");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    // AbstractMap is special because it actually has some fields so it needs to be instrumented and not just ADAPTABLE
    spec = getOrCreateSpec("java.util.AbstractMap");
    spec.setHonorTransient(true);

    // spec = getOrCreateSpec("java.lang.Number");
    // This hack is needed to make Number work in all platforms. Without this hack, if you add Number in bootjar, the
    // JVM crashes.
    // spec.generateNonStaticTCFields(false);

    // =================================================================

    spec = getOrCreateSpec("com.tcclient.object.DistributedMethodCall");

    spec = getOrCreateSpec("java.util.Date", "com.tc.object.applicator.DateApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.SET_TIME_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_YEAR_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_MONTH_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_DATE_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_HOURS_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_MINUTES_SIGNATURE);
    spec.addDateMethodLogSpec(SerializationUtil.SET_SECONDS_SIGNATURE);

    spec = getOrCreateSpec("java.sql.Date", "com.tc.object.applicator.DateApplicator");
    spec = getOrCreateSpec("java.sql.Time", "com.tc.object.applicator.DateApplicator");
    spec = getOrCreateSpec("java.sql.Timestamp", "com.tc.object.applicator.DateApplicator");
    spec.addDateMethodLogSpec(SerializationUtil.SET_TIME_SIGNATURE, MethodSpec.TIMESTAMP_SET_TIME_METHOD_WRAPPER_LOG);
    spec.addAlwaysLogSpec(SerializationUtil.SET_NANOS_SIGNATURE);

    spec = getOrCreateSpec("java.net.URL", "com.tc.object.applicator.URLApplicator");
    spec.setHonorTransient(true);
    spec.addAlwaysLogSpec(SerializationUtil.URL_SET_SIGNATURE);

    spec = getOrCreateSpec("java.util.Calendar");
    spec = getOrCreateSpec("java.util.GregorianCalendar");
    spec.setHonorTransient(true);

    // addJDK15PreInstrumentedSpec();
    // This section of spec are specified in the BootJarTool also
    // They are placed again so that the honorTransient
    // flag will be honored during runtime.
    // SECTION BEGINS
    if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
      addJavaUtilConcurrentHashMapSpec(); // should be in jdk15-preinst-config bundle
      addLogicalAdaptedLinkedBlockingQueueSpec(); // should be in jdk15-preinst-config bundle
    }
    // SECTION ENDS

    markAllSpecsPreInstrumented();
  }

  private void doAutoconfig() throws Exception {
    TransparencyClassSpec spec;

    addJDK15InstrumentedSpec();

    spec = getOrCreateSpec("java.lang.Object");
    spec.setCallConstructorOnLoad(true);

    // Autolocking FastHashMap.
    // addIncludePattern("org.apache.commons.collections.FastHashMap*", true);
    // addWriteAutolock("* org.apache.commons.collections.FastHashMap*.*(..)");
    // addReadAutolock(new String[] { "* org.apache.commons.collections.FastHashMap.clone(..)",
    // "* org.apache.commons.collections.FastHashMap*.contains*(..)",
    // "* org.apache.commons.collections.FastHashMap.equals(..)",
    // "* org.apache.commons.collections.FastHashMap.get(..)",
    // "* org.apache.commons.collections.FastHashMap*.hashCode(..)",
    // "* org.apache.commons.collections.FastHashMap*.isEmpty(..)",
    // "* org.apache.commons.collections.FastHashMap*.size(..)" });

    spec = getOrCreateSpec("gnu.trove.TObjectHash");
    spec.addTObjectHashRemoveAtLogSpec(SerializationUtil.TROVE_REMOVE_AT_SIGNATURE);

    spec = getOrCreateSpec("gnu.trove.THashMap", "com.tc.object.applicator.HashMapApplicator");
    spec.addTHashMapPutLogSpec(SerializationUtil.PUT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addEntrySetWrapperSpec(SerializationUtil.ENTRY_SET_SIGNATURE);
    spec.addKeySetWrapperSpec(SerializationUtil.KEY_SET_SIGNATURE);
    spec.addValuesWrapperSpec(SerializationUtil.VALUES_SIGNATURE);
    spec.addMethodAdapter(SerializationUtil.TRANSFORM_VALUES_SIGNATURE, new THashMapAdapter.TransformValuesAdapter());

    spec = getOrCreateSpec("gnu.trove.THashSet", "com.tc.object.applicator.HashSetApplicator");
    spec.addIfTrueLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);

    spec = getOrCreateSpec("gnu.trove.ToObjectArrayProcedure");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);

    spec = getOrCreateSpec("javax.servlet.GenericServlet");
    spec.setHonorTransient(true);
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    // TODO for the Event Swing sample only
    LockDefinition ld = new LockDefinitionImpl("setTextArea", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* test.event.*.setTextArea(..)", ld);

    // hard code junk for Axis2 problem (CDV-525)
    addCustomAdapter("org.codehaus.jam.internal.reflect.ReflectClassBuilder", new ReflectClassBuilderAdapter());

    if (hasBootJar) {
      // pre-load specs from boot jar
      BootJar bootJar = null;
      try {
        bootJar = BootJar.getDefaultBootJarForReading();
        Set allPreInstrumentedClasses = bootJar.getAllPreInstrumentedClasses();
        // Create specs for any instrumented classes in the boot jar (such thay they can be shared)
        for (Iterator i = allPreInstrumentedClasses.iterator(); i.hasNext();) {
          getOrCreateSpec((String) i.next());
        }
      } finally {
        BootJar.closeQuietly(bootJar);
      }
    }
  }

  public boolean addClassConfigBasedAdapters(final ClassInfo classInfo) {
    boolean addedAdapters = false;

    fields: for (FieldInfo fi : classInfo.getFields()) {

      if (Vm.isJDK15Compliant()) {
        Annotation[] annotations;
        try {
          annotations = fi.getAnnotations();
        } catch (Exception e) {
          logger.warn("Exception reading field annotations on " + classInfo.getName()
                      + " (possibly due to a badly behaved ClassLoader)");
          return false;
        }

        for (Annotation ann : annotations) {
          if ("com.tc.injection.annotations.InjectedDsoInstance".equals(ann.getInterfaceName())) {
            addInjectedField(classInfo.getName(), fi.getName(), "");
            addFieldInjectionAdapter(classInfo, fi, "");
            addedAdapters = true;
            continue fields;
          }
        }
      }

      final String type = getInjectedFieldType(classInfo, fi.getName());
      if (type != null) {
        addFieldInjectionAdapter(classInfo, fi, type);
        addedAdapters = true;
      }
    }

    return addedAdapters;
  }

  private void addFieldInjectionAdapter(final ClassInfo classInfo, final FieldInfo fi, String type) {

    if (null == type || 0 == type.length()) {
      type = fi.getType().getName();
    }
    InjectionInstrumentation instrumentation = injectionRegistry.lookupInstrumentation(type);
    if (null == instrumentation) { throw new UnsupportedInjectedDsoInstanceTypeException(classInfo.getName(), fi
        .getName(), fi.getType().getName()); }

    TransparencyClassSpec spec = getOrCreateSpec(classInfo.getName());
    spec.setHasOnLoadInjection(true);
    addCustomAdapter(classInfo.getName(), instrumentation.getClassAdapterFactoryForFieldInjection(fi));
  }

  private void addJDK15InstrumentedSpec() {
    if (Vm.isJDK15Compliant()) {

      TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock");
      spec.markPreInstrumented();
      spec.setPreCreateMethod("validateInUnLockState");
      spec.setCallConstructorOnLoad(true);
      spec.setHonorTransient(true);

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock$DsoLock");
      spec.setHonorTransient(true);

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock$ReadLock");
      spec.markPreInstrumented();
      spec.setPreCreateMethod("validateInUnLockState");
      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock$WriteLock");
      spec.setPreCreateMethod("validateInUnLockState");
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock$Sync");
      spec.setHonorTransient(true);
      spec.setCustomClassAdapter(new AQSSubclassStrongReferenceAdapter());
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantReadWriteLock$FairSync");
      spec.setCallConstructorOnLoad(true);
      spec.markPreInstrumented();

      spec = getOrCreateSpec("com.tcclient.util.concurrent.locks.ConditionObject");
      spec.disableWaitNotifyCodeSpec("signal()V");
      spec.disableWaitNotifyCodeSpec("signalAll()V");
      spec.setHonorTransient(true);
      spec.setCallConstructorOnLoad(true);

      spec = getOrCreateSpec("com.tcclient.util.concurrent.locks.ConditionObject$SyncCondition");
      spec.setCallConstructorOnLoad(true);

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock$Sync");
      spec.setHonorTransient(true);
      spec.setCustomClassAdapter(new AQSSubclassStrongReferenceAdapter());
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock$FairSync");
      spec.setCallConstructorOnLoad(true);
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock");
      spec.setPreCreateMethod("validateInUnLockState");
      spec.setCallConstructorOnLoad(true);
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.CopyOnWriteArrayList", "com.tc.object.applicator.ListApplicator");
      spec.setCallConstructorOnLoad(true);
      spec.markPreInstrumented();

      spec = getOrCreateSpec("java.util.concurrent.CopyOnWriteArraySet");
      spec.setCallConstructorOnLoad(true);
      spec.markPreInstrumented();

      addAbstractSynchronizerSpec();
    }
  }

  private void addAbstractSynchronizerSpec() {
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.locks.AbstractQueuedSynchronizer");
    spec.setHonorTransient(true);
    spec.addTransient("state");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec.setCustomClassAdapter(new JavaUtilConcurrentLocksAQSAdapter());
    spec.markPreInstrumented();

    if (Vm.isJDK16Compliant()) {
      spec = getOrCreateSpec("java.util.concurrent.locks.AbstractOwnableSynchronizer");
      spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
      spec.markPreInstrumented();
    }
  }

  private void addJavaUtilCollectionPreInstrumentedSpec() {
    // The details of the instrumentation spec is specified in BootJarTool.
    getOrCreateSpec("java.util.HashSet", "com.tc.object.applicator.HashSetApplicator");

    getOrCreateSpec("java.util.LinkedHashSet", "com.tc.object.applicator.HashSetApplicator");

    getOrCreateSpec("java.util.TreeSet", "com.tc.object.applicator.TreeSetApplicator");

    getOrCreateSpec("java.util.LinkedList", "com.tc.object.applicator.ListApplicator");

    getOrCreateSpec("java.util.Stack", "com.tc.object.applicator.ListApplicator");

    getOrCreateSpec("java.util.Vector", "com.tc.object.applicator.ListApplicator");
    // addWriteAutolock("synchronized * java.util.Vector.*(..)");
    // addReadAutolock(new String[] { "synchronized * java.util.Vector.capacity(..)",
    // "synchronized * java.util.Vector.clone(..)", "synchronized * java.util.Vector.containsAll(..)",
    // "synchronized * java.util.Vector.elementAt(..)", "synchronized * java.util.Vector.equals(..)",
    // "synchronized * java.util.Vector.firstElement(..)", "synchronized * java.util.Vector.get(..)",
    // "synchronized * java.util.Vector.hashCode(..)", "synchronized * java.util.Vector.indexOf(..)",
    // "synchronized * java.util.Vector.isEmpty(..)", "synchronized * java.util.Vector.lastElement(..)",
    // "synchronized * java.util.Vector.lastIndexOf(..)", "synchronized * java.util.Vector.size(..)",
    // "synchronized * java.util.Vector.subList(..)", "synchronized * java.util.Vector.toString(..)", });

    getOrCreateSpec("java.util.ArrayList", "com.tc.object.applicator.ListApplicator");
  }

  private void addJavaUtilConcurrentHashMapSpec() {
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.ConcurrentHashMap",
                                                 "com.tc.object.applicator.ConcurrentHashMapApplicator");
    spec.setHonorTransient(true);
    spec.setPostCreateMethod("__tc_rehash");
    // The "segments" array is itself not a shared object and doesn't need array instrumentation
    TransparencyCodeSpec defaultCodeSpec = TransparencyCodeSpecImpl.getDefaultLogicalCodeSpec();
    defaultCodeSpec.setArrayOperatorInstrumentationReq(false);
    spec.setDefaultCodeSpec(defaultCodeSpec);

    spec = getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$Segment");
    // The "table" array is itself not a shared object and doesn't need array instrumentation
    defaultCodeSpec = TransparencyCodeSpecImpl.getDefaultPhysicalCodeSpec();
    defaultCodeSpec.setArrayOperatorInstrumentationReq(false);
    defaultCodeSpec.setForceRawFieldAccess(); // field reads of HashEntry instances do not need to be instrumented
    spec.setDefaultCodeSpec(defaultCodeSpec);
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);

    if (Vm.isJDK16Compliant()) {
      spec = getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$WriteThroughEntry");
      spec = getOrCreateSpec("java.util.AbstractMap$SimpleEntry");
    }
  }

  private void addLogicalAdaptedLinkedBlockingQueueSpec() {
    getOrCreateSpec("java.util.AbstractQueue").setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    getOrCreateSpec("java.util.concurrent.LinkedBlockingQueue",
                    "com.tc.object.applicator.LinkedBlockingQueueApplicator");
  }

  public void addCustomAdapter(final String name, final ClassAdapterFactory factory) {
    synchronized (customAdapters) {
      Collection<ClassAdapterFactory> adapters = customAdapters.get(name);
      if (null == adapters) {
        adapters = new ArrayList<ClassAdapterFactory>();
        customAdapters.put(name, adapters);
      }
      adapters.add(factory);
    }
  }

  public boolean hasCustomAdapters(final ClassInfo classInfo) {
    synchronized (customAdapters) {
      return customAdapters.containsKey(classInfo.getName());
    }
  }

  public Collection<ClassAdapterFactory> getCustomAdapters(final ClassInfo classInfo) {
    synchronized (customAdapters) {
      return customAdapters.get(classInfo.getName());
    }
  }

  public void addClassReplacement(final String originalClassName, final String replacementClassName,
                                  final URL replacementResource, final ClassReplacementTest test) {
    this.classReplacements.addMapping(originalClassName, replacementClassName, replacementResource, test);
  }

  public void addClassReplacement(final String originalClassName, final String replacementClassName,
                                  final URL replacementResource) {
    addClassReplacement(originalClassName, replacementClassName, replacementResource, null);
  }

  public ClassReplacementMapping getClassReplacementMapping() {
    return classReplacements;
  }

  public void addClassResource(final String className, final URL resource, final boolean targetSystemLoaderOnly) {
    Resource prev = this.classResources.put(className, new Resource(resource, targetSystemLoaderOnly));
    // CDV-1053: don't call URL.equals() which can block
    if ((prev != null) && (!prev.getResource().toString().equals(resource.toString()))) {
      // we want to know if modules more than one module is trying to export the same class
      throw new AssertionError("Attempting to replace mapping for " + className + ", from " + prev + " to " + resource);
    }
  }

  public URL getClassResource(final String className, final ClassLoader loader,
                              final boolean hideSystemLoaderOnlyResources) {
    // don't allow export to a TIM loader. Use Import-Package instead
    if (loader instanceof BundleClassLoader) return null;

    Resource res = this.classResources.get(className);
    if (res == null) return null;

    if (!res.isTargetSystemLoaderOnly()) {
      return res.getResource();
    } else {
      if (!hideSystemLoaderOnlyResources) {
        if (ClassLoader.getSystemClassLoader() == loader) { return res.getResource(); }
        if (System.getProperty("java.system.class.loader") != null
            && ClassLoader.getSystemClassLoader().getParent() == loader) { return res.getResource(); }
      }
    }

    return null;
  }

  private void markAllSpecsPreInstrumented() {
    // Technically, synchronization isn't needed here if this method is only called
    // during construction, in a 1.5 JVM, and if specLock is final, because the JMM guarantees
    // initialization safety w/o synchronization under those conditions
    synchronized (specLock) {
      for (Iterator i = classSpecs.values().iterator(); i.hasNext();) {
        TransparencyClassSpec s = (TransparencyClassSpec) i.next();
        s.markPreInstrumented();
      }
    }
  }

  public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions() {
    return this.configSetupManager.dsoL1Config().instrumentationLoggingOptions();
  }

  public Iterator getAllUserDefinedBootSpecs() {
    Collection values = null;
    synchronized (specLock) {
      values = new HashSet(userDefinedBootSpecs.values());
    }
    return values.iterator();
  }

  public void setFaultCount(final int count) {
    this.faultCount = count;
  }

  public boolean isLockMethod(final MemberInfo memberInfo) {
    helperLogger.logIsLockMethodBegin(memberInfo.getModifiers(), memberInfo.getDeclaringType().getName(), //
                                      memberInfo.getName(), memberInfo.getSignature());

    LockDefinition lockDefinitions[] = lockDefinitionsFor(memberInfo);

    for (LockDefinition lockDefinition : lockDefinitions) {
      if (lockDefinition.isAutolock()) {
        if (isNotStaticAndIsSynchronized(memberInfo.getModifiers())) {
          helperLogger.logIsLockMethodAutolock();
          return true;
        }
      } else {
        return true;
      }
    }

    helperLogger.logIsLockMethodNoMatch(memberInfo.getDeclaringType().getName(), memberInfo.getName());
    return false;
  }

  public boolean matches(final Lock lock, final MemberInfo methodInfo) {
    return matches(lock.getMethodJoinPointExpression(), methodInfo);
  }

  public boolean matches(final String expression, final MemberInfo methodInfo) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    if (logger.isDebugEnabled()) {
      logger.debug("==>Testing for match: " + executionExpression + " against " + methodInfo);
    }
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    return visitor.match(expressionHelper.createExecutionExpressionContext(methodInfo));
  }

  // private MethodInfo getMethodInfo(int modifiers, String className, String methodName, String description,
  // String[] exceptions) {
  // // TODO: This probably needs caching.
  // return new AsmMethodInfo(classInfoFactory, modifiers, className, methodName, description, exceptions);
  // }

  // private ConstructorInfo getConstructorInfo(int modifiers, String className, String methodName, String description,
  // String[] exceptions) {
  // return new AsmConstructorInfo(classInfoFactory, modifiers, className, methodName, description, exceptions);
  // }

  // private MemberInfo getMemberInfo(int modifiers, String className, String methodName, String description,
  // String[] exceptions) {
  // if (false && "<init>".equals(methodName)) {
  // // XXX: ConstructorInfo seems to really break things. Plus, locks in
  // // constructors don't work yet.
  // // When locks in constructors work, we'll have to sort this problem out.
  // return getConstructorInfo(modifiers, className, methodName, description, exceptions);
  // } else {
  // return getMethodInfo(modifiers, className, methodName, description, exceptions);
  // }
  // }

  private static boolean isNotStaticAndIsSynchronized(final int modifiers) {
    return !Modifier.isStatic(modifiers) && Modifier.isSynchronized(modifiers);
  }

  /**
   * This is a simplified interface from DSOApplicationConfig. This is used for programmatically generating config.
   */
  public void addRoot(final String rootName, final String rootFieldName) {
    ClassSpec classSpec;
    try {
      classSpec = ClassUtils.parseFullyQualifiedFieldName(rootFieldName);
    } catch (ParseException e) {
      throw Assert.failure("Unable to parse root fieldName " + rootFieldName);
    }
    addRoot(new Root(classSpec.getFullyQualifiedClassName(), classSpec.getShortFieldName(), rootName), false);
  }

  public void addRoot(final Root root, final boolean addSpecForClass) {
    if (addSpecForClass) {
      this.getOrCreateSpec(root.getClassName());
    }

    roots.add(root);
  }

  public String rootNameFor(final FieldInfo fi) {
    Root r = findMatchingRootDefinition(fi);
    if (r != null) { return r.getRootName(fi); }
    throw Assert.failure("No such root for fieldName " + fi.getName() + " in class " + fi.getDeclaringType().getName());
  }

  public boolean isRoot(final FieldInfo fi) {
    return findMatchingRootDefinition(fi) != null;
  }

  public boolean isRootDSOFinal(final FieldInfo fi) {
    Root r = findMatchingRootDefinition(fi);
    if (r != null) { return r.isDsoFinal(fi.getType().isPrimitive()); }
    throw Assert.failure("No such root for fieldName " + fi.getName() + " in class " + fi.getDeclaringType().getName());
  }

  private Root findMatchingRootDefinition(final FieldInfo fi) {
    for (Iterator i = roots.iterator(); i.hasNext();) {
      Root r = (Root) i.next();
      if (r.matches(fi, expressionHelper)) { return r; }
    }
    return null;
  }

  private boolean classContainsAnyRoots(final ClassInfo classInfo) {
    FieldInfo[] fields = classInfo.getFields();
    for (FieldInfo fieldInfo : fields) {
      if (findMatchingRootDefinition(fieldInfo) != null) { return true; }
    }

    return false;
  }

  public String[] getMissingRootDeclarations(final ClassInfo classInfo) {
    final List missingRoots = new ArrayList();
    for (final Iterator i = roots.iterator(); i.hasNext();) {
      final Root root = (Root) i.next();

      // TODO: do we need to support checking for roots defined using expressions?
      if (root.isExpression()) continue;
      if (!root.matches(classInfo, expressionHelper)) continue;

      final String fieldName = root.getFieldName();
      final FieldInfo[] fields = classInfo.getFields();
      boolean found = false;
      for (int n = 0; (n < fields.length) && !found; n++) {
        FieldInfo fieldInfo = fields[n];
        found = fieldInfo.getName().equals(fieldName);
      }

      if (!found) {
        final String declaration = root.getClassName() + "." + root.getFieldName();
        missingRoots.add(declaration);

      }
    }
    return (String[]) missingRoots.toArray(new String[0]);
  }

  private void rewriteHashtableAutoLockSpecIfNecessary() {
    // addReadAutolock(new String[] { "synchronized * java.util.Hashtable.get(..)",
    // "synchronized * java.util.Hashtable.hashCode(..)", "synchronized * java.util.Hashtable.contains*(..)",
    // "synchronized * java.util.Hashtable.elements(..)", "synchronized * java.util.Hashtable.equals(..)",
    // "synchronized * java.util.Hashtable.isEmpty(..)", "synchronized * java.util.Hashtable.keys(..)",
    // "synchronized * java.util.Hashtable.size(..)", "synchronized * java.util.Hashtable.toString(..)" });

    String className = "java.util.Hashtable";
    ClassInfo classInfo = AsmClassInfo.getClassInfo(className, getClass().getClassLoader());

    String patterns = "get(Ljava/lang/Object;)Ljava/lang/Object;|" + //
                      "hashCode()I|" + //
                      "clone()Ljava/lang/Object;|" + //
                      "contains(Ljava/lang/Object;)Z|" + //
                      "containsKey(Ljava/lang/Object;)Z|" + //
                      "elements()Ljava/util/Enumeration;|" + //
                      "equals(Ljava/lang/Object;)Z|" + //
                      "isEmpty()Z|" + //
                      "keys()Ljava/util/Enumeration;|" + //
                      "size()I|" + //
                      "toString()Ljava/lang/String;";

    rewriteHashtableAutoLockSpecIfNecessaryInternal(classInfo, className, patterns);

    className = "java.util.HashtableTC";
    String realClassName = "java.util.Hashtable";
    classInfo = AsmClassInfo.getClassInfo(className, getClass().getClassLoader());
    patterns = "lookUpAndStoreIfNecessary(Ljava/util/Map$Entry;)Ljava/lang/Object;|" + //
               "storeValueIfValid(Ljava/util/Map$Entry;Ljava/lang/Object;)V|" + //
               "getEntry(Ljava/lang/Object;)Ljava/util/Map$Entry;|";
    rewriteHashtableAutoLockSpecIfNecessaryInternal(classInfo, realClassName, patterns);

    className = "java.util.HashtableTC$EntriesIterator";
    realClassName = "java.util.Hashtable$EntriesIterator";
    classInfo = AsmClassInfo.getClassInfo(className, getClass().getClassLoader());
    patterns = "hasNext()Z|" + //
               "nextEntry()Ljava/util/Map$Entry;";
    rewriteHashtableAutoLockSpecIfNecessaryInternal(classInfo, realClassName, patterns);

    className = "java.util.HashtableTC$EntrySetWrapper";
    realClassName = "java.util.Hashtable$EntrySetWrapper";
    classInfo = AsmClassInfo.getClassInfo(className, getClass().getClassLoader());
    patterns = "contains(Ljava/lang/Object;)Z";
    rewriteHashtableAutoLockSpecIfNecessaryInternal(classInfo, realClassName, patterns);

    className = "java.util.HashtableTC$EntryWrapper";
    realClassName = "java.util.Hashtable$EntryWrapper";
    classInfo = AsmClassInfo.getClassInfo(className, getClass().getClassLoader());
    patterns = "equals(Ljava/lang/Object;)Z|" + //
               "getKey()Ljava/lang/Object;|" + //
               "getValue()Ljava/lang/Object;|" + //
               "getValueFaultBreadth()Ljava/lang/Object;|" + //
               "hashCode()I|";
    rewriteHashtableAutoLockSpecIfNecessaryInternal(classInfo, realClassName, patterns);
  }

  private void rewriteHashtableAutoLockSpecIfNecessaryInternal(final ClassInfo classInfo, final String className,
                                                               final String patterns) {
    MemberInfo[] methods = classInfo.getMethods();
    for (MemberInfo methodInfo : methods) {
      if (patterns.indexOf(methodInfo.getName() + methodInfo.getSignature()) > -1) {
        for (Iterator i = locks.iterator(); i.hasNext();) {
          Lock lock = (Lock) i.next();
          if (matches(lock, methodInfo)) {
            LockDefinition ld = lock.getLockDefinition();
            if (ld.isAutolock() && ld.getLockLevel() != ConfigLockLevel.READ) {
              addReadAutolock("* " + className + "." + methodInfo.getName() + "(..)");
            }
            break;
          }
        }
      }
    }
  }

  public LockDefinition[] lockDefinitionsFor(final MemberInfo memberInfo) {
    final boolean isAutoLocksExcluded = matchesAutoLockExcludes(memberInfo);
    boolean foundMatchingAutoLock = false;

    List lockDefs = new ArrayList();

    for (Iterator i = locks.iterator(); i.hasNext();) {
      Lock lock = (Lock) i.next();
      if (matches(lock, memberInfo)) {
        LockDefinition definition = lock.getLockDefinition();

        if (definition.isAutolock()) {
          if (!isAutoLocksExcluded && !foundMatchingAutoLock) {
            foundMatchingAutoLock = true;
            lockDefs.add(definition);
          }
        } else {
          lockDefs.add(definition);
        }
      }
    }

    LockDefinition[] rv = new LockDefinition[lockDefs.size()];
    lockDefs.toArray(rv);
    return rv;
  }

  private boolean matchesAutoLockExcludes(final MemberInfo methodInfo) {
    ExpressionContext ctxt = expressionHelper.createExecutionExpressionContext(methodInfo);
    for (Iterator i = autoLockExcludes.iterator(); i.hasNext();) {
      ExpressionVisitor visitor = (ExpressionVisitor) i.next();
      if (visitor.match(ctxt)) return true;
    }
    return false;
  }

  public int getFaultCount() {
    return faultCount < 0 ? this.configSetupManager.dsoL1Config().faultCount() : faultCount;
  }

  private Boolean readAdaptableCache(final String name) {
    return (Boolean) adaptableCache.get(name);
  }

  private boolean cacheIsAdaptable(final String name, final boolean adaptable) {
    adaptableCache.put(name, adaptable ? Boolean.TRUE : Boolean.FALSE);
    return adaptable;
  }

  private void clearAdaptableCache() {
    this.adaptableCache.clear();
  }

  public void addWriteAutolock(final String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE);
  }

  public void addWriteAutolock(final String methodPattern, final String lockContextInfo) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE, lockContextInfo);
  }

  public void addSynchronousWriteAutolock(final String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.SYNCHRONOUS_WRITE);
  }

  public void addReadAutolock(final String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.READ);
  }

  public void addAutolock(final String methodPattern, final ConfigLockLevel type) {
    LockDefinition lockDefinition = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, type);
    lockDefinition.commit();
    addLock(methodPattern, lockDefinition);
  }

  public void addAutolock(final String methodPattern, final ConfigLockLevel type, final String configurationText) {
    LockDefinition lockDefinition = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, type, configurationText);
    lockDefinition.commit();
    addLock(methodPattern, lockDefinition);
  }

  public void addReadAutoSynchronize(final String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.AUTO_SYNCHRONIZED_READ);
  }

  public void addWriteAutoSynchronize(final String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.AUTO_SYNCHRONIZED_WRITE);
  }

  public void addLock(final String methodPattern, final LockDefinition lockDefinition) {
    // keep the list in reverse order of add
    locks.add(0, new Lock(methodPattern, lockDefinition));
  }

  private static void debug(String msg) {
    Date date = new Date();
    System.out.println(":::::::: XXX " + date + " [" + date.getTime() + "] " + Thread.currentThread().getName() + ": "
                       + msg);
  }

  public boolean shouldBeAdapted(final ClassInfo classInfo) {
    // now check if class is adaptable
    String fullClassName = classInfo.getName();

    // XXX: debugging for MNK-2290
    final boolean DEBUG = fullClassName != null && fullClassName.endsWith("DMITarget");

    Boolean cache = readAdaptableCache(fullClassName);
    if (cache != null) {
      if (DEBUG) debug("cached value: " + cache);
      return cache.booleanValue();
    }

    // @see isTCPatternMatchingHack() note elsewhere
    if (isTCPatternMatchingHack(classInfo) || permanentExcludesMatcher.match(classInfo)) {
      // permanent Excludes
      if (DEBUG) debug("permanent exclude");
      return cacheIsAdaptable(fullClassName, false);
    }

    if (fullClassName.indexOf(CGLIB_PATTERN) >= 0) {
      if (!isCapabilityEnabled(TimCapability.CGLIB)) {
        logger.error("Refusing to instrument CGLIB generated proxy type " + fullClassName
                     + " (CGLIB integration module not enabled)");
        return cacheIsAdaptable(fullClassName, false);
      }
    }

    String outerClassname = outerClassnameWithoutInner(fullClassName);
    if (isLogical(outerClassname)) {
      // We make inner classes of logical classes not instrumented while logical
      // bases are instrumented...UNLESS there is a explicit spec for said inner class
      boolean adaptable = getSpec(fullClassName) != null || outerClassname.equals(fullClassName);
      if (DEBUG) debug("outer is logical, this class adaptable = " + adaptable);
      return cacheIsAdaptable(fullClassName, adaptable);
    }

    // If a root is defined then we automagically instrument
    if (classContainsAnyRoots(classInfo)) {
      if (DEBUG) debug("contains roots");
      return cacheIsAdaptable(fullClassName, true);
    }

    // existing class specs trump config
    if (hasSpec(fullClassName)) {
      if (DEBUG) debug("has spec");
      return cacheIsAdaptable(fullClassName, true);
    }

    InstrumentationDescriptor desc = getInstrumentationDescriptorFor(classInfo);
    if (DEBUG) debug("matched desc: " + desc);
    return cacheIsAdaptable(fullClassName, desc.isInclude());
  }

  public void validateSessionConfig() {
    if (this.webApplications.size() > 0 && !isCapabilityEnabled(TimCapability.SESSIONS)) {
      consoleLogger
          .warn("One or more web applications are listed in the Terracotta configuration file, but no container TIMs have been loaded.\n"
                + "See http://www.terracotta.org/tim-warning for more information. ");
    }
  }

  private boolean isCapabilityEnabled(final TimCapability cap) {
    return timCapabilities.contains(cap);
  }

  public void enableCapability(final TimCapability cap) {
    timCapabilities.add(cap);
  }

  private boolean isTCPatternMatchingHack(final ClassInfo classInfo) {
    String fullClassName = classInfo.getName();
    return fullClassName.startsWith("com.tc.") || fullClassName.startsWith("com.terracottatech.");
  }

  public boolean isNeverAdaptable(final ClassInfo classInfo) {
    return isTCPatternMatchingHack(classInfo) || permanentExcludesMatcher.match(classInfo)
           || nonportablesMatcher.match(classInfo);
  }

  private InstrumentationDescriptor getInstrumentationDescriptorFor(final ClassInfo classInfo) {
    for (Iterator i = this.instrumentationDescriptors.iterator(); i.hasNext();) {
      InstrumentationDescriptor rv = (InstrumentationDescriptor) i.next();
      if (rv.matches(classInfo)) { return rv; }
    }
    return DEFAULT_INSTRUMENTATION_DESCRIPTOR;
  }

  private String outerClassnameWithoutInner(final String fullName) {
    int indexOfInner = fullName.indexOf('$');
    return indexOfInner < 0 ? fullName : fullName.substring(0, indexOfInner);
  }

  public boolean isTransient(final int modifiers, final ClassInfo classInfo, final String field) {
    if (ByteCodeUtil.isParent(field)) return true;
    if (ClassAdapterBase.isDelegateFieldName(field)) { return false; }

    String className = classInfo.getName();
    if (Modifier.isTransient(modifiers) && isHonorJavaTransient(classInfo)) return true;

    return transients.contains(className + "." + field);
  }

  public String getInjectedFieldType(final ClassInfo classInfo, final String field) {
    if (ByteCodeUtil.isParent(field)) return null;
    if (ClassAdapterBase.isDelegateFieldName(field)) { return null; }

    final String fullyQualifiedFieldName = classInfo.getName() + "." + field;
    return injectedFields.get(fullyQualifiedFieldName);
  }

  public boolean isVolatile(final int modifiers, final ClassInfo classInfo, final String field) {
    return Modifier.isVolatile(modifiers) && isHonorJavaVolatile(classInfo);
  }

  private boolean isHonorJavaTransient(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isHonorTransientSet()) { return spec.isHonorJavaTransient(); }
    return getInstrumentationDescriptorFor(classInfo).isHonorTransient();
  }

  private boolean isHonorJavaVolatile(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isHonorVolatileSet()) { return spec.isHonorVolatile(); }
    return getInstrumentationDescriptorFor(classInfo).isHonorVolatile();
  }

  public boolean isCallConstructorOnLoad(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isCallConstructorSet()) { return spec.isCallConstructorOnLoad(); }
    return getInstrumentationDescriptorFor(classInfo).isCallConstructorOnLoad();
  }

  public String getPreCreateMethodIfDefined(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPreCreateMethod();
    } else {
      return null;
    }
  }

  public String getPostCreateMethodIfDefined(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPostCreateMethod();
    } else {
      return null;
    }
  }

  public boolean hasOnLoadInjection(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null) { return spec.hasOnLoadInjection(); }
    // we don't delegate to the instrumentation descriptor since onload injection
    // can't be specified through configuration
    return false;
  }

  public String getOnLoadScriptIfDefined(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isExecuteScriptOnLoadSet()) { return spec.getOnLoadExecuteScript(); }
    return getInstrumentationDescriptorFor(classInfo).getOnLoadScriptIfDefined();
  }

  public String getOnLoadMethodIfDefined(final ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isCallMethodOnLoadSet()) { return spec.getOnLoadMethod(); }
    return getInstrumentationDescriptorFor(classInfo).getOnLoadMethodIfDefined();
  }

  public Class getTCPeerClass(Class clazz) {
    if (moduleSpecs != null) {
      for (ModuleSpec moduleSpec : moduleSpecs) {
        Class klass = moduleSpec.getPeerClass(clazz);
        if (klass != null) { return klass; }
      }
    }
    return clazz;
  }

  public String getAppGroup(String loaderName, String appName) {
    // treat empty strings as null
    if (loaderName != null && loaderName.length() == 0) {
      loaderName = null;
    }
    if (appName != null && appName.length() == 0) {
      appName = null;
    }
    if (loaderName == null && appName == null) { return null; }
    String nclAppGroup = (loaderName == null) ? null : (String) classLoaderNameToAppGroup.get(loaderName);
    String waAppGroup = (appName == null) ? null : (String) webAppNameToAppGroup.get(appName);
    if (nclAppGroup == null) { return waAppGroup; }
    if (waAppGroup != null && !nclAppGroup.equals(waAppGroup)) {
      logger.error("App-group configuration conflict: web-application " + appName + " is declared to be in app-group "
                   + waAppGroup + " but its classloader is " + loaderName + " which is declared to be in app-group "
                   + nclAppGroup);
    }
    return nclAppGroup;
  }

  private boolean matchesWildCard(final String regex, final String input) {
    return input.matches(regex.replaceAll("\\*", "\\.\\*"));
  }

  public TransparencyClassAdapter createDsoClassAdapterFor(final ClassVisitor writer, final ClassInfo classInfo,
                                                           final InstrumentationLogger lgr, final ClassLoader caller,
                                                           final boolean forcePortable, final boolean honorTransient) {
    String className = classInfo.getName();
    TransparencyClassSpec spec = getOrCreateSpec(className);
    spec.setHonorTransient(honorTransient);

    if (forcePortable) {
      if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
        spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
      } else {
        logger.info("Not making " + className + " forcefully portable");
      }
    }

    return new TransparencyClassAdapter(classInfo, basicGetOrCreateSpec(className, null, false), writer, lgr, caller,
                                        portability);
  }

  public ClassAdapter createClassAdapterFor(final ClassWriter writer, final ClassInfo classInfo,
                                            final InstrumentationLogger lgr, final ClassLoader caller) {
    return this.createClassAdapterFor(writer, classInfo, lgr, caller, false);
  }

  public ClassAdapter createClassAdapterFor(final ClassWriter writer, final ClassInfo classInfo,
                                            final InstrumentationLogger lgr, final ClassLoader caller,
                                            final boolean forcePortable) {
    TransparencyClassSpec spec = getOrCreateSpec(classInfo.getName());

    if (forcePortable) {
      if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
        spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
      } else {
        logger.info("Not making " + classInfo.getName() + " forcefully portable");
      }
    }

    ClassAdapter dsoAdapter = new TransparencyClassAdapter(classInfo, spec, writer, lgr, caller, portability);
    List<ClassAdapterFactory> factories = spec.getCustomClassAdapters();
    ClassVisitor cv = dsoAdapter;
    if (factories != null && !factories.isEmpty()) {
      for (ClassAdapterFactory factory : factories) {
        cv = factory.create(cv, caller);
      }
    }

    return new SafeSerialVersionUIDAdder(new OverridesHashCodeAdapter(cv));
  }

  private TransparencyClassSpec basicGetOrCreateSpec(final String className, final String applicator,
                                                     final boolean rememberSpec) {
    synchronized (specLock) {
      TransparencyClassSpec spec = getSpec(className);
      if (spec == null) {
        if (applicator != null) {
          spec = new TransparencyClassSpecImpl(className, this, applicator);
        } else {
          spec = new TransparencyClassSpecImpl(className, this);
        }
        if (rememberSpec) {
          addSpec(spec);
        }
      }
      return spec;
    }
  }

  public TransparencyClassSpec getOrCreateSpec(final String className) {
    return basicGetOrCreateSpec(className, null, true);
  }

  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator) {
    if (applicator == null) throw new AssertionError();
    return basicGetOrCreateSpec(className, applicator, true);
  }

  private void addSpec(final TransparencyClassSpec spec) {
    synchronized (specLock) {
      Assert.eval(!classSpecs.containsKey(spec.getClassName()));
      Assert.assertNotNull(spec);
      classSpecs.put(spec.getClassName(), spec);
    }
  }

  public boolean isLogical(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    return spec != null && spec.isLogical();
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  public boolean isPortableModuleClass(final Class clazz) {
    if (moduleSpecs != null) {
      for (ModuleSpec moduleSpec : moduleSpecs) {
        if (moduleSpec.isPortableClass(clazz)) { return true; }
      }
    }
    return false;
  }

  public Class getChangeApplicator(final Class clazz) {
    ChangeApplicatorSpec applicatorSpec = null;
    TransparencyClassSpec spec = getSpec(clazz.getName());
    if (spec != null) {
      applicatorSpec = spec.getChangeApplicatorSpec();
    }

    if (applicatorSpec == null) {
      if (moduleSpecs != null) {
        for (ModuleSpec moduleSpec : moduleSpecs) {
          if (moduleSpec.getChangeApplicatorSpec() != null) {
            Class applicatorClass = moduleSpec.getChangeApplicatorSpec().getChangeApplicator(clazz);
            if (applicatorClass != null) { return applicatorClass; }
          }
        }
      }
      return null;
    }
    return applicatorSpec.getChangeApplicator(clazz);
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  public boolean isUseNonDefaultConstructor(final Class clazz) {
    String className = clazz.getName();
    if (LiteralValues.isLiteral(className)) { return true; }
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) { return spec.isUseNonDefaultConstructor(); }
    if (moduleSpecs != null) {
      for (ModuleSpec moduleSpec : moduleSpecs) {
        if (moduleSpec.isUseNonDefaultConstructor(clazz)) { return true; }
      }
    }
    return false;
  }

  public void addModuleSpec(final ModuleSpec moduleSpec) {
    this.moduleSpecs.add(moduleSpec);
  }

  public void setMBeanSpecs(final MBeanSpec[] mbeanSpecs) {
    this.mbeanSpecs = mbeanSpecs;
  }

  public MBeanSpec[] getMBeanSpecs() {
    return this.mbeanSpecs;
  }

  public void setSRASpecs(final SRASpec[] sraSpecs) {
    this.sraSpecs = sraSpecs;
  }

  public SRASpec[] getSRASpecs() {
    return this.sraSpecs;
  }

  public boolean addTunneledMBeanDomain(final String tunneledMBeanDomain) {
    return this.tunneledMBeanDomains.add(tunneledMBeanDomain);
  }

  /*
   * public String getChangeApplicatorClassNameFor(String className) { TransparencyClassSpec spec = getSpec(className);
   * if (spec == null) return null; return spec.getChangeApplicatorClassName(); }
   */

  private boolean hasSpec(final String className) {
    return getSpec(className) != null;
  }

  private boolean hasSpec(final ClassInfo classInfo) {
    return getSpec(classInfo.getName()) != null;
  }

  /**
   * This is used in BootJarTool. In BootJarTool, it changes the package of our implementation of ReentrantLock and
   * FutureTask to the java.util.concurrent package. In order to change the different adapter together, we need to
   * create a spec with our package and remove the spec after the instrumentation is done.
   */
  public void removeSpec(String className) {
    className = className.replace('/', '.');
    classSpecs.remove(className);
    userDefinedBootSpecs.remove(className);
  }

  public TransparencyClassSpec getSpec(String className) {
    synchronized (specLock) {
      // NOTE: This method doesn't create a spec for you. If you want that use getOrCreateSpec()
      className = className.replace('/', '.');
      TransparencyClassSpec rv = (TransparencyClassSpec) classSpecs.get(className);

      if (rv == null) {
        rv = (TransparencyClassSpec) userDefinedBootSpecs.get(className);
      } else {
        // shouldn't have a spec in both of the spec collections
        Assert.assertNull(userDefinedBootSpecs.get(className));
      }

      return rv;
    }
  }

  private void scanForMissingClassesDeclaredInConfig(final BootJar bootJar) throws BootJarException, IOException {
    int preInstrumentedCount = 0;
    Set preinstClasses = bootJar.getAllPreInstrumentedClasses();
    int bootJarPopulation = preinstClasses.size();
    List<String> missingClasses = new ArrayList<String>();

    synchronized (specLock) {
      TransparencyClassSpec[] allSpecs = getAllSpecs(true);
      for (TransparencyClassSpec classSpec : allSpecs) {
        Assert.assertNotNull(classSpec);
        String cname = classSpec.getClassName().replace('/', '.');
        if (!classSpec.isForeign() && (userDefinedBootSpecs.get(cname) != null)) continue;
        if (classSpec.isPreInstrumented()) {
          preInstrumentedCount++;
          if (!(preinstClasses.contains(classSpec.getClassName()) || classSpec.isHonorJDKSubVersionSpecific())) {
            missingClasses.add(classSpec.getClassName());
          }
        }
      }
    }

    if (missingClasses.size() > 0) {
      logger.error("Number of classes in the DSO boot jar:" + bootJarPopulation);
      logger.error("Number of classes expected to be in the DSO boot jar:" + preInstrumentedCount);
      logger.error("Missing classes: " + missingClasses);
      throw new IncompleteBootJarException("Incomplete DSO boot jar; " + missingClasses.size()
                                           + " pre-instrumented class(es) found missing");
    }
  }

  /**
   * This method will: - check the contents of the boot-jar against tc-config.xml - check that all that all the
   * necessary referenced classes are also present in the boot jar
   */
  public void verifyBootJarContents(final File bjf) throws UnverifiedBootJarException {
    logger.debug("Verifying boot jar contents...");
    try {
      BootJar bootJar = (bjf == null) ? BootJar.getDefaultBootJarForReading() : BootJar.getBootJarForReading(bjf);
      scanForMissingClassesDeclaredInConfig(bootJar);
    } catch (BootJarException bjex) {
      throw new UnverifiedBootJarException(
                                           "BootJarException occurred while attempting to verify the contents of the boot jar.",
                                           bjex);
    } catch (IOException ioex) {
      throw new UnverifiedBootJarException(
                                           "IOException occurred while attempting to verify the contents of the boot jar.",
                                           ioex);
    }
  }

  private TransparencyClassSpec[] getAllSpecs(final boolean includeBootJarSpecs) {
    List rv = null;
    synchronized (specLock) {
      rv = new ArrayList(classSpecs.values());

      if (includeBootJarSpecs) {
        for (Iterator i = getAllUserDefinedBootSpecs(); i.hasNext();) {
          rv.add(i.next());
        }
      }
    }
    return (TransparencyClassSpec[]) rv.toArray(new TransparencyClassSpec[rv.size()]);
  }

  public TransparencyClassSpec[] getAllSpecs() {
    return getAllSpecs(false);
  }

  public void addDistributedMethodCall(final DistributedMethodSpec dms) {
    this.distributedMethods.add(dms);
  }

  public DistributedMethodSpec getDmiSpec(final MemberInfo memberInfo) {
    if (Modifier.isStatic(memberInfo.getModifiers()) || "<init>".equals(memberInfo.getName())
        || "<clinit>".equals(memberInfo.getName())) { return null; }
    for (Iterator i = distributedMethods.iterator(); i.hasNext();) {
      DistributedMethodSpec dms = (DistributedMethodSpec) i.next();
      if (matches(dms.getMethodExpression(), memberInfo)) { return dms; }
    }
    return null;
  }

  public void addTransient(final String className, final String fieldName) {
    if (null == className || null == fieldName) {
      //
      throw new IllegalArgumentException("class " + className + ", field = " + fieldName);
    }
    transients.add(className + "." + fieldName);
  }

  public void addInjectedField(final String className, final String fieldName, final String instanceType) {
    if (null == className || null == fieldName) { throw new IllegalArgumentException("class " + className
                                                                                     + ", field = " + fieldName); }

    final String fullyQualifiedFieldName = className + "." + fieldName;
    if (null == instanceType) {
      injectedFields.put(fullyQualifiedFieldName, "");
    } else {
      injectedFields.put(fullyQualifiedFieldName, instanceType);
    }
  }

  public boolean isInjectedField(final String className, final String fieldName) {
    if (null == className || null == fieldName) { throw new IllegalArgumentException("class " + className
                                                                                     + ", field = " + fieldName); }

    final String fullyQualifiedFieldName = className + "." + fieldName;
    return injectedFields.containsKey(fullyQualifiedFieldName);
  }

  @Override
  public String toString() {
    return "<StandardDSOClientConfigHelperImpl: " + configSetupManager + ">";
  }

  public void writeTo(final DSOApplicationConfigBuilder appConfigBuilder) {
    throw new UnsupportedOperationException();
  }

  public void addAspectModule(final String classNamePrefix, final String moduleName) {
    synchronized (aspectModules) {
      List modules = (List) this.aspectModules.get(classNamePrefix);
      if (modules == null) {
        modules = new ArrayList();
        this.aspectModules.put(classNamePrefix, modules);
      }
      modules.add(moduleName);
    }
  }

  public Map getAspectModules() {
    return this.aspectModules;
  }

  public String getLogicalExtendingClassName(final String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec == null || !spec.isLogical()) { return null; }
    return spec.getLogicalExtendingClassName();
  }

  public void addToAppGroup(final String appGroup, final String[] namedClassloaders, final String[] webAppNames) {
    if (namedClassloaders != null) {
      for (String namedClassloader : namedClassloaders) {
        String oldGroup = (String) classLoaderNameToAppGroup.put(namedClassloader, appGroup);
        if (oldGroup != null) {
          logger
              .error("Configuration error: named-classloader \"" + namedClassloader + "\" was declared in app-group \""
                     + oldGroup + "\" and also in app-group \"" + appGroup + "\"");
        }
      }
    }
    if (webAppNames != null) {
      for (String webAppName : webAppNames) {
        String oldGroup = (String) webAppNameToAppGroup.put(webAppName, appGroup);
        if (oldGroup != null) {
          logger.error("Configuration error: web-application \"" + webAppName + "\" was declared in app-group \""
                       + oldGroup + "\" and also in app-group \"" + appGroup + "\"");
        }
      }
    }
  }

  public void addUserDefinedBootSpec(final String className, final TransparencyClassSpec spec) {
    synchronized (specLock) {
      userDefinedBootSpecs.put(className, spec);
    }
  }

  public void addRepository(final String location) {
    modulesContext.modules.addRepository(location);
  }

  public void addModule(final String artifactId, final String version) {
    Module newModule = modulesContext.modules.addNewModule();
    newModule.setName(artifactId);
    newModule.setVersion(version);
  }

  public void addModule(final String groupId, final String artifactId, final String version) {
    Module newModule = modulesContext.modules.addNewModule();
    newModule.setGroupId(groupId);
    newModule.setName(artifactId);
    newModule.setVersion(version);
  }

  public Modules getModulesForInitialization() {
    return modulesContext.getModulesForInitialization();
  }

  private static class ModulesContext {

    private boolean alwaysInitializedModules = true; // set to false only when in test
    private boolean modulesInitialized       = false; // set to true only when in test

    private Modules modules;

    // This is used only in test
    // XXX: Remove anything test related from production code
    void initializedModulesOnlyOnce() {
      this.alwaysInitializedModules = false;
    }

    void setModules(final Modules modules) {
      this.modules = modules;
    }

    Modules getModulesForInitialization() {
      if (alwaysInitializedModules) {
        return this.modules;
      } else {
        // this could happen only in test
        if (modulesInitialized) {
          return Modules.Factory.newInstance();
        } else {
          modulesInitialized = true;
          return this.modules;
        }
      }
    }
  }

  public void addWebApplication(final String pattern, final SessionConfiguration config) {
    this.webApplications.put(pattern, config);
  }

  public SessionConfiguration getSessionConfiguration(String name) {
    if (ProductInfo.getInstance().isEnterprise()) {
      try {
        LicenseManager.verifySessionCapability();
      } catch (LicenseException e) {
        logger.error(e);
        System.exit(1);
      }
    }

    name = ClassProcessorHelper.computeAppName(name);

    for (Entry<String, SessionConfiguration> entry : webApplications.entrySet()) {
      String pattern = entry.getKey();
      if (matchesWildCard(pattern, name)) {
        SessionConfiguration config = entry.getValue();
        logger.info("Clustered HTTP sessions IS enabled for [" + name + "]. matched [" + pattern + "] " + config);
        return config;
      }
    }

    // log this for custom mode only
    if (hasBootJar) {
      logger.info("Clustered HTTP sessions is NOT enabled for [" + name + "]");
    }

    return null;
  }

  public void validateGroupInfo() throws ConfigurationSetupException {
    PreparedComponentsFromL2Connection connectionComponents = new PreparedComponentsFromL2Connection(configSetupManager);
    ServerGroups serverGroupsFromL2 = new ConfigInfoFromL2Impl(configSetupManager).getServerGroupsFromL2()
        .getServerGroups();

    ConnectionInfoConfig[] connectionInfoItems = connectionComponents.createConnectionInfoConfigItemByGroup();
    HashSet<ConnectionInfo> connInfoFromL1 = new HashSet<ConnectionInfo>();
    for (int i = 0; i < connectionInfoItems.length; i++) {
      ConnectionInfo[] connectionInfo = connectionInfoItems[i].getConnectionInfos();
      for (int j = 0; j < connectionInfo.length; j++) {
        ConnectionInfo connectionIn = new ConnectionInfo(getIpAddressOfServer(connectionInfo[j].getHostname()),
                                                         connectionInfo[j].getPort(), i * j + j, connectionInfo[j]
                                                             .getGroupName());
        connInfoFromL1.add(connectionIn);
      }
    }

    HashSet<ConnectionInfo> connInfoFromL2 = new HashSet<ConnectionInfo>();
    ServerGroup[] grpArray = serverGroupsFromL2.getServerGroupArray();
    for (int i = 0; i < grpArray.length; i++) {
      String grpName = grpArray[i].getGroupName();
      ServerInfo[] serverInfos = grpArray[i].getServerInfoArray();
      for (int j = 0; j < serverInfos.length; j++) {
        ConnectionInfo connectionIn = new ConnectionInfo(getIpAddressOfServer(serverInfos[j].getName()), serverInfos[j]
            .getDsoPort().intValue(), i * j + j, grpName);
        connInfoFromL2.add(connectionIn);
      }
    }

    String errMsg = "Client and server configurations don't match.\n";
    if (connInfoFromL1.size() != connInfoFromL2.size()) {
      StringBuilder builder = new StringBuilder();
      builder.append("The number of servers specified in the client and server configs are different. ");
      // dump connInfoFromL1 and connInfoFromL2 for debugging DEV-4769
      dumpConnInfo(builder, "ConnInfo from L1", connInfoFromL1);
      dumpConnInfo(builder, "ConnInfo from L2", connInfoFromL2);
      errMsg += builder.toString();
      throw new ConfigurationSetupException(errMsg);
    }

    /**
     * This check is there because of TC_SERVER env variable
     */
    if (connInfoFromL1.size() == 1) {
      ConnectionInfo[] temp = new ConnectionInfo[1];
      connInfoFromL1.toArray(temp);
      int portFromL1 = temp[0].getPort();
      connInfoFromL2.toArray(temp);
      int portFromL2 = temp[0].getPort();
      if (portFromL1 == portFromL2) {
        return;
      } else {
        logConfigMismatchAndThrowException(connInfoFromL1, connInfoFromL2, errMsg);
      }
    }

    if (!connInfoFromL1.containsAll(connInfoFromL2)) {
      logConfigMismatchAndThrowException(connInfoFromL1, connInfoFromL2, errMsg);
    }
  }

  private void dumpConnInfo(StringBuilder builder, String mesg, HashSet<ConnectionInfo> connInfo) {
    builder.append(mesg);
    builder.append("[");
    for (ConnectionInfo ci : connInfo) {
      builder.append(ci.toString());
      builder.append(" ");
    }
    builder.append("] ");
  }

  private void logConfigMismatchAndThrowException(final HashSet<ConnectionInfo> connInfoFromL1,
                                                  final HashSet<ConnectionInfo> connInfoFromL2, String errMsg)
      throws ConfigurationSetupException {
    logger.info("L1 connection info: " + connInfoFromL1);
    logger.info("L2 connection info: " + connInfoFromL2);
    errMsg = errMsg
             + "See \"L1 connection info\" and \"L2 connection info\" in the Terracotta log files for more information.";
    throw new ConfigurationSetupException(errMsg);
  }

  private String getIpAddressOfServer(final String name) throws ConfigurationSetupException {
    InetAddress address;
    try {
      address = InetAddress.getByName(name);
      if (address.isLoopbackAddress()) {
        address = InetAddress.getLocalHost();
      }
    } catch (UnknownHostException e) {
      throw new ConfigurationSetupException(e.getMessage());
    }
    return address.getHostAddress();
  }

  private void setupL1ReconnectProperties() throws ConfigurationSetupException {

    L1ReconnectPropertiesDocument l1ReconnectPropFromL2 = new ConfigInfoFromL2Impl(configSetupManager)
        .getL1ReconnectPropertiesFromL2();

    boolean l1ReconnectEnabled = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectEnabled();
    int l1ReconnectTimeout = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectTimeout().intValue();
    int l1ReconnectSendqueuecap = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendqueuecap()
        .intValue();
    int l1ReconnectMaxdelayedacks = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectMaxDelayedAcks()
        .intValue();
    int l1ReconnectSendwindow = l1ReconnectPropFromL2.getL1ReconnectProperties().getL1ReconnectSendwindow().intValue();
    this.l1ReconnectConfig = new L1ReconnectConfigImpl(l1ReconnectEnabled, l1ReconnectTimeout, l1ReconnectSendqueuecap,
                                                       l1ReconnectMaxdelayedacks, l1ReconnectSendwindow);
  }

  public synchronized ReconnectConfig getL1ReconnectProperties() throws ConfigurationSetupException {
    if (l1ReconnectConfig == null) {
      setupL1ReconnectProperties();
    }
    return l1ReconnectConfig;
  }

  public boolean useResolveLockWhenClearing(final Class clazz) {
    // If this condition ever needs to be true for any other classes besides ConcurrentHashMap, this setting should be
    // move into the TransparencyClassSpec (as opposed to growing the list of classes here)
    return !clazz.getName().equals("java.util.concurrent.ConcurrentHashMap");
  }

  public boolean hasBootJar() {
    return this.hasBootJar;
  }

  public void recordBundleURLs(final Map<Bundle, URL> toAdd) {
    this.bundleURLs.putAll(toAdd);
  }

  public URL getBundleURL(final Bundle bundle) {
    return this.bundleURLs.get(bundle);
  }

  public UUID getUUID() {
    return id;
  }

  public String[] getTunneledDomains() {
    synchronized (tunneledMBeanDomains) {
      String[] result = new String[tunneledMBeanDomains.size()];
      tunneledMBeanDomains.toArray(result);
      return result;
    }
  }

  private static class Resource {

    private final URL     resource;
    private final boolean targetSystemLoaderOnly;

    Resource(final URL resource, final boolean targetSystemLoaderOnly) {
      this.resource = resource;
      this.targetSystemLoaderOnly = targetSystemLoaderOnly;
    }

    URL getResource() {
      return resource;
    }

    boolean isTargetSystemLoaderOnly() {
      return targetSystemLoaderOnly;
    }

    @Override
    public String toString() {
      return resource.toExternalForm();
    }
  }

  public L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException {
    configSetupManager.reloadServersConfiguration();
    return configSetupManager;
  }

  public String[] processArguments() {
    return configSetupManager.processArguments();
  }
}
