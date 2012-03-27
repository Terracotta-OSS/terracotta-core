/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;
import org.terracotta.groupConfigForL1.ServerInfo;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.backport175.bytecode.AnnotationElement.Annotation;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.injection.DsoClusterInjectionInstrumentation;
import com.tc.injection.InjectionInstrumentation;
import com.tc.injection.InjectionInstrumentationRegistry;
import com.tc.injection.exceptions.UnsupportedInjectedDsoInstanceTypeException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.ConnectionInfo;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.bytecode.AddInterfacesAdapter;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterBase;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.DelegateMethodAdapter;
import com.tc.object.bytecode.NotClearable;
import com.tc.object.bytecode.SafeSerialVersionUIDAdder;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.L1ReconnectConfigImpl;
import com.tc.properties.ReconnectConfig;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.tc.util.UUID;
import com.tc.util.runtime.Vm;
import com.terracottatech.config.L1ReconnectPropertiesDocument;

import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StandardDSOClientConfigHelperImpl implements DSOClientConfigHelper {

  private static final TCLogger                              logger                             = CustomerLogging
                                                                                                    .getDSOGenericLogger();

  private static final InstrumentationDescriptor             DEFAULT_INSTRUMENTATION_DESCRIPTOR = new NullInstrumentationDescriptor();

  private final DSOClientConfigHelperLogger                  helperLogger;
  private final L1ConfigurationSetupManager                  configSetupManager;
  private final UUID                                         id;

  private final List                                         locks                              = new CopyOnWriteArrayList();
  private final List                                         roots                              = new CopyOnWriteArrayList();
  private final Set                                          transients                         = Collections
                                                                                                    .synchronizedSet(new HashSet());
  private final Map<String, String>                          injectedFields                     = new ConcurrentHashMap<String, String>();
  private final CompoundExpressionMatcher                    permanentExcludesMatcher;
  private final CompoundExpressionMatcher                    nonportablesMatcher;
  private final List                                         autoLockExcludes                   = new CopyOnWriteArrayList();
  private final List                                         distributedMethods                 = new CopyOnWriteArrayList();
  private final ExpressionHelper                             expressionHelper;
  private final Map                                          adaptableCache                     = Collections
                                                                                                    .synchronizedMap(new HashMap());

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
  private final Map                                          aspectModules                      = new ConcurrentHashMap();
  private final Portability                                  portability;
  private int                                                faultCount                         = -1;
  private final Set<String>                                  tunneledMBeanDomains               = Collections
                                                                                                    .synchronizedSet(new HashSet<String>());
  private ReconnectConfig                                    l1ReconnectConfig                  = null;
  private final InjectionInstrumentationRegistry             injectionRegistry                  = new InjectionInstrumentationRegistry();

  public StandardDSOClientConfigHelperImpl(final boolean initializedModulesOnlyOnce,
                                           final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager);
  }

  public StandardDSOClientConfigHelperImpl(final L1ConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;
    this.id = UUID.getUUID();
    helperLogger = new DSOClientConfigHelperLogger(logger);
    // this.classInfoFactory = new ClassInfoFactory();
    this.expressionHelper = new ExpressionHelper();

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

    try {
      doPreInstrumentedAutoconfig();
      doAutoconfig();
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    logger.debug("roots: " + this.roots);
    logger.debug("locks: " + this.locks);
    logger.debug("distributed-methods: " + this.distributedMethods);
  }

  public String rawConfigText() {
    return configSetupManager.rawConfigText();
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

  public void addIncludePattern(String expression, boolean honorTransient, String methodToCallOnLoad) {
    addIncludePattern(expression, honorTransient, methodToCallOnLoad, false);
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
    getOrCreateSpec("com.tcclient.object.DistributedMethodCall");
    markAllSpecsPreInstrumented();
  }

  private void doAutoconfig() throws Exception {
    TransparencyClassSpec spec;

    spec = getOrCreateSpec("java.lang.Object");
    spec.setCallConstructorOnLoad(true);
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
    if (null == instrumentation) { throw new UnsupportedInjectedDsoInstanceTypeException(classInfo.getName(),
                                                                                         fi.getName(), fi.getType()
                                                                                             .getName()); }

    TransparencyClassSpec spec = getOrCreateSpec(classInfo.getName());
    spec.setHasOnLoadInjection(true);
    addCustomAdapter(classInfo.getName(), instrumentation.getClassAdapterFactoryForFieldInjection(fi));
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

  public Collection<ClassAdapterFactory> getAfterDSOAdapters(ClassInfo classInfo) {
    TransparencyClassSpec spec = getSpec(classInfo.getName());
    if (spec == null) {
      return Collections.EMPTY_LIST;
    } else {
      return spec.getAfterDSOClassAdapters();
    }
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

  public boolean shouldBeAdapted(final ClassInfo classInfo) {
    // now check if class is adaptable
    String fullClassName = classInfo.getName();

    Boolean cache = readAdaptableCache(fullClassName);
    if (cache != null) { return cache.booleanValue(); }

    // @see isTCPatternMatchingHack() note elsewhere
    if (isTCPatternMatchingHack(classInfo) || permanentExcludesMatcher.match(classInfo)) {
      // permanent Excludes
      return cacheIsAdaptable(fullClassName, false);
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

    return new SafeSerialVersionUIDAdder(cv);
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

  public Class getChangeApplicator(final Class clazz) {
    ChangeApplicatorSpec applicatorSpec = null;
    TransparencyClassSpec spec = getSpec(clazz.getName());
    if (spec != null) {
      applicatorSpec = spec.getChangeApplicatorSpec();
    }

    if (applicatorSpec == null) { return null; }
    return applicatorSpec.getChangeApplicator(clazz);
  }

  // TODO: Need to optimize this by identifying the module to query instead of querying all the modules.
  public boolean isUseNonDefaultConstructor(final Class clazz) {
    String className = clazz.getName();
    if (LiteralValues.isLiteral(className)) { return true; }
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) { return spec.isUseNonDefaultConstructor(); }
    return false;
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

  @Override
  public void addDistributedMethod(String expression) {
    addDistributedMethodCall(new DistributedMethodSpec(expression, false));
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

  public void addUserDefinedBootSpec(final String className, final TransparencyClassSpec spec) {
    synchronized (specLock) {
      userDefinedBootSpecs.put(className, spec);
    }
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
                                                         connectionInfo[j].getPort(), i * j + j,
                                                         connectionInfo[j].getGroupName());
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

  @Override
  public void addDelegateMethodAdapter(String type, String delegateType, String delegateField) {
    addCustomAdapter(type, new DelegateMethodAdapter(delegateType, delegateField));
  }

  @Override
  public void addNotClearableAdapter(String type) {
    String iface = NotClearable.class.getName().replace('.', '/');
    addCustomAdapter(type, new AddInterfacesAdapter(new String[] { iface }));
  }

  public L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException {
    configSetupManager.reloadServersConfiguration();
    return configSetupManager;
  }

  public String[] processArguments() {
    return configSetupManager.processArguments();
  }
}
