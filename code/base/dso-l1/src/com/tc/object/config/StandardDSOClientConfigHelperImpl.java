/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.geronimo.transform.HostGBeanAdapter;
import com.tc.geronimo.transform.MultiParentClassLoaderAdapter;
import com.tc.geronimo.transform.ProxyMethodInterceptorAdapter;
import com.tc.geronimo.transform.TomcatClassLoaderAdapter;
import com.tc.jam.transform.ReflectClassBuilderAdapter;
import com.tc.jboss.transform.MainAdapter;
import com.tc.jboss.transform.UCLAdapter;
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
import com.tc.object.bytecode.DelegateMethodAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLocksAQSAdapter;
import com.tc.object.bytecode.ManagerHelper;
import com.tc.object.bytecode.ManagerHelperFactory;
import com.tc.object.bytecode.SafeSerialVersionUIDAdder;
import com.tc.object.bytecode.THashMapAdapter;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TreeMapAdapter;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewSpringApplicationConfig;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.tools.BootJar;
import com.tc.object.tools.BootJarException;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.tomcat.transform.BootstrapAdapter;
import com.tc.tomcat.transform.CatalinaAdapter;
import com.tc.tomcat.transform.ContainerBaseAdapter;
import com.tc.tomcat.transform.JspWriterImplAdapter;
import com.tc.tomcat.transform.WebAppLoaderAdapter;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.runtime.Vm;
import com.tc.weblogic.WeblogicHelper;
import com.tc.weblogic.transform.EJBCodeGeneratorAdapter;
import com.tc.weblogic.transform.EventsManagerAdapter;
import com.tc.weblogic.transform.FilterManagerAdapter;
import com.tc.weblogic.transform.GenericClassLoaderAdapter;
import com.tc.weblogic.transform.ServerAdapter;
import com.tc.weblogic.transform.ServletResponseImplAdapter;
import com.tc.weblogic.transform.WebAppServletContextAdapter;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.L1ReconnectPropertiesDocument;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.SpringApplication;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandardDSOClientConfigHelperImpl implements StandardDSOClientConfigHelper, DSOClientConfigHelper {

  private static final String                    CGLIB_PATTERN                      = "$$EnhancerByCGLIB$$";

  private static final LiteralValues             literalValues                      = new LiteralValues();

  private static final TCLogger                  logger                             = CustomerLogging
                                                                                        .getDSOGenericLogger();
  private static final TCLogger                  consoleLogger                      = CustomerLogging
                                                                                        .getConsoleLogger();

  private static final InstrumentationDescriptor DEFAULT_INSTRUMENTATION_DESCRIPTOR = new NullInstrumentationDescriptor();

  private final ManagerHelperFactory             mgrHelperFactory                   = new ManagerHelperFactory();
  private final DSOClientConfigHelperLogger      helperLogger;

  private final L1TVSConfigurationSetupManager   configSetupManager;

  private final List                             locks                              = new CopyOnWriteArrayList();
  private final List                             roots                              = new CopyOnWriteArrayList();
  private final Set                              transients                         = Collections
                                                                                        .synchronizedSet(new HashSet());

  private final Set                              applicationNames                   = Collections
                                                                                        .synchronizedSet(new HashSet());
  private final List                             synchronousWriteApplications       = new ArrayList();
  private final CompoundExpressionMatcher        permanentExcludesMatcher;
  private final CompoundExpressionMatcher        nonportablesMatcher;
  private final List                             autoLockExcludes                   = new CopyOnWriteArrayList();
  private final List                             distributedMethods                 = new CopyOnWriteArrayList();

  // private final ClassInfoFactory classInfoFactory;
  private final ExpressionHelper                 expressionHelper;

  private final Map                              adaptableCache                     = Collections
                                                                                        .synchronizedMap(new HashMap());

  /**
   * A list of InstrumentationDescriptor representing include/exclude patterns
   */
  private final List                             instrumentationDescriptors         = new CopyOnWriteArrayList();

  //====================================================================================================================
  /**
   * The lock for both {@link #userDefinedBootSpecs} and {@link #classSpecs} Maps
   */
  private final Object                           specLock                           = new Object();

  /**
   * A map of class names to TransparencyClassSpec
   * 
   * @GuardedBy {@link #specLock}
   */
  private final Map                              userDefinedBootSpecs               = new HashMap();

  /**
   * A map of class names to TransparencyClassSpec for individual classes
   * 
   * @GuardedBy {@link #specLock}
   */
  private final Map                              classSpecs                         = new HashMap();
  //====================================================================================================================

  private final Map                              customAdapters                     = new ConcurrentHashMap();

  private final ClassReplacementMapping          classReplacements                  = new ClassReplacementMappingImpl();

  private final Map                              classResources                     = new ConcurrentHashMap();

  private final Map                              aspectModules                      = new ConcurrentHashMap();

  private final List                             springConfigs                      = new CopyOnWriteArrayList();

  private final boolean                          supportSharingThroughReflection;

  private final Portability                      portability;

  private int                                    faultCount                         = -1;

  private ModuleSpec[]                           moduleSpecs                        = null;

  private final ModulesContext                   modulesContext                     = new ModulesContext();

  private volatile boolean                       allowCGLIBInstrumentation          = false;

  private ReconnectConfig                        l1ReconnectConfig                  = null;

  public StandardDSOClientConfigHelperImpl(L1TVSConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
  }

  public StandardDSOClientConfigHelperImpl(boolean initializedModulesOnlyOnce,
                                           L1TVSConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
    if (initializedModulesOnlyOnce) {
      modulesContext.initializedModulesOnlyOnce();
    }
  }

  public StandardDSOClientConfigHelperImpl(L1TVSConfigurationSetupManager configSetupManager, boolean interrogateBootJar)
      throws ConfigurationSetupException {
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;
    helperLogger = new DSOClientConfigHelperLogger(logger);
    // this.classInfoFactory = new ClassInfoFactory();
    this.expressionHelper = new ExpressionHelper();
    modulesContext.setModules(configSetupManager.commonL1Config().modules() != null ? configSetupManager
        .commonL1Config().modules() : Modules.Factory.newInstance());

    permanentExcludesMatcher = new CompoundExpressionMatcher();

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

    NewDSOApplicationConfig appConfig = configSetupManager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);
    NewSpringApplicationConfig springConfig = configSetupManager
        .springApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);

    supportSharingThroughReflection = appConfig.supportSharingThroughReflection().getBoolean();
    try {
      doPreInstrumentedAutoconfig(interrogateBootJar);
      doAutoconfig(interrogateBootJar);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    ConfigLoader loader = new ConfigLoader(this, logger);
    loader.loadDsoConfig((DsoApplication) appConfig.getBean());
    loader.loadSpringConfig((SpringApplication) springConfig.getBean());

    logger.debug("web-applications: " + this.applicationNames);
    logger.debug("synchronous-write web-applications: " + this.synchronousWriteApplications);
    logger.debug("roots: " + this.roots);
    logger.debug("locks: " + this.locks);
    logger.debug("distributed-methods: " + this.distributedMethods);

    rewriteHashtableAutoLockSpecIfNecessary();
    removeTomcatAdapters();
  }

  public String rawConfigText() {
    return configSetupManager.rawConfigText();
  }

  public void allowCGLIBInstrumentation() {
    this.allowCGLIBInstrumentation = true;
  }

  public boolean reflectionEnabled() {
    return this.supportSharingThroughReflection;
  }

  public Portability getPortability() {
    return this.portability;
  }

  public void addAutoLockExcludePattern(String expression) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    autoLockExcludes.add(visitor);
  }

  public void addPermanentExcludePattern(String pattern) {
    permanentExcludesMatcher.add(new ClassExpressionMatcherImpl(expressionHelper, pattern));
  }

  public LockDefinition createLockDefinition(String name, ConfigLockLevel level) {
    return new LockDefinitionImpl(name, level);
  }

  public void addNonportablePattern(String pattern) {
    nonportablesMatcher.add(new ClassExpressionMatcherImpl(expressionHelper, pattern));
  }

  private InstrumentationDescriptor newInstrumentationDescriptor(InstrumentedClass classDesc) {
    return new InstrumentationDescriptorImpl(classDesc, //
                                             new ClassExpressionMatcherImpl(expressionHelper, //
                                                                            classDesc.classExpression()));
  }

  // This is used only for tests right now
  public void addIncludePattern(String expression) {
    addIncludePattern(expression, false, false, false);
  }

  public NewCommonL1Config getNewCommonL1Config() {
    return configSetupManager.commonL1Config();
  }

  // This is used only for tests right now
  public void addIncludePattern(String expression, boolean honorTransient) {
    addIncludePattern(expression, honorTransient, false, false);
  }

  public void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                                boolean honorVolatile) {
    IncludeOnLoad onLoad = new IncludeOnLoad();
    if (oldStyleCallConstructorOnLoad) {
      onLoad.setToCallConstructorOnLoad(true);
    }
    addInstrumentationDescriptor(new IncludedInstrumentedClass(expression, honorTransient, honorVolatile, onLoad));

    clearAdaptableCache();
  }

  public void addIncludeAndLockIfRequired(String expression, boolean honorTransient,
                                          boolean oldStyleCallConstructorOnLoad, boolean honorVolatile,
                                          String lockExpression, ClassInfo classInfo) {
    if (hasSpec(classInfo)) { return; }

    // The addition of the lock expression and the include need to be atomic -- see LKC-2616
    synchronized (this.instrumentationDescriptors) {
      // TODO see LKC-1893. Need to check for primitive types, logically managed classes, etc.
      if (!hasIncludeExcludePattern(classInfo)) {
        // only add include if not specified in tc-config
        addIncludePattern(expression, honorTransient, oldStyleCallConstructorOnLoad, honorVolatile);
        addWriteAutolock(lockExpression);
      }
    }
  }

  // This is used only for tests right now
  public void addExcludePattern(String expression) {
    addInstrumentationDescriptor(new ExcludedInstrumentedClass(expression));
  }

  public void addInstrumentationDescriptor(InstrumentedClass classDesc) {
    this.instrumentationDescriptors.add(0, newInstrumentationDescriptor(classDesc));
  }

  public boolean hasIncludeExcludePatterns() {
    return !this.instrumentationDescriptors.isEmpty();
  }

  public boolean hasIncludeExcludePattern(ClassInfo classInfo) {
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

  private void doPreInstrumentedAutoconfig(boolean interrogateBootJar) {
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
      spec = getOrCreateSpec("java.util.RegularEnumSet");
      spec = getOrCreateSpec("java.util.RegularEnumSet$EnumSetIterator");
    }

    spec = getOrCreateSpec("java.util.Collections");
    spec = getOrCreateSpec("java.util.Collections$EmptyList", "com.tc.object.applicator.ListApplicator");
    spec = getOrCreateSpec("java.util.Collections$EmptyMap", "com.tc.object.applicator.HashMapApplicator");
    spec = getOrCreateSpec("java.util.Collections$EmptySet", "com.tc.object.applicator.HashSetApplicator");

    spec = getOrCreateSpec("java.util.Collections$UnmodifiableCollection");
    spec.setHonorTransient(true);
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

  private void doAutoconfig(boolean interrogateBootJar) {
    TransparencyClassSpec spec;

    addJDK15InstrumentedSpec();

    // Generic Session classes
    spec = getOrCreateSpec("com.terracotta.session.SessionData");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("com.terracotta.session.util.Timestamp");
    spec.setHonorTransient(true);

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

    spec = getOrCreateSpec("gnu.trove.THashMap", "com.tc.object.applicator.HashMapApplicator");
    spec.addTHashMapPutLogSpec(SerializationUtil.PUT_SIGNATURE);
    spec.addTHashRemoveAtLogSpec(SerializationUtil.TROVE_REMOVE_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addEntrySetWrapperSpec(SerializationUtil.ENTRY_SET_SIGNATURE);
    spec.addKeySetWrapperSpec(SerializationUtil.KEY_SET_SIGNATURE);
    spec.addValuesWrapperSpec(SerializationUtil.VALUES_SIGNATURE);
    spec.addMethodAdapter(SerializationUtil.TRANSFORM_VALUES_SIGNATURE, new THashMapAdapter.TransformValuesAdapter());

    spec = getOrCreateSpec("gnu.trove.THashSet", "com.tc.object.applicator.HashSetApplicator");
    spec.addTHashSetAddLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addTHashSetRemoveAtLogSpec(SerializationUtil.REMOVE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);

    spec = getOrCreateSpec("gnu.trove.ToObjectArrayProcedure");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);

    spec = getOrCreateSpec("javax.servlet.GenericServlet");
    spec.setHonorTransient(true);
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    addWeblogicInstrumentation();

    // BEGIN: tomcat stuff
    // don't install tomcat-specific adaptors if this sys prop is defined
    final boolean doTomcat = System.getProperty("com.tc.tomcat.disabled") == null;
    if (doTomcat) addTomcatCustomAdapters();
    // END: tomcat stuff

    // Geronimo + WebsphereCE stuff
    addCustomAdapter("org.apache.geronimo.kernel.basic.ProxyMethodInterceptor", new ProxyMethodInterceptorAdapter());
    addCustomAdapter("org.apache.geronimo.kernel.config.MultiParentClassLoader", new MultiParentClassLoaderAdapter());
    addCustomAdapter("org.apache.geronimo.tomcat.HostGBean", new HostGBeanAdapter());
    addCustomAdapter("org.apache.geronimo.tomcat.TomcatClassLoader", new TomcatClassLoaderAdapter());

    // JBoss adapters
    addCustomAdapter("org.jboss.mx.loading.UnifiedClassLoader", new UCLAdapter());
    addCustomAdapter("org.jboss.Main", new MainAdapter());

    // TODO for the Event Swing sample only
    LockDefinition ld = new LockDefinitionImpl("setTextArea", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* test.event.*.setTextArea(..)", ld);

    // hard code junk for Axis2 problem (CDV-525)
    addCustomAdapter("org.codehaus.jam.internal.reflect.ReflectClassBuilder", new ReflectClassBuilderAdapter());

    if (interrogateBootJar) {
      // pre-load specs from boot jar
      BootJar bootJar = null;
      try {
        bootJar = BootJar.getDefaultBootJarForReading();

        Set allPreInstrumentedClasses = bootJar.getAllPreInstrumentedClasses();
        for (Iterator i = allPreInstrumentedClasses.iterator(); i.hasNext();) {
          // Create specs for any instrumented classes in the boot jar (such thay they can be shared)
          getOrCreateSpec((String) i.next());
        }
      } catch (Throwable e) {
        logger.error(e);

        // don't needlessly wrap errors and runtimes
        if (e instanceof RuntimeException) { throw (RuntimeException) e; }
        if (e instanceof Error) { throw (Error) e; }

        throw new RuntimeException(e);
      } finally {
        try {
          if (bootJar != null) {
            bootJar.close();
          }
        } catch (Exception e) {
          logger.error(e);
        }
      }
    }
  }

  private void addWeblogicInstrumentation() {
    if (WeblogicHelper.isWeblogicPresent()) {
      if (WeblogicHelper.isSupportedVersion()) {
        addAspectModule("weblogic.servlet.internal", "com.tc.weblogic.SessionAspectModule");

        addCustomAdapter("weblogic.Server", new ServerAdapter());
        addCustomAdapter("weblogic.utils.classloaders.GenericClassLoader", new GenericClassLoaderAdapter());
        addCustomAdapter("weblogic.ejb20.ejbc.EjbCodeGenerator", new EJBCodeGeneratorAdapter());
        addCustomAdapter("weblogic.ejb.container.ejbc.EjbCodeGenerator", new EJBCodeGeneratorAdapter());
        addCustomAdapter("weblogic.servlet.internal.WebAppServletContext", new WebAppServletContextAdapter());
        addCustomAdapter("weblogic.servlet.internal.EventsManager", new EventsManagerAdapter());
        addCustomAdapter("weblogic.servlet.internal.FilterManager", new FilterManagerAdapter());
        addCustomAdapter("weblogic.servlet.internal.ServletResponseImpl", new ServletResponseImplAdapter());
        addCustomAdapter("weblogic.servlet.internal.TerracottaServletResponseImpl",
                         new DelegateMethodAdapter("weblogic.servlet.internal.ServletResponseImpl", "nativeResponse"));
      } else {
        final String msg = "weblogic instrumentation NOT being added since this appears to be an unsupported version";
        logger.warn(msg);
        consoleLogger.warn(msg);
      }
    }
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
    TransparencyClassSpec spec = getOrCreateSpec("java.util.AbstractQueue");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    spec = getOrCreateSpec("java.util.concurrent.LinkedBlockingQueue",
                           "com.tc.object.applicator.LinkedBlockingQueueApplicator");
  }

  private void addTomcatCustomAdapters() {
    addCustomAdapter("org.apache.jasper.runtime.JspWriterImpl", new JspWriterImplAdapter());
    addCustomAdapter("org.apache.catalina.loader.WebappLoader", new WebAppLoaderAdapter());
    addCustomAdapter("org.apache.catalina.startup.Catalina", new CatalinaAdapter());
    addCustomAdapter("org.apache.catalina.startup.Bootstrap", new BootstrapAdapter());
    addCustomAdapter("org.apache.catalina.core.ContainerBase", new ContainerBaseAdapter());
    addCustomAdapter("org.apache.catalina.connector.SessionRequest55",
                     new DelegateMethodAdapter("org.apache.catalina.connector.Request", "valveReq"));
    addCustomAdapter("org.apache.catalina.connector.SessionResponse55",
                     new DelegateMethodAdapter("org.apache.catalina.connector.Response", "valveRes"));
  }

  private void removeTomcatAdapters() {
    // XXX: hack to avoid problems with coresident L1 (this can be removed when session support becomes a 1st class
    // module)
    if (applicationNames.isEmpty()) {
      removeCustomAdapter("org.apache.catalina.core.ContainerBase");
    }
  }

  public boolean removeCustomAdapter(String name) {
    synchronized (customAdapters) {
      Object prev = this.customAdapters.remove(name);
      return prev != null;
    }
  }

  public void addCustomAdapter(final String name, final ClassAdapterFactory factory) {
    synchronized (customAdapters) {
      if (customAdapters.containsKey(name)) { return; }
      Object prev = this.customAdapters.put(name, factory);
      Assert.assertNull(prev);
    }
  }

  public boolean hasCustomAdapter(ClassInfo classInfo) {
    synchronized (customAdapters) {
      return customAdapters.containsKey(classInfo.getName());
    }
  }

  public ClassAdapterFactory getCustomAdapter(ClassInfo classInfo) {
    synchronized (customAdapters) {
      return (ClassAdapterFactory) customAdapters.get(classInfo.getName());
    }
  }

  public void addClassReplacement(final String originalClassName, final String replacementClassName,
                                  final URL replacementResource) {
    synchronized (classReplacements) {
      String prev = this.classReplacements.addMapping(originalClassName, replacementClassName, replacementResource);
      Assert.assertNull(prev);
    }
  }

  public ClassReplacementMapping getClassReplacementMapping() {
    return classReplacements;
  }

  public void addClassResource(final String className, final URL resource) {
    URL prev = (URL) this.classResources.put(className, resource);
    if ((prev != null) && (!prev.equals(resource))) {
      // we want to know if modules more than one module is trying to export the same class
      throw new AssertionError("Attempting to replace mapping for " + className + ", from " + prev + " to " + resource);
    }
  }

  public URL getClassResource(String className) {
    return (URL) this.classResources.get(className);
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

  public void setFaultCount(int count) {
    this.faultCount = count;
  }

  public boolean isLockMethod(MemberInfo memberInfo) {
    helperLogger.logIsLockMethodBegin(memberInfo.getModifiers(), memberInfo.getDeclaringType().getName(), //
                                      memberInfo.getName(), memberInfo.getSignature());

    LockDefinition lockDefinitions[] = lockDefinitionsFor(memberInfo);

    for (int j = 0; j < lockDefinitions.length; j++) {
      if (lockDefinitions[j].isAutolock()) {
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
    if (logger.isDebugEnabled()) logger
        .debug("==>Testing for match: " + executionExpression + " against " + methodInfo);
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

  private static boolean isNotStaticAndIsSynchronized(int modifiers) {
    return !Modifier.isStatic(modifiers) && Modifier.isSynchronized(modifiers);
  }

  /**
   * This is a simplified interface from DSOApplicationConfig. This is used for programmatically generating config.
   */
  public void addRoot(String rootName, String rootFieldName) {
    ClassSpec classSpec;
    try {
      classSpec = ClassUtils.parseFullyQualifiedFieldName(rootFieldName);
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
    addRoot(new Root(classSpec.getFullyQualifiedClassName(), classSpec.getShortFieldName(), rootName), false);
  }

  public void addRoot(Root root, boolean addSpecForClass) {
    if (addSpecForClass) {
      this.getOrCreateSpec(root.getClassName());
    }

    roots.add(root);
  }

  public String rootNameFor(FieldInfo fi) {
    Root r = findMatchingRootDefinition(fi);
    if (r != null) { return r.getRootName(fi); }
    throw Assert.failure("No such root for fieldName " + fi.getName() + " in class " + fi.getDeclaringType().getName());
  }

  public boolean isRoot(FieldInfo fi) {
    return findMatchingRootDefinition(fi) != null;
  }

  public boolean isRootDSOFinal(FieldInfo fi) {
    Root r = findMatchingRootDefinition(fi);
    if (r != null) { return r.isDsoFinal(fi.getType().isPrimitive()); }
    throw Assert.failure("No such root for fieldName " + fi.getName() + " in class " + fi.getDeclaringType().getName());
  }

  private Root findMatchingRootDefinition(FieldInfo fi) {
    for (Iterator i = roots.iterator(); i.hasNext();) {
      Root r = (Root) i.next();
      if (r.matches(fi, expressionHelper)) { return r; }
    }
    return null;
  }

  private boolean classContainsAnyRoots(ClassInfo classInfo) {
    FieldInfo[] fields = classInfo.getFields();
    for (int i = 0; i < fields.length; i++) {
      FieldInfo fieldInfo = fields[i];
      if (findMatchingRootDefinition(fieldInfo) != null) { return true; }
    }

    return false;
  }

  public String[] getMissingRootDeclarations(ClassInfo classInfo) {
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

  private void rewriteHashtableAutoLockSpecIfNecessaryInternal(ClassInfo classInfo, String className, String patterns) {
    MemberInfo[] methods = classInfo.getMethods();
    for (int j = 0; j < methods.length; j++) {
      MemberInfo methodInfo = methods[j];
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

  public LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo) {
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

  private boolean matchesAutoLockExcludes(MemberInfo methodInfo) {
    ExpressionContext ctxt = expressionHelper.createExecutionExpressionContext(methodInfo);
    for (Iterator i = autoLockExcludes.iterator(); i.hasNext();) {
      ExpressionVisitor visitor = (ExpressionVisitor) i.next();
      if (visitor.match(ctxt)) return true;
    }
    return false;
  }

  public int getFaultCount() {
    return faultCount < 0 ? this.configSetupManager.dsoL1Config().faultCount().getInt() : faultCount;
  }

  private Boolean readAdaptableCache(String name) {
    return (Boolean) adaptableCache.get(name);
  }

  private boolean cacheIsAdaptable(String name, boolean adaptable) {
    adaptableCache.put(name, adaptable ? Boolean.TRUE : Boolean.FALSE);
    return adaptable;
  }

  private void clearAdaptableCache() {
    this.adaptableCache.clear();
  }

  public void addWriteAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE);
  }

  public void addWriteAutolock(String methodPattern, String lockContextInfo) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE, lockContextInfo);
  }

  public void addSynchronousWriteAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.SYNCHRONOUS_WRITE);
  }

  public void addReadAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.READ);
  }

  public void addAutolock(String methodPattern, ConfigLockLevel type) {
    LockDefinition lockDefinition = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, type);
    lockDefinition.commit();
    addLock(methodPattern, lockDefinition);
  }

  public void addAutolock(String methodPattern, ConfigLockLevel type, String configurationText) {
    LockDefinition lockDefinition = new LockDefinitionImpl(LockDefinition.TC_AUTOLOCK_NAME, type, configurationText);
    lockDefinition.commit();
    addLock(methodPattern, lockDefinition);
  }

  public void addReadAutoSynchronize(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.AUTO_SYNCHRONIZED_READ);
  }

  public void addWriteAutoSynchronize(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.AUTO_SYNCHRONIZED_WRITE);
  }

  public void addLock(String methodPattern, LockDefinition lockDefinition) {
    // keep the list in reverse order of add
    locks.add(0, new Lock(methodPattern, lockDefinition));
  }

  public boolean shouldBeAdapted(ClassInfo classInfo) {
    // now check if class is adaptable
    String fullClassName = classInfo.getName();
    Boolean cache = readAdaptableCache(fullClassName);
    if (cache != null) { return cache.booleanValue(); }

    // @see isTCPatternMatchingHack() note elsewhere
    if (isTCPatternMatchingHack(classInfo) || permanentExcludesMatcher.match(classInfo)) {
      // permanent Excludes
      return cacheIsAdaptable(fullClassName, false);
    }

    if (fullClassName.indexOf(CGLIB_PATTERN) >= 0) {
      if (!allowCGLIBInstrumentation) {
        logger.error("Refusing to instrument CGLIB generated proxy type " + fullClassName
                     + " (CGLIB terracotta plugin not installed)");
        return cacheIsAdaptable(fullClassName, false);
      }
    }

    String outerClassname = outerClassnameWithoutInner(fullClassName);
    if (isLogical(outerClassname)) {
      // We make inner classes of logical classes not instrumented while logical
      // bases are instrumented...UNLESS there is a explicit spec for said inner class
      boolean adaptable = getSpec(fullClassName) != null || outerClassname.equals(fullClassName);
      return cacheIsAdaptable(fullClassName, adaptable);
    }

    // If a root is defined then we automagically instrument
    if (classContainsAnyRoots(classInfo)) { return cacheIsAdaptable(fullClassName, true); }

    // existing class specs trump config
    if (hasSpec(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }

    InstrumentationDescriptor desc = getInstrumentationDescriptorFor(classInfo);
    return cacheIsAdaptable(fullClassName, desc.isInclude());
  }

  private boolean isTCPatternMatchingHack(ClassInfo classInfo) {
    String fullClassName = classInfo.getName();
    return fullClassName.startsWith("com.tc.") || fullClassName.startsWith("com.terracottatech.");
  }

  public boolean isNeverAdaptable(ClassInfo classInfo) {
    return isTCPatternMatchingHack(classInfo) || permanentExcludesMatcher.match(classInfo)
           || nonportablesMatcher.match(classInfo);
  }

  private InstrumentationDescriptor getInstrumentationDescriptorFor(ClassInfo classInfo) {
    for (Iterator i = this.instrumentationDescriptors.iterator(); i.hasNext();) {
      InstrumentationDescriptor rv = (InstrumentationDescriptor) i.next();
      if (rv.matches(classInfo)) { return rv; }
    }
    return DEFAULT_INSTRUMENTATION_DESCRIPTOR;
  }

  private String outerClassnameWithoutInner(String fullName) {
    int indexOfInner = fullName.indexOf('$');
    return indexOfInner < 0 ? fullName : fullName.substring(0, indexOfInner);
  }

  public boolean isTransient(int modifiers, ClassInfo classInfo, String field) {
    if (ByteCodeUtil.isParent(field)) return true;
    if (ClassAdapterBase.isDelegateFieldName(field)) { return false; }

    String className = classInfo.getName();
    if (Modifier.isTransient(modifiers) && isHonorJavaTransient(classInfo)) return true;

    return transients.contains(className + "." + field);
  }

  public boolean isVolatile(int modifiers, ClassInfo classInfo, String field) {
    return Modifier.isVolatile(modifiers) && isHonorJavaVolatile(classInfo);
  }

  private boolean isHonorJavaTransient(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isHonorTransientSet()) { return spec.isHonorJavaTransient(); }
    return getInstrumentationDescriptorFor(classInfo).isHonorTransient();
  }

  private boolean isHonorJavaVolatile(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isHonorVolatileSet()) { return spec.isHonorVolatile(); }
    return getInstrumentationDescriptorFor(classInfo).isHonorVolatile();
  }

  public boolean isCallConstructorOnLoad(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isCallConstructorSet()) { return spec.isCallConstructorOnLoad(); }
    return getInstrumentationDescriptorFor(classInfo).isCallConstructorOnLoad();
  }

  public String getPreCreateMethodIfDefined(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPreCreateMethod();
    } else {
      return null;
    }
  }

  public String getPostCreateMethodIfDefined(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPostCreateMethod();
    } else {
      return null;
    }
  }

  public String getOnLoadScriptIfDefined(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isExecuteScriptOnLoadSet()) { return spec.getOnLoadExecuteScript(); }
    return getInstrumentationDescriptorFor(classInfo).getOnLoadScriptIfDefined();
  }

  public String getOnLoadMethodIfDefined(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec != null && spec.isCallMethodOnLoadSet()) { return spec.getOnLoadMethod(); }
    return getInstrumentationDescriptorFor(classInfo).getOnLoadMethodIfDefined();
  }

  public Class getTCPeerClass(Class clazz) {
    if (moduleSpecs != null) {
      for (int i = 0; i < moduleSpecs.length; i++) {
        clazz = moduleSpecs[i].getPeerClass(clazz);
      }
    }
    return clazz;
  }

  public boolean isDSOSessions(String name) {
    for (Iterator it = applicationNames.iterator(); it.hasNext();) {
      String appName = (String) it.next();
      if (name.matches(appName.replaceAll("\\*", "\\.\\*"))) return true;
    }
    return false;
  }

  public TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, ClassInfo classInfo,
                                                           InstrumentationLogger lgr, ClassLoader caller,
                                                           final boolean forcePortable, boolean honorTransient) {
    String className = classInfo.getName();
    ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
    TransparencyClassSpec spec = getOrCreateSpec(className);
    spec.setHonorTransient(honorTransient);

    if (forcePortable) {
      if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
        spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
      } else {
        logger.info("Not making " + className + " forcefully portable");
      }
    }

    return new TransparencyClassAdapter(classInfo, basicGetOrCreateSpec(className, null, false), writer, mgrHelper,
                                        lgr, caller, portability);
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller) {
    return this.createClassAdapterFor(writer, classInfo, lgr, caller, false);
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller, final boolean forcePortable) {
    ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
    TransparencyClassSpec spec = getOrCreateSpec(classInfo.getName());

    if (forcePortable) {
      if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
        spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
      } else {
        logger.info("Not making " + classInfo.getName() + " forcefully portable");
      }
    }

    ClassAdapter dsoAdapter = new TransparencyClassAdapter(classInfo, spec, writer, mgrHelper, lgr, caller, portability);
    ClassAdapterFactory factory = spec.getCustomClassAdapter();
    ClassVisitor cv;
    if (factory == null) {
      cv = dsoAdapter;
    } else {
      cv = factory.create(dsoAdapter, caller);
    }

    return new SafeSerialVersionUIDAdder(cv);
  }

  private TransparencyClassSpec basicGetOrCreateSpec(String className, String applicator, boolean rememberSpec) {
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

  public TransparencyClassSpec getOrCreateSpec(String className) {
    return basicGetOrCreateSpec(className, null, true);
  }

  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator) {
    if (applicator == null) throw new AssertionError();
    return basicGetOrCreateSpec(className, applicator, true);
  }

  private void addSpec(TransparencyClassSpec spec) {
    synchronized (specLock) {
      Assert.eval(!classSpecs.containsKey(spec.getClassName()));
      Assert.assertNotNull(spec);
      classSpecs.put(spec.getClassName(), spec);
    }
  }

  public boolean isLogical(String className) {
    TransparencyClassSpec spec = getSpec(className);
    return spec != null && spec.isLogical();
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  public boolean isPortableModuleClass(Class clazz) {
    if (moduleSpecs != null) {
      for (int i = 0; i < moduleSpecs.length; i++) {
        if (moduleSpecs[i].isPortableClass(clazz)) { return true; }
      }
    }
    return false;
  }

  public Class getChangeApplicator(Class clazz) {
    ChangeApplicatorSpec applicatorSpec = null;
    TransparencyClassSpec spec = getSpec(clazz.getName());
    if (spec != null) {
      applicatorSpec = spec.getChangeApplicatorSpec();
    }

    if (applicatorSpec == null) {
      if (moduleSpecs != null) {
        for (int i = 0; i < moduleSpecs.length; i++) {
          Class applicatorClass = moduleSpecs[i].getChangeApplicatorSpec().getChangeApplicator(clazz);
          if (applicatorClass != null) { return applicatorClass; }
        }
      }
      return null;
    }
    return applicatorSpec.getChangeApplicator(clazz);
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  public boolean isUseNonDefaultConstructor(Class clazz) {
    String className = clazz.getName();
    if (literalValues.isLiteral(className)) { return true; }
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) { return spec.isUseNonDefaultConstructor(); }
    if (moduleSpecs != null) {
      for (int i = 0; i < moduleSpecs.length; i++) {
        if (moduleSpecs[i].isUseNonDefaultConstructor(clazz)) { return true; }
      }
    }
    return false;
  }

  public void setModuleSpecs(ModuleSpec[] moduleSpecs) {
    this.moduleSpecs = moduleSpecs;
  }

  /*
   * public String getChangeApplicatorClassNameFor(String className) { TransparencyClassSpec spec = getSpec(className);
   * if (spec == null) return null; return spec.getChangeApplicatorClassName(); }
   */

  public boolean hasSpec(String className) {
    return getSpec(className) != null;
  }

  private boolean hasSpec(ClassInfo classInfo) {
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

  private void scanForMissingClassesDeclaredInConfig(BootJar bootJar) throws BootJarException, IOException {
    int missingCount = 0;
    int preInstrumentedCount = 0;
    Set preinstClasses = bootJar.getAllPreInstrumentedClasses();
    int bootJarPopulation = preinstClasses.size();

    synchronized (specLock) {
      TransparencyClassSpec[] allSpecs = getAllSpecs(true);
      for (int i = 0; i < allSpecs.length; i++) {
        TransparencyClassSpec classSpec = allSpecs[i];
        Assert.assertNotNull(classSpec);
        String cname = classSpec.getClassName().replace('/', '.');
        if (!classSpec.isForeign() && (userDefinedBootSpecs.get(cname) != null)) continue;
        if (classSpec.isPreInstrumented()) {
          preInstrumentedCount++;
          if (!(preinstClasses.contains(classSpec.getClassName()) || classSpec.isHonorJDKSubVersionSpecific())) {
            String message = "* " + classSpec.getClassName() + "... missing";
            missingCount++;
            logger.info(message);
          }
        }
      }
    }

    if (missingCount > 0) {
      logger.info("Number of classes in the DSO boot jar:" + bootJarPopulation);
      logger.info("Number of classes expected to be in the DSO boot jar:" + preInstrumentedCount);
      logger.info("Number of classes found missing from the DSO boot jar:" + missingCount);
      throw new IncompleteBootJarException("Incomplete DSO boot jar; " + missingCount
                                           + " pre-instrumented class(es) found missing.");
    }
  }

  /**
   * This method will: - check the contents of the boot-jar against tc-config.xml - check that all that all the
   * necessary referenced classes are also present in the boot jar
   */
  public void verifyBootJarContents(File bjf) throws UnverifiedBootJarException {
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

  private TransparencyClassSpec[] getAllSpecs(boolean includeBootJarSpecs) {
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

  public void addDistributedMethodCall(DistributedMethodSpec dms) {
    this.distributedMethods.add(dms);
  }

  public DistributedMethodSpec getDmiSpec(MemberInfo memberInfo) {
    if (Modifier.isStatic(memberInfo.getModifiers()) || "<init>".equals(memberInfo.getName())
        || "<clinit>".equals(memberInfo.getName())) { return null; }
    for (Iterator i = distributedMethods.iterator(); i.hasNext();) {
      DistributedMethodSpec dms = (DistributedMethodSpec) i.next();
      if (matches(dms.getMethodExpression(), memberInfo)) { return dms; }
    }
    return null;
  }

  public void addTransient(String className, String fieldName) {
    if ((className == null) || (fieldName == null)) {
      //
      throw new IllegalArgumentException("class " + className + ", field = " + fieldName);
    }
    transients.add(className + "." + fieldName);
  }

  public String toString() {
    return "<StandardDSOClientConfigHelperImpl: " + configSetupManager + ">";
  }

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder) {
    throw new UnsupportedOperationException();
  }

  public void addAspectModule(String classNamePrefix, String moduleName) {
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

  public void addDSOSpringConfig(DSOSpringConfigHelper config) {
    this.springConfigs.add(config);

    synchronized (aspectModules) {
      if (!this.aspectModules.containsKey("org.springframework")) {
        addAspectModule("org.springframework", "com.tc.object.config.SpringAspectModule");
      }
    }
  }

  public Collection getDSOSpringConfigs() {
    return this.springConfigs;
  }

  public String getLogicalExtendingClassName(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec == null || !spec.isLogical()) { return null; }
    return spec.getLogicalExtendingClassName();
  }

  public void addApplicationName(String name) {
    applicationNames.add(name);
  }

  public void addSynchronousWriteApplication(String name) {
    this.synchronousWriteApplications.add(name);
  }

  public void addUserDefinedBootSpec(String className, TransparencyClassSpec spec) {
    synchronized (specLock) {
      userDefinedBootSpecs.put(className, spec);
    }
  }

  public void addRepository(String location) {
    modulesContext.modules.addRepository(location);
  }

  public void addModule(String name, String version) {
    Module newModule = modulesContext.modules.addNewModule();
    newModule.setName(name);
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

    void setModules(Modules modules) {
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

  public int getSessionLockType(String appName) {
    for (Iterator iter = synchronousWriteApplications.iterator(); iter.hasNext();) {
      String webApp = (String) iter.next();
      if (webApp.equals(appName)) { return LockLevel.SYNCHRONOUS_WRITE; }
    }
    return LockLevel.WRITE;
  }

  public static InputStream getL1PropertiesFromL2Stream(ConnectionInfo[] connectInfo) throws Exception {
    URLConnection connection = null;
    InputStream l1PropFromL2Stream = null;
    URL theURL = null;
    for (int i = 0; i < connectInfo.length; i++) {
      ConnectionInfo ci = connectInfo[i];
      try {
        theURL = new URL("http", ci.getHostname(), ci.getPort(), "/l1reconnectproperties");
        String text = "Trying to get L1 Reconnect Properties from " + theURL.toString();
        logger.info(text);
        connection = theURL.openConnection();
        l1PropFromL2Stream = connection.getInputStream();
        if (l1PropFromL2Stream != null) return l1PropFromL2Stream;
      } catch (IOException e) {
        String text = "Cannot connect to [" + ci + "].";
        boolean tryAgain = (i < connectInfo.length - 1);
        if (tryAgain) text += " Will retry next server.";
        logger.warn(text);
      }
    }
    return null;
  }

  private void setupL1ReconnectProperties() {
    InputStream in = null;
    String serverList = "";
    boolean loggedInConsole = false;

    PreparedComponentsFromL2Connection serverInfos = new PreparedComponentsFromL2Connection(configSetupManager);
    ConnectionInfoConfigItem connectInfo = (ConnectionInfoConfigItem) serverInfos.createConnectionInfoConfigItem();
    ConnectionInfo[] connections = (ConnectionInfo[]) connectInfo.getObject();

    for (int i = 0; i < connections.length; i++) {
      if (serverList.length() > 0) serverList += ", ";
      serverList += connections[i];
    }
    String text = "Cannot connect to " + (connections.length > 1 ? "any of the servers" : "server") + "[" + serverList
                  + "]. Retrying...\n";

    while (in == null) {
      try {
        in = getL1PropertiesFromL2Stream(connections);

        if (in == null) {
          if (loggedInConsole == false) {
            consoleLogger.warn(text);
            loggedInConsole = true;
          }
          if (connections.length > 1) logger.warn(text);
          ThreadUtil.reallySleep(1000);
        }
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    L1ReconnectPropertiesDocument l1ReconnectPropFromL2;
    try {
      l1ReconnectPropFromL2 = L1ReconnectPropertiesDocument.Factory.parse(in);
    } catch (Exception e) {
      throw new AssertionError(e);
    }

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

  public synchronized ReconnectConfig getL1ReconnectProperties() {
    if (l1ReconnectConfig == null) setupL1ReconnectProperties();
    return l1ReconnectConfig;
  }

}
