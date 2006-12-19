/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

import org.apache.commons.lang.ArrayUtils;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.SerialVersionUIDAdder;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ConstructorInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.aspectwerkz.reflect.MethodInfo;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.geronimo.transform.HostGBeanAdapter;
import com.tc.geronimo.transform.MultiParentClassLoaderAdapter;
import com.tc.geronimo.transform.ProxyMethodInterceptorAdapter;
import com.tc.geronimo.transform.TomcatClassLoaderAdapter;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.AbstractListMethodCreator;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.DSOUnsafeAdapter;
import com.tc.object.bytecode.JavaLangReflectArrayAdapter;
import com.tc.object.bytecode.JavaLangReflectFieldAdapter;
import com.tc.object.bytecode.JavaUtilWeakHashMapAdapter;
import com.tc.object.bytecode.ManagerHelper;
import com.tc.object.bytecode.ManagerHelperFactory;
import com.tc.object.bytecode.THashMapAdapter;
import com.tc.object.bytecode.TableModelMethodAdapter;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TreeMapAdapter;
import com.tc.object.bytecode.UnsafeAdapter;
import com.tc.object.bytecode.aspectwerkz.AsmConstructorInfo;
import com.tc.object.bytecode.aspectwerkz.AsmMethodInfo;
import com.tc.object.bytecode.aspectwerkz.ClassInfoFactory;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
import com.tc.object.bytecode.struts.IncludeTagAdapter;
import com.tc.object.config.schema.AppContext;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.ExcludedInstrumentedClass;
import com.tc.object.config.schema.IncludeOnLoad;
import com.tc.object.config.schema.IncludedInstrumentedClass;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.config.schema.LockLevel;
import com.tc.object.config.schema.NewDSOApplicationConfig;
import com.tc.object.config.schema.NewSpringApplicationConfig;
import com.tc.object.config.schema.SpringApp;
import com.tc.object.config.schema.SpringContextBean;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.tools.BootJar;
import com.tc.tomcat.transform.BootstrapAdapter;
import com.tc.tomcat.transform.CatalinaAdapter;
import com.tc.tomcat.transform.ContainerBaseAdapter;
import com.tc.tomcat.transform.JspWriterImplAdapter;
import com.tc.tomcat.transform.TomcatLoaderAdapter;
import com.tc.tomcat.transform.WebAppLoaderAdapter;
import com.tc.util.Assert;
import com.tc.util.ClassUtils;
import com.tc.util.ClassUtils.ClassSpec;
import com.tc.util.runtime.Vm;
import com.tc.weblogic.transform.EJBCodeGeneratorAdapter;
import com.tc.weblogic.transform.GenericClassLoaderAdapter;
import com.tc.weblogic.transform.ServerAdapter;
import com.tc.weblogic.transform.ServletResponseImplAdapter;
import com.tc.weblogic.transform.TerracottaServletResponseImplAdapter;
import com.tc.weblogic.transform.WebAppServletContextAdapter;
import com.tcclient.util.DSOUnsafe;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The standard implementation of {@link DSOClientConfigHelper}.
 */
public class StandardDSOClientConfigHelper implements DSOClientConfigHelper {
  private static final LiteralValues             literalValues                      = new LiteralValues();

  private static final TCLogger                  logger                             = CustomerLogging
                                                                                        .getDSOGenericLogger();
  private static final Class[]                   ADAPTER_CSTR_SIGNATURE             = new Class[] { ClassVisitor.class,
      ClassLoader.class                                                            };
  private static final InstrumentationDescriptor DEAFULT_INSTRUMENTATION_DESCRIPTOR = new NullInstrumentationDescriptor();

  private final ManagerHelperFactory             mgrHelperFactory                   = new ManagerHelperFactory();
  private final DSOClientConfigHelperLogger      helperLogger;

  private final L1TVSConfigurationSetupManager   configSetupManager;

  private Lock[]                                 locks;
  private final Map                              roots                              = new ConcurrentHashMap();
  private final Map                              types;

  private final LinkedList                       instrumentationDescriptors         = new LinkedList();

  private final String[]                         applications;
  private final CompoundExpressionMatcher        permanentExcludesMatcher;
  private final CompoundExpressionMatcher        nonportablesMatcher;
  private final List                             autoLockExcludes                   = new ArrayList();
  private final List                             distributedMethods                 = new LinkedList();
  private final Map                              userDefinedBootSpecs               = new HashMap();

  private final ClassInfoFactory                 classInfoFactory;
  private final ExpressionHelper                 expressionHelper;

  private final Map                              adaptableCache                     = new HashMap();

  private final Map                              classSpecs                         = Collections
                                                                                        .synchronizedMap(new HashMap());

  private final Map                              customAdapters                     = new ConcurrentHashMap();

  private final Map                              aspectModules                      = Collections
                                                                                        .synchronizedMap(new HashMap());

  private final List                             springConfigs                      = Collections
                                                                                        .synchronizedList(new ArrayList());

  private final boolean                          supportSharingThroughReflection;

  private final Portability                      portability;

  private int                                    faultCount                         = -1;

  public StandardDSOClientConfigHelper(L1TVSConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
  }

  public StandardDSOClientConfigHelper(L1TVSConfigurationSetupManager configSetupManager, boolean interrogateBootJar)
      throws ConfigurationSetupException {
    this(configSetupManager, new ClassInfoFactory(), new ExpressionHelper(), interrogateBootJar);
  }

  public StandardDSOClientConfigHelper(L1TVSConfigurationSetupManager configSetupManager,
                                       ClassInfoFactory classInfoFactory, ExpressionHelper eh,
                                       boolean interrogateBootJar) throws ConfigurationSetupException {
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;
    helperLogger = new DSOClientConfigHelperLogger(logger);
    this.classInfoFactory = classInfoFactory;
    this.expressionHelper = eh;

    permanentExcludesMatcher = new CompoundExpressionMatcher();
    // TODO:: come back and add all possible non-portable/non-adaptable classes here. This is by no means exhaustive !

    // XXX:: There is a bug in aspectwerkz com.tc..* matches both com.tc and com.tctest classes. As a work around
    // this is commented and isTCPatternMatchingHack() method is added instead. When that bug is fixed, uncomment
    // this and remove isTCPatternMatchingHack();
    // addPermanentExcludePattern("com.tc..*");
    // addPermanentExcludePattern("com.terracottatech..*");
    addPermanentExcludePattern("java.awt.Component");
    addPermanentExcludePattern("java.lang.Object");
    addPermanentExcludePattern("java.lang.Thread");
    addPermanentExcludePattern("java.lang.Process");
    addPermanentExcludePattern("java.lang.ClassLoader");
    addPermanentExcludePattern("java.lang.Runtime");
    addPermanentExcludePattern("java.io.FileReader");
    addPermanentExcludePattern("java.io.FileWriter");
    addPermanentExcludePattern("java.io.FileDescriptor");
    addPermanentExcludePattern("java.io.FileInputStream");
    addPermanentExcludePattern("java.io.FileOutputStream");
    addPermanentExcludePattern("java.net.DatagramSocket");
    addPermanentExcludePattern("java.net.DatagramSocketImpl");
    addPermanentExcludePattern("java.net.MulticastSocket");
    addPermanentExcludePattern("java.net.ServerSocket");
    addPermanentExcludePattern("java.net.Socket");
    addPermanentExcludePattern("java.net.SocketImpl");
    addPermanentExcludePattern("java.nio.channels.DatagramChannel");
    addPermanentExcludePattern("java.nio.channels.FileChannel");
    addPermanentExcludePattern("java.nio.channels.FileLock");
    addPermanentExcludePattern("java.nio.channels.ServerSocketChannel");
    addPermanentExcludePattern("java.nio.channels.SocketChannel");
    addPermanentExcludePattern("java.util.logging.FileHandler");
    addPermanentExcludePattern("java.util.logging.SocketHandler");

    addAutoLockExcludePattern("* java.lang.Throwable.*(..)");

    nonportablesMatcher = new CompoundExpressionMatcher();
    addNonportablePattern("javax.servlet.GenericServlet");

    NewDSOApplicationConfig appConfig = configSetupManager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);
    NewSpringApplicationConfig springConfig = configSetupManager
        .springApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);

    appConfig.changesInItemForbidden(appConfig.roots());
    com.tc.object.config.schema.Root[] configRoots = (com.tc.object.config.schema.Root[]) appConfig.roots().getObject();
    if (configRoots == null) {
      configRoots = new com.tc.object.config.schema.Root[] {};
    }

    for (int i = 0; i < configRoots.length; ++i) {
      try {
        ClassSpec classSpec = ClassUtils.parseFullyQualifiedFieldName(configRoots[i].fieldName());
        String className = classSpec.getFullyQualifiedClassName();
        String fieldName = classSpec.getShortFieldName();
        String rootName = configRoots[i].rootName();
        addRoot(className, fieldName, rootName, false);
      } catch (ParseException pe) {
        throw new ConfigurationSetupException("Root '" + configRoots[i].fieldName() + "' is invalid", pe);
      }
    }
    logger.debug("roots: " + roots);

    Set applicationNames = new HashSet();
    appConfig.changesInItemForbidden(appConfig.webApplications());
    if (appConfig.webApplications().getStringArray() != null) {
      applicationNames.addAll(Arrays.asList(appConfig.webApplications().getStringArray()));
    }

    appConfig.changesInItemForbidden(appConfig.locks());
    List lockList = new ArrayList();

    addLocks(lockList, (com.tc.object.config.schema.Lock[]) appConfig.locks().getObject());

    this.types = new HashMap();

    appConfig.changesInItemForbidden(appConfig.transientFields());
    addTransientFields(appConfig.transientFields().getStringArray());

    SpringApp[] springApps = (SpringApp[]) springConfig.springApps().getObjects();
    for (int i = 0; springApps != null && i < springApps.length; i++) {
      SpringApp springApp = springApps[i];
      if (springApp != null) {
        addSpringApp(springApp, applicationNames, lockList);
      }
    }

    this.applications = (String[]) applicationNames.toArray(new String[applicationNames.size()]);
    logger.debug("web-applications: " + applicationNames);

    logger.debug("transients: " + types);

    logger.debug("locks: " + lockList);

    this.locks = (Lock[]) lockList.toArray(new Lock[lockList.size()]);
    rewriteHashtableAutLockSpecIfNecessary();

    // process includes and excludes
    appConfig.changesInItemForbidden(appConfig.instrumentedClasses());
    addInstrumentedClasses((InstrumentedClass[]) appConfig.instrumentedClasses().getObject());

    appConfig.changesInItemForbidden(appConfig.distributedMethods());
    if (appConfig.distributedMethods().getStringArray() != null) {
      this.distributedMethods.addAll(Arrays.asList(appConfig.distributedMethods().getStringArray()));
    }
    logger.debug("distributed-methods: " + ArrayUtils.toString(this.distributedMethods));

    Set userDefinedBootClassNames = new HashSet();

    appConfig.changesInItemForbidden(appConfig.additionalBootJarClasses());
    if (appConfig.additionalBootJarClasses().getStringArray() != null) {
      userDefinedBootClassNames.addAll(Arrays.asList(appConfig.additionalBootJarClasses().getStringArray()));
    }
    logger.debug("boot-jar/includes: " + ArrayUtils.toString(userDefinedBootClassNames));

    supportSharingThroughReflection = appConfig.supportSharingThroughReflection().getBoolean();
    try {
      doAutoconfig(interrogateBootJar);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    for (Iterator i = userDefinedBootClassNames.iterator(); i.hasNext();) {
      String className = (String) i.next();
      if (getSpec(className) == null) {
        TransparencyClassSpec spec = new TransparencyClassSpec(className, this);
        spec.markPreInstrumented();
        userDefinedBootSpecs.put(spec.getClassName(), spec);
      }
    }

    // process classes
  }

  private void addSpringApp(SpringApp springApp, Set appNames, List sLocks) throws ConfigurationSetupException {
    // TODO scope the following by app namespace https://jira.terracotta.lan/jira/browse/LKC-2284
    addInstrumentedClasses(springApp.includes());
    addLocks(sLocks, springApp.locks());
    addTransientFields(springApp.transientFields());

    if (springApp.sessionSupport()) {
      appNames.add(springApp.name()); // enable session support
    }

    AppContext[] appContexts = springApp.appContexts();
    for (int j = 0; appContexts != null && j < appContexts.length; j++) {
      AppContext appContext = appContexts[j];
      if (appContext == null) continue;

      DSOSpringConfigHelper springConfigHelper = new StandardDSOSpringConfigHelper();
      springConfigHelper.addApplicationNamePattern(springApp.name());
      springConfigHelper.setFastProxyEnabled(springApp.fastProxy()); // copy flag to all subcontexts

      String[] distributedEvents = appContext.distributedEvents();
      for (int k = 0; distributedEvents != null && k < distributedEvents.length; k++) {
        springConfigHelper.addDistributedEvent(distributedEvents[k]);
      }

      String[] paths = appContext.paths();
      for (int k = 0; paths != null && k < paths.length; k++) {
        if (paths[k] != null) {
          springConfigHelper.addConfigPattern(paths[k]);
        }
      }

      SpringContextBean[] beans = appContext.beans();
      for (int k = 0; beans != null && k < beans.length; k++) {
        SpringContextBean bean = beans[k];
        if (bean != null) {
          springConfigHelper.addBean(bean.name());
          String[] fields = bean.nonDistributedFields();
          for (int l = 0; fields != null && l < fields.length; l++) {
            if (fields[l] != null) {
              springConfigHelper.excludeField(bean.name(), fields[l]);
            }
          }
        }
      }

      addDSOSpringConfig(springConfigHelper);
    }
  }

  public Portability getPortability() {
    return this.portability;
  }

  private void addTransientFields(String[] configTransients) throws ConfigurationSetupException {
    if (configTransients != null && configTransients.length > 0) {
      try {
        this.types.putAll(new TypeMap(configTransients).getTypes());
      } catch (ParseException e) {
        throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
      }
    }
  }

  private void addInstrumentedClasses(InstrumentedClass[] configInstrumentedClasses) {
    if (configInstrumentedClasses == null) return;

    for (int i = 0; i < configInstrumentedClasses.length; i++) {
      InstrumentedClass classDesc = configInstrumentedClasses[i];
      InstrumentationDescriptor instrumentationDescriptor = newInstrumentationDescriptor(classDesc);

      synchronized (this.instrumentationDescriptors) {
        this.instrumentationDescriptors.addFirst(instrumentationDescriptor);
      }
    }
  }

  private void addLocks(List lockList, com.tc.object.config.schema.Lock[] configLocks) {
    if (configLocks == null) return;

    for (int i = 0; i < configLocks.length; ++i) {
      LockLevel inLevel = configLocks[i].lockLevel();

      ConfigLockLevel outLevel;
      if (inLevel.equals(LockLevel.CONCURRENT)) outLevel = ConfigLockLevel.CONCURRENT;
      else if (inLevel.equals(LockLevel.READ)) outLevel = ConfigLockLevel.READ;
      else if (inLevel.equals(LockLevel.WRITE)) outLevel = ConfigLockLevel.WRITE;
      else throw Assert.failure("Unknown lock level " + inLevel);

      LockDefinition definition;
      if (configLocks[i].isAutoLock()) {
        definition = new LockDefinition(LockDefinition.TC_AUTOLOCK_NAME, outLevel);
      } else {
        definition = new LockDefinition(configLocks[i].lockName(), outLevel);
      }
      definition.commit();

      lockList.add(new Lock(configLocks[i].methodExpression(), definition));
    }
  }

  private void addAutoLockExcludePattern(String expression) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    autoLockExcludes.add(visitor);
  }

  private void addPermanentExcludePattern(String pattern) {
    permanentExcludesMatcher.add(new ClassExpressionMatcherImpl(classInfoFactory, expressionHelper, pattern));
  }

  private void addNonportablePattern(String pattern) {
    nonportablesMatcher.add(new ClassExpressionMatcherImpl(classInfoFactory, expressionHelper, pattern));
  }

  private InstrumentationDescriptor newInstrumentationDescriptor(InstrumentedClass classDesc) {
    ClassExpressionMatcher classExpressionMatcher = new ClassExpressionMatcherImpl(classInfoFactory, expressionHelper,
                                                                                   classDesc.classExpression());

    InstrumentationDescriptor instrumentationDescriptor = new InstrumentationDescriptorImpl(classDesc,
                                                                                            classExpressionMatcher);
    return instrumentationDescriptor;
  }

  // This is used only for tests right now
  public void addIncludePattern(String expression) {
    addIncludePattern(expression, false, false, false);
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
    InstrumentedClass classDesc = new IncludedInstrumentedClass(expression, honorTransient, honorVolatile, onLoad);
    InstrumentationDescriptor descriptor = newInstrumentationDescriptor(classDesc);
    synchronized (this.instrumentationDescriptors) {
      this.instrumentationDescriptors.addFirst(descriptor);
    }

    clearAdaptableCache();
  }

  public void addIncludeAndLockIfRequired(String expression, boolean honorTransient,
                                          boolean oldStyleCallConstructorOnLoad, boolean honorVolatile,
                                          String lockExpression) {
    // The addition of the lock expression and the include need to be atomic -- see LKC-2616

    synchronized (this.instrumentationDescriptors) {
      // TODO see LKC-1893. Need to check for primitive types, logically managed classes, etc.
      if (!hasIncludeExcludePattern(expression)) {
        // only add include if not specified in tc-config
        addIncludePattern(expression, honorTransient, oldStyleCallConstructorOnLoad, honorVolatile);
        addWriteAutolock(lockExpression);
      }
    }
  }

  // This is used only for tests right now
  public void addExcludePattern(String expression) {
    InstrumentedClass classDesc = new ExcludedInstrumentedClass(expression);

    InstrumentationDescriptor descriptor = newInstrumentationDescriptor(classDesc);

    synchronized (this.instrumentationDescriptors) {
      this.instrumentationDescriptors.addFirst(descriptor);
    }
  }

  public boolean hasIncludeExcludePatterns() {
    synchronized (this.instrumentationDescriptors) {
      return !this.instrumentationDescriptors.isEmpty();
    }
  }

  public boolean hasIncludeExcludePattern(String className) {
    return getInstrumentationDescriptorFor(className) != DEAFULT_INSTRUMENTATION_DESCRIPTOR;
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

  private void doAutoconfig(boolean interrogateBootJar) {
    // Table model stuff
    addIncludePattern("javax.swing.table.AbstractTableModel", true);
    TransparencyClassSpec spec = getOrCreateSpec("javax.swing.table.AbstractTableModel");
    spec.addMethodAdapter(TableModelMethodAdapter.METHOD, new TableModelMethodAdapter());
    spec.addTransient("listenerList");

    spec = getOrCreateSpec("javax.swing.table.DefaultTableModel");
    spec.setCallConstructorOnLoad(true);
    LockDefinition ld = new LockDefinition("tcdefaultTableLock", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* javax.swing.table.DefaultTableModel.set*(..)", ld);
    addLock("* javax.swing.table.DefaultTableModel.insert*(..)", ld);
    addLock("* javax.swing.table.DefaultTableModel.move*(..)", ld);
    addLock("* javax.swing.table.DefaultTableModel.remove*(..)", ld);

    ld = new LockDefinition("tcdefaultTableLock", ConfigLockLevel.READ);
    ld.commit();
    addLock("* javax.swing.table.DefaultTableModel.get*(..)", ld);

    spec = getOrCreateSpec("javax.swing.DefaultListModel");
    spec.setCallConstructorOnLoad(true);

    ld = new LockDefinition("tcdefaultListLock", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* javax.swing.DefaultListModel.*(..)", ld);

    addIncludePattern("java.awt.Color", true);
    spec = getOrCreateSpec("java.awt.Color");
    spec.addTransient("cs");

    spec = getOrCreateSpec("java.awt.event.MouseMotionAdapter");
    spec = getOrCreateSpec("java.awt.event.MouseAdapter");

    // java.awt.point
    spec = getOrCreateSpec("java.awt.Point");
    spec = getOrCreateSpec("java.awt.geom.Point2D");
    spec = getOrCreateSpec("java.awt.geom.Point2D$Double");
    spec = getOrCreateSpec("java.awt.geom.Point2D$Float");
    // end java.awt.Point

    // java.awt.geom.Line
    spec = getOrCreateSpec("java.awt.geom.Line2D");
    spec = getOrCreateSpec("java.awt.geom.Line2D$Double");
    spec = getOrCreateSpec("java.awt.geom.Line2D$Float");
    // end java.awt.geom.Line

    // java.awt.Rectangle
    spec = getOrCreateSpec("java.awt.Rectangle");
    spec = getOrCreateSpec("java.awt.geom.Rectangle2D");
    spec = getOrCreateSpec("java.awt.geom.RectangularShape");
    spec = getOrCreateSpec("java.awt.geom.Rectangle2D$Double");
    spec = getOrCreateSpec("java.awt.geom.Rectangle2D$Float");
    spec = getOrCreateSpec("java.awt.geom.RoundRectangle2D");
    spec = getOrCreateSpec("java.awt.geom.RoundRectangle2D$Double");
    spec = getOrCreateSpec("java.awt.geom.RoundRectangle2D$Float");
    // end java.awt.Rectangle

    // java.awt.geom.Ellipse2D
    spec = getOrCreateSpec("java.awt.geom.Ellipse2D");
    spec = getOrCreateSpec("java.awt.geom.Ellipse2D$Double");
    spec = getOrCreateSpec("java.awt.geom.Ellipse2D$Float");
    // end java.awt.geom.Ellipse2D

    // java.awt.geom.Path2D
    if (Vm.isJDK16()) {
      spec = getOrCreateSpec("java.awt.geom.Path2D");
      spec = getOrCreateSpec("java.awt.geom.Path2D$Double");
      spec = getOrCreateSpec("java.awt.geom.Path2D$Float");
    }
    // end java.awt.geom.Path2D

    // java.awt.geom.GeneralPath
    spec = getOrCreateSpec("java.awt.geom.GeneralPath");
    // end java.awt.geom.GeneralPath

    // java.awt.BasicStroke
    spec = getOrCreateSpec("java.awt.BasicStroke");
    // end java.awt.BasicStroke

    // java.awt.Dimension
    spec = getOrCreateSpec("java.awt.Dimension");
    spec = getOrCreateSpec("java.awt.geom.Dimension2D");
    // end java.awt.Dimension

    spec = getOrCreateSpec("javax.swing.tree.DefaultTreeModel");
    addIncludePattern("javax.swing.tree.DefaultMutableTreeNode", false);
    ld = new LockDefinition("tctreeLock", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* javax.swing.tree.DefaultTreeModel.get*(..)", ld);
    addLock("* javax.swing.tree.DefaultTreeModel.set*(..)", ld);
    addLock("* javax.swing.tree.DefaultTreeModel.insert*(..)", ld);

    spec.addTransient("listenerList");
    spec.addDistributedMethodCall("fireTreeNodesChanged",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V");
    spec.addDistributedMethodCall("fireTreeNodesInserted",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V");
    spec.addDistributedMethodCall("fireTreeNodesRemoved",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V");
    spec.addDistributedMethodCall("fireTreeStructureChanged",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V");
    spec.addDistributedMethodCall("fireTreeStructureChanged", "(Ljava/lang/Object;Ljavax/swing/tree/TreePath;)V");

    spec = getOrCreateSpec("javax.swing.AbstractListModel");
    spec.addTransient("listenerList");
    spec.addDistributedMethodCall("fireContentsChanged", "(Ljava/lang/Object;II)V");
    spec.addDistributedMethodCall("fireIntervalAdded", "(Ljava/lang/Object;II)V");
    spec.addDistributedMethodCall("fireIntervalRemoved", "(Ljava/lang/Object;II)V");

    spec = getOrCreateSpec("java.util.Arrays");
    spec = getOrCreateSpec("java.util.Arrays$ArrayList");

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
    /*
     * spec.addSupportMethodCreator(new HashtableMethodCreator());
     * spec.addHashtablePutLogSpec(SerializationUtil.PUT_SIGNATURE);
     * spec.addHashtableRemoveLogSpec(SerializationUtil.REMOVE_KEY_SIGNATURE);
     * spec.addHashtableClearLogSpec(SerializationUtil.CLEAR_SIGNATURE);
     * spec.addMethodAdapter("entrySet()Ljava/util/Set;", new HashtableAdapter.EntrySetAdapter());
     * spec.addMethodAdapter("keySet()Ljava/util/Set;", new HashtableAdapter.KeySetAdapter());
     * spec.addMethodAdapter("values()Ljava/util/Collection;", new HashtableAdapter.ValuesAdapter());
     */
    // addWriteAutolock("synchronized * java.util.Hashtable.*(..)");
    // addReadAutolock(new String[] { "synchronized * java.util.Hashtable.get(..)",
    // "synchronized * java.util.Hashtable.hashCode(..)", "synchronized * java.util.Hashtable.contains*(..)",
    // "synchronized * java.util.Hashtable.elements(..)", "synchronized * java.util.Hashtable.equals(..)",
    // "synchronized * java.util.Hashtable.isEmpty(..)", "synchronized * java.util.Hashtable.keys(..)",
    // "synchronized * java.util.Hashtable.size(..)", "synchronized * java.util.Hashtable.toString(..)" });
    spec = getOrCreateSpec("java.util.Properties", "com.tc.object.applicator.PartialHashMapApplicator");
    addWriteAutolock("synchronized * java.util.Properties.*(..)");

    spec = getOrCreateSpec("com.tcclient.util.MapEntrySetWrapper$EntryWrapper");

    spec = getOrCreateSpec("java.util.IdentityHashMap", "com.tc.object.applicator.HashMapApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.PUT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_KEY_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);

    spec = getOrCreateSpec("java.util.BitSet");
    spec.setHonorTransient(false);

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
    spec = getOrCreateSpec("java.util.AbstractMap");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec = getOrCreateSpec("java.util.AbstractList");
    spec.setHonorTransient(true);
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec.addSupportMethodCreator(new AbstractListMethodCreator());
    spec = getOrCreateSpec("java.util.AbstractSet");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec = getOrCreateSpec("java.util.AbstractSequentialList");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);
    spec = getOrCreateSpec("java.util.Dictionary");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    // spec = getOrCreateSpec("java.lang.Number");
    // This hack is needed to make Number work in all platforms. Without this hack, if you add Number in bootjar, the
    // JVM crashes.
    // spec.generateNonStaticTCFields(false);

    spec = getOrCreateSpec("java.lang.Exception");
    spec = getOrCreateSpec("java.lang.RuntimeException");
    spec = getOrCreateSpec("java.lang.InterruptedException");
    spec = getOrCreateSpec("java.awt.AWTException");
    spec = getOrCreateSpec("java.io.IOException");
    spec = getOrCreateSpec("java.io.FileNotFoundException");
    spec = getOrCreateSpec("java.lang.Error");
    spec = getOrCreateSpec("java.util.ConcurrentModificationException");
    spec = getOrCreateSpec("java.util.NoSuchElementException");

    spec = getOrCreateSpec("java.util.EventObject");
    // spec.setHonorTransient(true);

    spec = getOrCreateSpec("com.tcclient.object.Client");
    spec = getOrCreateSpec("com.tcclient.object.DistributedMethodCall");

    spec = getOrCreateSpec("java.io.File");

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

    spec = getOrCreateSpec("java.util.WeakHashMap");
    addCustomAdapter("java.util.WeakHashMap", JavaUtilWeakHashMapAdapter.class);

    addReflectionPreInstrumentedSpec();

    addJDK15PreInstrumentedSpec();

    /* ******* ALL ABOVE SPECS ARE PRE-INSTRUMENTED ******* */
    markAllSpecsPreInstrumented();

    addJDK15InstrumentedSpec();

    // Hack for honoring transient in Struts action classes
    spec = getOrCreateSpec("org.apache.struts.action.ActionForm");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("org.apache.struts.action.ActionMappings");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("org.apache.struts.action.ActionServletWrapper");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("org.apache.struts.action.DynaActionFormClass");
    spec.setHonorTransient(true);

    // Hack for Struts <bean:include> tag
    addCustomAdapter("org.apache.struts.taglib.bean.IncludeTag", IncludeTagAdapter.class);

    // Generic Session classes
    spec = getOrCreateSpec("com.terracotta.session.SessionData");
    spec.setHonorTransient(true);
    spec = getOrCreateSpec("com.terracotta.session.util.Timestamp");
    spec.setHonorTransient(true);

    spec = getOrCreateSpec("java.lang.Object");
    spec.setCallConstructorOnLoad(true);

    // Autolocking FastHashMap.
    addIncludePattern("org.apache.commons.collections.FastHashMap*", true);
    addWriteAutolock("* org.apache.commons.collections.FastHashMap*.*(..)");
    addReadAutolock(new String[] { "* org.apache.commons.collections.FastHashMap.clone(..)",
        "* org.apache.commons.collections.FastHashMap*.contains*(..)",
        "* org.apache.commons.collections.FastHashMap.equals(..)",
        "* org.apache.commons.collections.FastHashMap.get(..)",
        "* org.apache.commons.collections.FastHashMap*.hashCode(..)",
        "* org.apache.commons.collections.FastHashMap*.isEmpty(..)",
        "* org.apache.commons.collections.FastHashMap*.size(..)" });

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

    spec = getOrCreateSpec("gnu.trove.ToObjectArrayProcedure");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);

    spec = getOrCreateSpec("javax.servlet.GenericServlet");
    spec.setHonorTransient(true);
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    // BEGIN: weblogic stuff
    addAspectModule("weblogic.servlet.internal", "com.tc.weblogic.SessionAspectModule");
    addWeblogicCustomAdapters();
    // END: weblogic stuff

    // BEGIN: tomcat stuff
    addTomcatCustomAdapters();
    // END: tomcat stuff

    // Geronimo + WebsphereCE stuff
    addCustomAdapter("org.apache.geronimo.kernel.basic.ProxyMethodInterceptor", ProxyMethodInterceptorAdapter.class);
    addCustomAdapter("org.apache.geronimo.kernel.config.MultiParentClassLoader", MultiParentClassLoaderAdapter.class);
    addCustomAdapter("org.apache.geronimo.tomcat.HostGBean", HostGBeanAdapter.class);
    addCustomAdapter("org.apache.geronimo.tomcat.TomcatClassLoader", TomcatClassLoaderAdapter.class);

    // TODO for the Event Swing sample only
    ld = new LockDefinition("setTextArea", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* test.event.*.setTextArea(..)", ld);

    doAutoconfigForSpring();
    doAutoconfigForSpringWebFlow();

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

  /**
   * Configure defaults for Spring Runtime
   */
  private void doAutoconfigForSpring() {
    addIncludePattern("org.springframework.context.ApplicationEvent", false, false, false);
    addIncludePattern("com.tcspring.ApplicationContextEventProtocol", true, true, true);

    addIncludePattern("com.tcspring.ComplexBeanId", true, true, true);
    addIncludePattern("com.tcspring.GetBeanProtocolWithScope$ScopedBeanDestructionCallBack", true, true, true);
    addIncludePattern("com.tcspring.GetBeanProtocolWithScope$ChainedBindingListener", true, true, true);

    // Spring AOP introduction/mixin classes
    addIncludePattern("org.springframework.aop.support.IntroductionInfoSupport", true, true, true);
    addIncludePattern("org.springframework.aop.support.DelegatingIntroductionInterceptor", true, true, true);
    addIncludePattern("org.springframework.aop.support.DefaultIntroductionAdvisor", true, true, true);
    addIncludePattern("gnu.trove..*", false, false, true);
    addIncludePattern("java.lang.reflect.Proxy", false, false, false);
    addIncludePattern("com.tc.aspectwerkz.proxy..*", false, false, true);

    // TODO remove if we find a better way using ProxyApplicator etc.
    addIncludePattern("$Proxy..*", false, false, true);

    // backport concurrent classes
    addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractCollection", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.AbstractQueue", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue$Node", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.FutureTask", false, false, false);

    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ConcurrentLinkedQueue", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.PriorityBlockingQueue", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.ArrayBlockingQueue", false, false, false);
    addIncludePattern("edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList", false, false, false);

    LockDefinition ld = new LockDefinition("addApplicationListener", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* org.springframework.context.event.AbstractApplicationEventMulticaster.addApplicationListener(..)", ld);

    // used by WebFlow
    addIncludePattern("org.springframework.core.enums.*", false, false, false);
    addIncludePattern("org.springframework.binding..*", true, false, false);
    addIncludePattern("org.springframework.validation..*", true, false, false);
  }

  private void doAutoconfigForSpringWebFlow() {
    addAspectModule("org.springframework.webflow", "com.tc.object.config.SpringWebFlowAspectModule");
    addIncludePattern("com.tcspring.DSOConversationLock", false, false, false);

    addIncludePattern("org.springframework.webflow..*", true, false, false);

    addIncludePattern("org.springframework.webflow.conversation.impl.ConversationEntry", false, false, false);
    addIncludePattern("org.springframework.webflow.core.collection.LocalAttributeMap", false, false, false);
    addIncludePattern("org.springframework.webflow.conversation.impl.*", false, false, false);

    // getOrCreateSpec("org.springframework.webflow.engine.impl.FlowSessionImpl").setHonorTransient(false).addTransient("flow");
    // flow : Flow
    // flowId : String
    // state : State
    // stateId : String
    // .addTransient("parent") // : FlowSessionImpl
    // .addTransient("scope") // : LocalAttributeMap
    // .addTransient("status"); // : FlowSessionStatus

    // all "transient" for all subclasses except "State.id"
    // getOrCreateSpec("org.springframework.webflow.engine.State") //
    // .addTransient("logger").addTransient("flow") //
    // .addTransient("entryActionList") //
    // .addTransient("exceptionHandlerSet"); //
    // getOrCreateSpec("org.springframework.webflow.engine.EndState") //
    // .addTransient("viewSelector") //
    // .addTransient("outputMapper"); //
    // getOrCreateSpec("org.springframework.webflow.engine.TransitionableState") // abstract
    // .addTransient("transitions")
    // .addTransient("exitActionList");
    // getOrCreateSpec("org.springframework.webflow.engine.ActionState") //
    // .addTransient("actionList");
    // getOrCreateSpec("org.springframework.webflow.engine.SubflowState") //
    // .addTransient("subflow") //
    // .addTransient("attributeMapper"); //
    // getOrCreateSpec("org.springframework.webflow.engine.ViewState") //
    // .addTransient("viewSelector") //
    // .addTransient("renderActionList");
    // // getOrCreateSpec("org.springframework.webflow.engine.DecisionState"); no fields

    // TODO investigate if better granularity of above classes is required
    // org.springframework.webflow.execution.repository.support.DefaultFlowExecutionRepository
    // org.springframework.webflow.execution.repository.support.AbstractConversationFlowExecutionRepository
    // org.springframework.webflow.execution.repository.support.AbstractFlowExecutionRepository
    // org.springframework.webflow.execution.repository.support.DefaultFlowExecutionRepositoryFactory
    // org.springframework.webflow.execution.repository.support.DelegatingFlowExecutionRepositoryFactory
    // org.springframework.webflow.execution.repository.support.FlowExecutionRepositoryServices
    // org.springframework.webflow.execution.repository.support.SharedMapFlowExecutionRepositoryFactory
    // org.springframework.webflow.execution.repository.conversation.impl.LocalConversationService
    // org.springframework.webflow.util.RandomGuidUidGenerator
    // org.springframework.webflow.registry.FlowRegistryImpl
    // etc...
  }

  private void addReflectionPreInstrumentedSpec() {
    if (supportSharingThroughReflection) {
      getOrCreateSpec("java.lang.reflect.Field");
      addCustomAdapter("java.lang.reflect.Field", JavaLangReflectFieldAdapter.class);

      getOrCreateSpec("java.lang.reflect.Array");
      addCustomAdapter("java.lang.reflect.Array", JavaLangReflectArrayAdapter.class);
    }
  }

  private void addLogicalAdaptedLinkedBlockingQueueSpec() {
    TransparencyClassSpec spec = getOrCreateSpec("java.util.AbstractQueue");
    spec.setInstrumentationAction(TransparencyClassSpec.ADAPTABLE);

    spec = getOrCreateSpec("java.util.concurrent.LinkedBlockingQueue",
                           "com.tc.object.applicator.LinkedBlockingQueueApplicator");
  }

  private void addJavaUtilConcurrentHashMapSpec() {
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.ConcurrentHashMap",
                                                 "com.tc.object.applicator.ConcurrentHashMapApplicator");
    spec.setHonorTransient(true);
    spec.setPostCreateMethod("__tc_rehash");

    spec = getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$Segment");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
  }

  private void addJavaUtilConcurrentFutureTaskSpec() {
    if (Vm.isJDK16()) {
      getOrCreateSpec("java.util.concurrent.locks.AbstractOwnableSynchronizer");
    }
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.FutureTask$Sync");
    addWriteAutolock("* java.util.concurrent.FutureTask$Sync.*(..)");
    spec.setHonorTransient(true);
    spec.addDistributedMethodCall("managedInnerCancel", "()V");

    getOrCreateSpec("java.util.concurrent.FutureTask");

    getOrCreateSpec("java.util.concurrent.Executors$RunnableAdapter");
  }

  private void addJDK15InstrumentedSpec() {
    TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock$ConditionObject");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
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

  private void addJDK15PreInstrumentedSpec() {
    if (Vm.isJDK15()) {
      TransparencyClassSpec spec = getOrCreateSpec("sun.misc.Unsafe");
      addCustomAdapter("sun.misc.Unsafe", UnsafeAdapter.class);
      spec = getOrCreateSpec(DSOUnsafe.CLASS_DOTS);
      addCustomAdapter(DSOUnsafe.CLASS_DOTS, DSOUnsafeAdapter.class);

      spec = getOrCreateSpec("java.util.concurrent.CyclicBarrier");

      spec = getOrCreateSpec("java.util.concurrent.CyclicBarrier$Generation");
      spec.setHonorJDKSubVersionSpecific(true);

      spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicInteger");
      spec.setHonorVolatile(true);
      spec = getOrCreateSpec("java.util.concurrent.atomic.AtomicLong");
      spec.setHonorVolatile(true);

      spec = getOrCreateSpec("java.util.concurrent.TimeUnit");

      /*****************************************************************************************************************
       * This section of spec are specified in the BootJarTool also. They are placed again so that the honorTransient *
       * flag will be honored during runtime. *
       ****************************************************************************************************************/

      addJavaUtilConcurrentHashMapSpec();

      addLogicalAdaptedLinkedBlockingQueueSpec();

      addJavaUtilConcurrentFutureTaskSpec();

      spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock");
      spec.setHonorTransient(true);
      spec.setCallConstructorOnLoad(true);
      spec = getOrCreateSpec("java.util.concurrent.locks.AbstractQueuedSynchronizer");
      spec.setHonorTransient(true);
      spec.setCallConstructorOnLoad(true);
      spec = getOrCreateSpec("java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject");
      spec.setHonorTransient(true);
      spec.setCallConstructorOnLoad(true);

      /*****************************************************************************************************************
       * This section of spec are specified in the BootJarTool also. They are placed again so that the honorTransient *
       * flag will be honored during runtime. *
       ****************************************************************************************************************/
    }
  }

  private void addTomcatCustomAdapters() {
    addCustomAdapter("org.apache.jasper.runtime.JspWriterImpl", JspWriterImplAdapter.class);
    addCustomAdapter("org.apache.catalina.loader.WebappLoader", WebAppLoaderAdapter.class);
    addCustomAdapter("org.apache.catalina.startup.Catalina", CatalinaAdapter.class);
    addCustomAdapter("org.apache.catalina.core.ContainerBase", ContainerBaseAdapter.class);
    addCustomAdapter("org.apache.catalina.startup.Bootstrap", BootstrapAdapter.class);
    addCustomAdapter("org.apache.catalina.loader.WebappClassLoader", TomcatLoaderAdapter.class);
    addCustomAdapter("org.apache.catalina.loader.StandardClassLoader", TomcatLoaderAdapter.class);
  }

  private void addWeblogicCustomAdapters() {
    addCustomAdapter("weblogic.Server", ServerAdapter.class);
    addCustomAdapter("weblogic.utils.classloaders.GenericClassLoader", GenericClassLoaderAdapter.class);
    addCustomAdapter("weblogic.ejb20.ejbc.EjbCodeGenerator", EJBCodeGeneratorAdapter.class);
    addCustomAdapter("weblogic.servlet.internal.WebAppServletContext", WebAppServletContextAdapter.class);
    addCustomAdapter("weblogic.servlet.internal.ServletResponseImpl", ServletResponseImplAdapter.class);
    addCustomAdapter("weblogic.servlet.internal.TerracottaServletResponseImpl",
                     TerracottaServletResponseImplAdapter.class);
  }

  public void addCustomAdapter(String name, Class adapter) {
    try {
      Constructor cstr = adapter.getConstructor(ADAPTER_CSTR_SIGNATURE);
      Object prev = this.customAdapters.put(name, cstr);
      Assert.assertNull(prev);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void markAllSpecsPreInstrumented() {
    for (Iterator i = classSpecs.values().iterator(); i.hasNext();) {
      TransparencyClassSpec s = (TransparencyClassSpec) i.next();
      s.markPreInstrumented();
    }
  }

  public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions() {
    return this.configSetupManager.dsoL1Config().instrumentationLoggingOptions();
  }

  public Iterator getAllUserDefinedBootSpecs() {
    return this.userDefinedBootSpecs.values().iterator();
  }

  public void setFaultCount(int count) {
    this.faultCount = count;
  }

  public boolean isLockMethod(int access, String className, String methodName, String description, String[] exceptions) {
    helperLogger.logIsLockMethodBegin(access, className, methodName, description);

    LockDefinition lockDefinitions[] = lockDefinitionsFor(access, className, methodName, description, exceptions);

    for (int j = 0; j < lockDefinitions.length; j++) {
      if (lockDefinitions[j].isAutolock()) {
        if (isNotStaticAndIsSynchronized(access)) {
          helperLogger.logIsLockMethodAutolock();
          return true;
        }
      } else {
        return true;
      }
    }

    helperLogger.logIsLockMethodNoMatch(className, methodName);
    return false;
  }

  public boolean matches(final Lock lock, final MemberInfo methodInfo) {
    return matches(lock.getMethodJoinPointExpression(), methodInfo);
  }

  public boolean matches(final String expression, final MemberInfo methodInfo) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    if (logger.isDebugEnabled()) logger
        .debug("==>Testing for match: " + executionExpression + " against " + methodInfo);
    ExpressionContext ctxt = expressionHelper.createExecutionExpressionContext(methodInfo);
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    return visitor.match(ctxt);
  }

  private MethodInfo getMethodInfo(int modifiers, String className, String methodName, String description,
                                   String[] exceptions) {
    // TODO: This probably needs caching.
    return new AsmMethodInfo(classInfoFactory, modifiers, className, methodName, description, exceptions);
  }

  private ConstructorInfo getConstructorInfo(int modifiers, String className, String methodName, String description,
                                             String[] exceptions) {
    return new AsmConstructorInfo(classInfoFactory, modifiers, className, methodName, description, exceptions);
  }

  private MemberInfo getMemberInfo(int modifiers, String className, String methodName, String description,
                                   String[] exceptions) {
    if (false && "<init>".equals(methodName)) {
      // XXX: ConstructorInfo seems to really break things. Plus, locks in
      // constructors don't work yet.
      // When locks in constructors work, we'll have to stort this problem out.
      return getConstructorInfo(modifiers, className, methodName, description, exceptions);
    } else {
      return getMethodInfo(modifiers, className, methodName, description, exceptions);
    }
  }

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
    addRoot(classSpec.getFullyQualifiedClassName(), classSpec.getShortFieldName(), rootName, false);
  }

  private void addRoot(String className, String fieldName, Root newRoot, boolean addSpecForClass) {
    if (addSpecForClass) {
      this.getOrCreateSpec(className);
    }

    synchronized (roots) {
      Map rootsForClass = (Map) roots.get(className);
      if (rootsForClass == null) {
        rootsForClass = new ConcurrentHashMap();
        roots.put(className, rootsForClass);
      }

      rootsForClass.put(fieldName, newRoot);
    }
  }

  public void addRoot(String className, String fieldName, String rootName, boolean addSpecForClass) {
    addRoot(className, fieldName, new Root(className, fieldName, rootName), addSpecForClass);
  }

  public void addRoot(String className, String fieldName, String rootName, boolean dsoFinal, boolean addSpecForClass) {
    addRoot(className, fieldName, new Root(className, fieldName, rootName, dsoFinal), addSpecForClass);
  }

  public String rootNameFor(String className, String fieldName) {
    Map rootsForClass = (Map) roots.get(className);
    if (rootsForClass == null) { throw Assert.failure("No roots at all for class " + className); }

    Root root = (Root) rootsForClass.get(fieldName);
    if (root == null) { throw Assert.failure("No such root for fieldName " + fieldName + " in class " + className); }

    return root.getRootName();
  }

  public boolean isRoot(String className, String fieldName) {
    Map rootsForClass = (Map) roots.get(className);
    if (rootsForClass == null) { return false; }
    return rootsForClass.containsKey(fieldName);
  }

  public boolean isRootDSOFinal(String className, String fieldName, boolean isPrimitive) {
    Map rootsForClass = (Map) roots.get(className);
    if (rootsForClass == null) { throw Assert.failure("No roots at all for class " + className); }
    Root root = (Root) rootsForClass.get(fieldName);
    if (root == null) { throw Assert.failure("No such root for fieldName " + fieldName + " in class " + className); }

    return root.isDsoFinal(isPrimitive);
  }

  private boolean classContainsAnyRoots(String className) {
    return roots.containsKey(className);
  }

  private void rewriteHashtableAutLockSpecIfNecessary() {
    // addReadAutolock(new String[] { "synchronized * java.util.Hashtable.get(..)",
    // "synchronized * java.util.Hashtable.hashCode(..)", "synchronized * java.util.Hashtable.contains*(..)",
    // "synchronized * java.util.Hashtable.elements(..)", "synchronized * java.util.Hashtable.equals(..)",
    // "synchronized * java.util.Hashtable.isEmpty(..)", "synchronized * java.util.Hashtable.keys(..)",
    // "synchronized * java.util.Hashtable.size(..)", "synchronized * java.util.Hashtable.toString(..)" });

    Set readOnlyLockMethodSpec = new HashSet();

    int access = Opcodes.ACC_PUBLIC;
    String className = "java.util.Hashtable";
    String methodName = "get";
    String description = "(Ljava/lang/Object;)Ljava/lang/Object;";
    MemberInfo methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "hashCode";
    description = "()I";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "contains";
    description = "(Ljava/lang/Object;)Z";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "containsKey";
    description = "(Ljava/lang/Object;)Z";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "elements";
    description = "()Ljava/util/Enumeration;";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "equals";
    description = "(Ljava/lang/Object;)Z";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "isEmpty";
    description = "()Z";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "keys";
    description = "()Ljava/util/Enumeration;";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "size";
    description = "()I";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    methodName = "toString";
    description = "()Ljava/lang/String;";
    methodInfo = getMemberInfo(access, className, methodName, description, null);
    readOnlyLockMethodSpec.add(methodInfo);

    Set readLockSpec = new HashSet();
    for (Iterator itr = readOnlyLockMethodSpec.iterator(); itr.hasNext();) {
      methodInfo = (MemberInfo) itr.next();

      for (int i = 0; i < locks.length; i++) {
        Lock lock = locks[i];
        if (matches(lock, methodInfo)) {
          LockDefinition ld = lock.getLockDefinition();
          if (ld.isAutolock() && ld.getLockLevel() != ConfigLockLevel.READ) {
            readLockSpec.add("* " + className + "." + methodInfo.getName() + "(..)");
          }
          break;
        }
      }
    }
    String[] readLockSpecs = new String[readLockSpec.size()];
    readLockSpec.toArray(readLockSpecs);
    addReadAutolock(readLockSpecs);
  }

  public synchronized LockDefinition[] lockDefinitionsFor(int access, String className, String methodName,
                                                          String description, String[] exceptions) {
    MemberInfo methodInfo = getMemberInfo(access, className, methodName, description, exceptions);
    boolean isAutoLocksExcluded = matchesAutoLockExcludes(methodInfo);
    List lockDefs = new ArrayList();
    // for (int i = 0; i < this.locks.length; i++) {
    for (int i = locks.length - 1; i >= 0; i--) {
      if (matches(this.locks[i], methodInfo)) {
        LockDefinition definition = this.locks[i].getLockDefinition();
        if (!(definition.isAutolock() && isAutoLocksExcluded)) {
          lockDefs.add(definition);
          if (definition.isAutolock()) {
            isAutoLocksExcluded = true;
          }
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

  private synchronized boolean isInAdaptableCache(String name) {
    return adaptableCache.containsKey(name);
  }

  private synchronized boolean isCachedAdaptable(String name) {
    return ((Boolean) adaptableCache.get(name)).booleanValue();
  }

  private synchronized boolean cacheIsAdaptable(String name, boolean adaptable) {
    adaptableCache.put(name, new Boolean(adaptable));
    return adaptable;
  }

  private synchronized void clearAdaptableCache() {
    this.adaptableCache.clear();
  }

  public void addWriteAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE);
  }

  public void addReadAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.READ);
  }

  private void addReadAutolock(String[] methodPatterns) {
    for (int i = 0; i < methodPatterns.length; i++) {
      addAutolock(methodPatterns[i], ConfigLockLevel.READ);
    }
  }

  public synchronized void addAutolock(String methodPattern, ConfigLockLevel type) {
    LockDefinition lockDefinition = new LockDefinition(LockDefinition.TC_AUTOLOCK_NAME, type);
    lockDefinition.commit();
    addLock(methodPattern, lockDefinition);
  }

  public synchronized void addLock(String methodPattern, LockDefinition lockDefinition) {
    Lock[] result = new Lock[locks.length + 1];
    System.arraycopy(locks, 0, result, 0, locks.length);
    result[locks.length] = new Lock(methodPattern, lockDefinition);
    locks = result;
  }

  public boolean shouldBeAdapted(String fullClassName) {
    if (isInAdaptableCache(fullClassName)) { return isCachedAdaptable(fullClassName); }

    // @see isTCPatternMatchingHack() note elsewhere
    if (isTCPatternMatchingHack(fullClassName) || permanentExcludesMatcher.match(fullClassName)) {
      // permanent Excludes
      return cacheIsAdaptable(fullClassName, false);
    }

    String outerClassname = outerClassnameWithoutInner(fullClassName);
    if (isLogical(outerClassname)) {
      // We make inner classes of logical classes not instrumented while logical
      // bases are instrumented...UNLESS there is a explicit spec for said inner class
      boolean adaptable = (getSpec(fullClassName)) != null || outerClassname.equals(fullClassName);
      return cacheIsAdaptable(fullClassName, adaptable);
    }

    // If a root is defined then we automagically instrument
    if (classContainsAnyRoots(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }
    // custom adapters trump config.
    if (hasCustomAdapter(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }
    // existing class specs trump config
    if (hasSpec(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }

    InstrumentationDescriptor desc = getInstrumentationDescriptorFor(fullClassName);
    return cacheIsAdaptable(fullClassName, desc.isInclude());
  }

  private boolean isTCPatternMatchingHack(String fullClassName) {
    return fullClassName.startsWith("com.tc.") || fullClassName.startsWith("com.terracottatech.");
  }

  public boolean isNeverAdaptable(String fullName) {
    return (isTCPatternMatchingHack(fullName) || permanentExcludesMatcher.match(fullName) || nonportablesMatcher
        .match(fullName));
  }

  private InstrumentationDescriptor getInstrumentationDescriptorFor(String fullName) {
    InstrumentationDescriptor rv;

    synchronized (this.instrumentationDescriptors) {
      for (Iterator i = this.instrumentationDescriptors.iterator(); i.hasNext();) {
        rv = (InstrumentationDescriptor) i.next();
        if (rv.matches(fullName)) { return rv; }
      }
    }
    return DEAFULT_INSTRUMENTATION_DESCRIPTOR;
  }

  private boolean hasCustomAdapter(String fullName) {
    return this.customAdapters.containsKey(fullName);
  }

  private String outerClassnameWithoutInner(String fullName) {
    int indexOfInner = fullName.indexOf('$');
    return indexOfInner < 0 ? fullName : fullName.substring(0, indexOfInner);
  }

  public boolean isTransient(int modifiers, String classname, String field) {
    if (ByteCodeUtil.isParent(field)) return true;
    if (Modifier.isTransient(modifiers) && isHonorJavaTransient(classname)) return true;
    Type type = (Type) types.get(classname);
    if (type != null) { return (type.containsTransient(field)); }
    return false;
  }

  public boolean isVolatile(int modifiers, String classname, String field) {
    return (Modifier.isVolatile(modifiers) && isHonorJavaVolatile(classname));
  }

  public boolean isHonorJavaTransient(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if ((spec != null) && (spec.isHonorTransientSet())) { return spec.isHonorJavaTransient(); }
    return getInstrumentationDescriptorFor(className).isHonorTransient();
  }

  public boolean isHonorJavaVolatile(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if ((spec != null) && (spec.isHonorVolatileSet())) { return spec.isHonorVolatile(); }
    return getInstrumentationDescriptorFor(className).isHonorVolatile();
  }

  public boolean isCallConstructorOnLoad(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if ((spec != null) && spec.isCallConstructorSet()) { return spec.isCallConstructorOnLoad(); }
    return getInstrumentationDescriptorFor(className).isCallConstructorOnLoad();
  }

  public String getPostCreateMethodIfDefined(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) {
      return spec.getPostCreateMethod();
    } else {
      return null;
    }
  }

  public String getOnLoadScriptIfDefined(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if ((spec != null) && spec.isExecuteScriptOnLoadSet()) { return spec.getOnLoadExecuteScript(); }
    return getInstrumentationDescriptorFor(className).getOnLoadScriptIfDefined();
  }

  public String getOnLoadMethodIfDefined(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if ((spec != null) && spec.isCallMethodOnLoadSet()) { return spec.getOnLoadMethod(); }
    return getInstrumentationDescriptorFor(className).getOnLoadMethodIfDefined();
  }

  public boolean isUseNonDefaultConstructor(String className) {
    if (literalValues.isLiteral(className)) { return true; }
    TransparencyClassSpec spec = getSpec(className);
    if (spec == null) { return false; }
    return spec.isUseNonDefaultConstructor();
  }

  public boolean isDSOSessions(String name) {
    for (int i = 0; i < applications.length; i++) {
      if (name.matches(applications[i].replaceAll("\\*", "\\.\\*"))) return true;
    }
    return false;
  }

  public TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, String className,
                                                           InstrumentationLogger lgr, ClassLoader caller,
                                                           final boolean forcePortable) {
    ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
    TransparencyClassSpec spec = getOrCreateSpec(className);

    if (forcePortable) {
      if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
        spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
      } else {
        logger.info("Not making " + className + " forcefully portable");
      }
    }

    TransparencyClassAdapter dsoAdapter = new TransparencyClassAdapter(getOrCreateSpec(className), writer, mgrHelper,
                                                                       lgr, caller, portability);
    return dsoAdapter;
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, String className, InstrumentationLogger lgr,
                                            ClassLoader caller) {
    return this.createClassAdapterFor(writer, className, lgr, caller, false);
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, String className, InstrumentationLogger lgr,
                                            ClassLoader caller, final boolean forcePortable) {
    Constructor customCstr = (Constructor) this.customAdapters.get(className);
    if (customCstr != null) {
      return createCustomAdapter(customCstr, writer, caller);
    } else {
      ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
      TransparencyClassSpec spec = getOrCreateSpec(className);

      if (forcePortable) {
        if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
          spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
        } else {
          logger.info("Not making " + className + " forcefully portable");
        }
      }

      ClassAdapter dsoAdapter = new TransparencyClassAdapter(getOrCreateSpec(className), writer, mgrHelper, lgr,
                                                             caller, portability);
      return new SerialVersionUIDAdder(dsoAdapter);
    }
  }

  private ClassAdapter createCustomAdapter(Constructor cstr, ClassWriter writer, ClassLoader caller) {
    try {
      return (ClassAdapter) cstr.newInstance(new Object[] { writer, caller });
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private TransparencyClassSpec basicGetOrCreateSpec(String className, String applicator) {
    synchronized (classSpecs) {

      TransparencyClassSpec spec = getSpec(className);
      if (spec == null) {
        if (applicator != null) {
          spec = new TransparencyClassSpec(className, this, applicator);
        } else {
          spec = new TransparencyClassSpec(className, this);
        }
        addSpec(spec);
      }
      return spec;
    }
  }

  public TransparencyClassSpec getOrCreateSpec(String className) {
    return basicGetOrCreateSpec(className, null);
  }

  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator) {
    if (applicator == null) throw new AssertionError();
    return basicGetOrCreateSpec(className, applicator);
  }

  private void addSpec(TransparencyClassSpec spec) {
    synchronized (classSpecs) {
      Assert.eval(!classSpecs.containsKey(spec.getClassName()));
      classSpecs.put(spec.getClassName(), spec);
    }
  }

  public boolean isLogical(String className) {
    TransparencyClassSpec spec = getSpec(className);
    return spec != null && spec.isLogical();
  }

  public String getChangeApplicatorClassNameFor(String className) {
    TransparencyClassSpec spec = getSpec(className);
    if (spec == null) return null;
    return spec.getChangeApplicatorClassName();
  }

  public boolean hasSpec(String className) {
    return getSpec(className) != null;
  }

  /**
   * This is used in BootJarTool. In BootJarTool, it changes the package of our implementation of ReentrantLock and
   * FutureTask to the java.util.concurrent package. In order to change the different adapter together, we need to
   * create a spec with our package and remove the spec after the instrumentation is done.
   */
  public void removeSpec(String className) {
    className = className.replace('/', '.');
    classSpecs.remove(className);
  }

  public TransparencyClassSpec getSpec(String className) {
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

  public Iterator getAllSpecs() {
    return classSpecs.values().iterator();
  }

  public void addDistributedMethodCall(String methodExpression) {
    distributedMethods.add(methodExpression);
  }

  public boolean isDistributedMethodCall(int modifiers, String className, String methodName, String description,
                                         String[] exceptions) {
    if (Modifier.isStatic(modifiers) || "<init>".equals(methodName) || "<clinit>".equals(methodName)) { return false; }
    MemberInfo methodInfo = getMemberInfo(modifiers, className, methodName, description, exceptions);
    for (Iterator i = distributedMethods.iterator(); i.hasNext();) {
      if (matches((String) i.next(), methodInfo)) { return true; }
    }
    return false;
  }

  public void addTransient(String className, String fieldName) {
    TransparencyClassSpec spec = this.getOrCreateSpec(className);
    spec.addTransient(fieldName);
  }

  public String toString() {
    return "<StandardDSOClientConfigHelper: " + configSetupManager + ">";
  }

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder) {
    throw new UnsupportedOperationException();
  }

  public void addAspectModule(String pattern, String moduleName) {
    List modules = (List) this.aspectModules.get(pattern);
    if (modules == null) {
      modules = new ArrayList();
      this.aspectModules.put(pattern, modules);
    }
    modules.add(moduleName);
  }

  public Map getAspectModules() {
    return this.aspectModules;
  }

  public void addDSOSpringConfig(DSOSpringConfigHelper config) {
    this.springConfigs.add(config);

    if (!this.aspectModules.containsKey("org.springframework")) {
      addAspectModule("org.springframework", "com.tc.object.config.SpringAspectModule");
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

}
