/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.commons.SerialVersionUIDAdder;
import com.tc.aspectwerkz.expression.ExpressionContext;
import com.tc.aspectwerkz.expression.ExpressionVisitor;
import com.tc.aspectwerkz.reflect.ClassInfo;
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
import com.tc.jboss.transform.MainAdapter;
import com.tc.jboss.transform.UCLAdapter;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.object.LiteralValues;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.bytecode.AbstractListMethodCreator;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterBase;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.DSOUnsafeAdapter;
import com.tc.object.bytecode.JavaLangReflectArrayAdapter;
import com.tc.object.bytecode.JavaLangReflectFieldAdapter;
import com.tc.object.bytecode.JavaUtilWeakHashMapAdapter;
import com.tc.object.bytecode.ManagerHelper;
import com.tc.object.bytecode.ManagerHelperFactory;
import com.tc.object.bytecode.THashMapAdapter;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.bytecode.TreeMapAdapter;
import com.tc.object.bytecode.UnsafeAdapter;
import com.tc.object.bytecode.aspectwerkz.ExpressionHelper;
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
import com.tc.tomcat.transform.BootstrapAdapter;
import com.tc.tomcat.transform.CatalinaAdapter;
import com.tc.tomcat.transform.ContainerBaseAdapter;
import com.tc.tomcat.transform.JspWriterImplAdapter;
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
import com.tc.wicket.WicketWebApplicationAdapter;
import com.tcclient.util.DSOUnsafe;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;
import com.terracottatech.config.SpringApplication;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StandardDSOClientConfigHelper implements DSOClientConfigHelper {

  private static final String                    CGLIB_PATTERN                      = "$$EnhancerByCGLIB$$";

  private static final LiteralValues             literalValues                      = new LiteralValues();

  private static final TCLogger                  logger                             = CustomerLogging
                                                                                        .getDSOGenericLogger();
  private static final InstrumentationDescriptor DEAFULT_INSTRUMENTATION_DESCRIPTOR = new NullInstrumentationDescriptor();

  private final ManagerHelperFactory             mgrHelperFactory                   = new ManagerHelperFactory();
  private final DSOClientConfigHelperLogger      helperLogger;

  private final L1TVSConfigurationSetupManager   configSetupManager;

  private Lock[]                                 locks                              = new Lock[0];
  private final Map                              roots                              = new ConcurrentHashMap();

  private final Set                              applicationNames                   = Collections
                                                                                        .synchronizedSet(new HashSet());
  private final List                             synchronousWriteApplications       = new ArrayList();
  private final CompoundExpressionMatcher        permanentExcludesMatcher;
  private final CompoundExpressionMatcher        nonportablesMatcher;
  private final List                             autoLockExcludes                   = new ArrayList();
  private final List                             distributedMethods                 = new LinkedList();                    // <DistributedMethodSpec>
  private final Map                              userDefinedBootSpecs               = new HashMap();

  // private final ClassInfoFactory classInfoFactory;
  private final ExpressionHelper                 expressionHelper;

  private final Map                              adaptableCache                     = new HashMap();

  /**
   * A list of InstrumentationDescriptor representing include/exclude patterns
   */
  private final LinkedList                       instrumentationDescriptors         = new LinkedList();

  /**
   * A map of class names to TransparencyClassSpec for individual classes
   */
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

  private ModuleSpec[]                           moduleSpecs                        = null;

  private final ModulesContext                   modulesContext                     = new ModulesContext();

  private volatile boolean                       allowCGLIBInstrumentation          = false;

  public StandardDSOClientConfigHelper(L1TVSConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
  }

  public StandardDSOClientConfigHelper(boolean initializedModulesOnlyOnce,
                                       L1TVSConfigurationSetupManager configSetupManager)
      throws ConfigurationSetupException {
    this(configSetupManager, true);
    if (initializedModulesOnlyOnce) {
      modulesContext.initializedModulesOnlyOnce();
    }
  }

  public StandardDSOClientConfigHelper(L1TVSConfigurationSetupManager configSetupManager, boolean interrogateBootJar)
      throws ConfigurationSetupException {
    this.portability = new PortabilityImpl(this);
    this.configSetupManager = configSetupManager;
    helperLogger = new DSOClientConfigHelperLogger(logger);
    // this.classInfoFactory = new ClassInfoFactory();
    this.expressionHelper = new ExpressionHelper();
    modulesContext.setModules(configSetupManager.commonL1Config().modules() != null ? configSetupManager
        .commonL1Config().modules() : Modules.Factory.newInstance());

    permanentExcludesMatcher = new CompoundExpressionMatcher();
    // TODO:: come back and add all possible non-portable/non-adaptable classes here. This is by no means exhaustive !

    // XXX:: There is a bug in aspectwerkz com.tc..* matches both com.tc and com.tctest classes. As a work around
    // this is commented and isTCPatternMatchingHack() method is added instead. When that bug is fixed, uncomment
    // this and remove isTCPatternMatchingHack();
    // addPermanentExcludePattern("com.tc..*");
    // addPermanentExcludePattern("com.terracottatech..*");
    addPermanentExcludePattern("java.awt.Component");
    addPermanentExcludePattern("java.lang.Thread");
    addPermanentExcludePattern("java.lang.ThreadLocal");
    addPermanentExcludePattern("java.lang.ThreadGroup");
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
    addUnsupportedJavaUtilConcurrentTypes();

    addAutoLockExcludePattern("* java.lang.Throwable.*(..)");

    nonportablesMatcher = new CompoundExpressionMatcher();
    addNonportablePattern("javax.servlet.GenericServlet");

    NewDSOApplicationConfig appConfig = configSetupManager
        .dsoApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);
    NewSpringApplicationConfig springConfig = configSetupManager
        .springApplicationConfigFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME);

    supportSharingThroughReflection = appConfig.supportSharingThroughReflection().getBoolean();
    try {
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

  public void allowCGLIBInstrumentation() {
    this.allowCGLIBInstrumentation = true;
  }

  private void addUnsupportedJavaUtilConcurrentTypes() {
    addPermanentExcludePattern("java.util.concurrent.AbstractExecutorService");
    addPermanentExcludePattern("java.util.concurrent.ArrayBlockingQueue*");
    addPermanentExcludePattern("java.util.concurrent.ConcurrentLinkedQueue*");
    addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListMap*");
    addPermanentExcludePattern("java.util.concurrent.ConcurrentSkipListSet*");
    addPermanentExcludePattern("java.util.concurrent.CopyOnWriteArrayList*");
    addPermanentExcludePattern("java.util.concurrent.CopyOnWriteArraySet*");
    addPermanentExcludePattern("java.util.concurrent.CountDownLatch*");
    addPermanentExcludePattern("java.util.concurrent.DelayQueue*");
    addPermanentExcludePattern("java.util.concurrent.Exchanger*");
    addPermanentExcludePattern("java.util.concurrent.ExecutorCompletionService*");
    addPermanentExcludePattern("java.util.concurrent.LinkedBlockingDeque*");
    addPermanentExcludePattern("java.util.concurrent.PriorityBlockingQueue*");
    addPermanentExcludePattern("java.util.concurrent.ScheduledThreadPoolExecutor*");
    addPermanentExcludePattern("java.util.concurrent.Semaphore*");
    addPermanentExcludePattern("java.util.concurrent.SynchronousQueue*");
    addPermanentExcludePattern("java.util.concurrent.ThreadPoolExecutor*");

    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicBoolean*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerArray*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicIntegerFieldUpdater*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongArray*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicLongFieldUpdater*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicMarkableReference*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReference*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceArray*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicReferenceFieldUpdater*");
    addPermanentExcludePattern("java.util.concurrent.atomic.AtomicStampedReference*");

    addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedLongSynchronizer*");
    addPermanentExcludePattern("java.util.concurrent.locks.AbstractQueuedSynchronizer*");
    addPermanentExcludePattern("java.util.concurrent.locks.LockSupport*");
    addPermanentExcludePattern("java.util.concurrent.locks.ReentrantReadWriteLock*");
  }

  public Portability getPortability() {
    return this.portability;
  }

  private void addAutoLockExcludePattern(String expression) {
    String executionExpression = ExpressionHelper.expressionPattern2ExecutionExpression(expression);
    ExpressionVisitor visitor = expressionHelper.createExpressionVisitor(executionExpression);
    autoLockExcludes.add(visitor);
  }

  private void addPermanentExcludePattern(String pattern) {
    permanentExcludesMatcher.add(new ClassExpressionMatcherImpl(expressionHelper, pattern));
  }

  private void addNonportablePattern(String pattern) {
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
    synchronized (this.instrumentationDescriptors) {
      this.instrumentationDescriptors.addFirst(newInstrumentationDescriptor(classDesc));
    }
  }

  public boolean hasIncludeExcludePatterns() {
    synchronized (this.instrumentationDescriptors) {
      return !this.instrumentationDescriptors.isEmpty();
    }
  }

  public boolean hasIncludeExcludePattern(ClassInfo classInfo) {
    return getInstrumentationDescriptorFor(classInfo) != DEAFULT_INSTRUMENTATION_DESCRIPTOR;
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
    addIncludePattern("javax.swing.event.TableModelEvent", true);
    TransparencyClassSpec spec = getOrCreateSpec("javax.swing.event.TableModelEvent");

    addIncludePattern("javax.swing.table.AbstractTableModel", true);
    spec = getOrCreateSpec("javax.swing.table.AbstractTableModel");
    spec.addDistributedMethodCall("fireTableChanged", "(Ljavax/swing/event/TableModelEvent;)V", false);
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

    addIncludePattern("javax.swing.tree.TreePath", false);
    spec = getOrCreateSpec("javax.swing.tree.TreePath");

    addIncludePattern("javax.swing.tree.DefaultMutableTreeNode", false);
    spec = getOrCreateSpec("javax.swing.tree.DefaultMutableTreeNode");

    spec = getOrCreateSpec("javax.swing.tree.DefaultTreeModel");
    ld = new LockDefinition("tctreeLock", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* javax.swing.tree.DefaultTreeModel.get*(..)", ld);
    addLock("* javax.swing.tree.DefaultTreeModel.set*(..)", ld);
    addLock("* javax.swing.tree.DefaultTreeModel.insert*(..)", ld);

    spec.addTransient("listenerList");
    spec.addDistributedMethodCall("fireTreeNodesChanged",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V", false);
    spec.addDistributedMethodCall("fireTreeNodesInserted",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V", false);
    spec.addDistributedMethodCall("fireTreeNodesRemoved",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V", false);
    spec.addDistributedMethodCall("fireTreeStructureChanged",
                                  "(Ljava/lang/Object;[Ljava/lang/Object;[I[Ljava/lang/Object;)V", false);
    spec
        .addDistributedMethodCall("fireTreeStructureChanged", "(Ljava/lang/Object;Ljavax/swing/tree/TreePath;)V", false);

    spec = getOrCreateSpec("javax.swing.AbstractListModel");
    spec.addTransient("listenerList");
    spec.addDistributedMethodCall("fireContentsChanged", "(Ljava/lang/Object;II)V", false);
    spec.addDistributedMethodCall("fireIntervalAdded", "(Ljava/lang/Object;II)V", false);
    spec.addDistributedMethodCall("fireIntervalRemoved", "(Ljava/lang/Object;II)V", false);

    spec = getOrCreateSpec("java.util.Arrays");
    spec.addDoNotInstrument("copyOfRange");
    spec.addDoNotInstrument("copyOf");

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
    addCustomAdapter("java.util.WeakHashMap", new JavaUtilWeakHashMapAdapter());

    spec = getOrCreateSpec("java.lang.reflect.AccessibleObject");
    spec.addTransient("securityCheckCache");

    addReflectionPreInstrumentedSpec();

    addJDK15PreInstrumentedSpec();

    /* ******* ALL ABOVE SPECS ARE PRE-INSTRUMENTED ******* */
    markAllSpecsPreInstrumented();

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
    addCustomAdapter("org.apache.geronimo.kernel.basic.ProxyMethodInterceptor", new ProxyMethodInterceptorAdapter());
    addCustomAdapter("org.apache.geronimo.kernel.config.MultiParentClassLoader", new MultiParentClassLoaderAdapter());
    addCustomAdapter("org.apache.geronimo.tomcat.HostGBean", new HostGBeanAdapter());
    addCustomAdapter("org.apache.geronimo.tomcat.TomcatClassLoader", new TomcatClassLoaderAdapter());

    // JBoss adapters
    addCustomAdapter("org.jboss.mx.loading.UnifiedClassLoader", new UCLAdapter());
    addCustomAdapter("org.jboss.Main", new MainAdapter());

    // TODO for the Event Swing sample only
    ld = new LockDefinition("setTextArea", ConfigLockLevel.WRITE);
    ld.commit();
    addLock("* test.event.*.setTextArea(..)", ld);

    // TODO move into its own plugin/module
    addCustomAdapter("wicket.protocol.http.WebApplication", new WicketWebApplicationAdapter());

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
    // addIncludePattern("com.tcspring.BeanContainer", true, true, true);
    getOrCreateSpec("com.tcspring.BeanContainer").addTransient("isInitialized"); // .setHonorTransient(true);

    // scoped beans
    // addTransient("org.springframework.web.context.request.ServletRequestAttributes$DestructionCallbackBindingListener",
    // "aw$MIXIN_0");
    addIncludePattern("com.tcspring.SessionProtocol$DestructionCallbackBindingListener", true, true, true);
    addIncludePattern("com.tcspring.ScopedBeanDestructionCallBack", true, true, true);

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
      addCustomAdapter("java.lang.reflect.Field", new JavaLangReflectFieldAdapter());

      getOrCreateSpec("java.lang.reflect.Array");
      addCustomAdapter("java.lang.reflect.Array", new JavaLangReflectArrayAdapter());
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
    spec.addDistributedMethodCall("managedInnerCancel", "()V", false);

    getOrCreateSpec("java.util.concurrent.FutureTask");

    getOrCreateSpec("java.util.concurrent.Executors$RunnableAdapter");
  }

  private void addJDK15InstrumentedSpec() {
    if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
      TransparencyClassSpec spec = getOrCreateSpec("java.util.concurrent.locks.ReentrantLock$ConditionObject");
      spec.setCallConstructorOnLoad(true);
      spec.setHonorTransient(true);
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

  private void addJDK15PreInstrumentedSpec() {
    if (Vm.getMegaVersion() >= 1 && Vm.getMajorVersion() > 4) {
      TransparencyClassSpec spec = getOrCreateSpec("sun.misc.Unsafe");
      addCustomAdapter("sun.misc.Unsafe", new UnsafeAdapter());
      spec = getOrCreateSpec(DSOUnsafe.CLASS_DOTS);
      addCustomAdapter(DSOUnsafe.CLASS_DOTS, new DSOUnsafeAdapter());

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

      /*****************************************************************************************************************
       * This section of spec are specified in the BootJarTool also. They are placed again so that the honorTransient *
       * flag will be honored during runtime. *
       ****************************************************************************************************************/
    }
  }

  private void addTomcatCustomAdapters() {
    addCustomAdapter("org.apache.jasper.runtime.JspWriterImpl", new JspWriterImplAdapter());
    addCustomAdapter("org.apache.catalina.loader.WebappLoader", new WebAppLoaderAdapter());
    addCustomAdapter("org.apache.catalina.startup.Catalina", new CatalinaAdapter());
    addCustomAdapter("org.apache.catalina.startup.Bootstrap", new BootstrapAdapter());
    addCustomAdapter("org.apache.catalina.core.ContainerBase", new ContainerBaseAdapter());
  }

  private void removeTomcatAdapters() {
    // XXX: hack for starting Glassfish w/o session support
    if (applicationNames.isEmpty()) {
      removeCustomAdapter("org.apache.catalina.core.ContainerBase");
    }
  }

  private void addWeblogicCustomAdapters() {
    addCustomAdapter("weblogic.Server", new ServerAdapter());
    addCustomAdapter("weblogic.utils.classloaders.GenericClassLoader", new GenericClassLoaderAdapter());
    addCustomAdapter("weblogic.ejb20.ejbc.EjbCodeGenerator", new EJBCodeGeneratorAdapter());
    addCustomAdapter("weblogic.servlet.internal.WebAppServletContext", new WebAppServletContextAdapter());
    addCustomAdapter("weblogic.servlet.internal.ServletResponseImpl", new ServletResponseImplAdapter());
    addCustomAdapter("weblogic.servlet.internal.TerracottaServletResponseImpl",
                     new TerracottaServletResponseImplAdapter());
  }

  public boolean removeCustomAdapter(String name) {
    synchronized (customAdapters) {
      Object prev = this.customAdapters.remove(name);
      return prev != null;
    }
  }

  public boolean addCustomAdapter(String name, ClassAdapterFactory factory) {
    synchronized (customAdapters) {
      if (customAdapters.containsKey(name)) { return false; }
      Object prev = this.customAdapters.put(name, factory);
      Assert.assertNull(prev);
      return true;
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
                      "contains(Ljava/lang/Object;)Z|" + //
                      "containsKey(Ljava/lang/Object;)Z|" + //
                      "elements()Ljava/util/Enumeration;|" + //
                      "equals(Ljava/lang/Object;)Z|" + //
                      "isEmpty()Z|" + //
                      "keys()Ljava/util/Enumeration;|" + //
                      "size()I|" + //
                      "toString()Ljava/lang/String;";

    MemberInfo[] methods = classInfo.getMethods();
    for (int j = 0; j < methods.length; j++) {
      MemberInfo methodInfo = methods[j];
      if (patterns.indexOf(methodInfo.getName() + methodInfo.getSignature()) > -1) {
        for (int i = 0; i < locks.length; i++) {
          Lock lock = locks[i];
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

  public synchronized LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo) {
    boolean isAutoLocksExcluded = matchesAutoLockExcludes(memberInfo);
    List lockDefs = new ArrayList();
    // for (int i = 0; i < this.locks.length; i++) {
    for (int i = locks.length - 1; i >= 0; i--) {
      if (matches(this.locks[i], memberInfo)) {
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

  private synchronized Boolean readAdaptableCache(String name) {
    return (Boolean) adaptableCache.get(name);
  }

  private synchronized boolean cacheIsAdaptable(String name, boolean adaptable) {
    adaptableCache.put(name, adaptable ? Boolean.TRUE : Boolean.FALSE);
    return adaptable;
  }

  private synchronized void clearAdaptableCache() {
    this.adaptableCache.clear();
  }

  public void addWriteAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.WRITE);
  }

  public void addSynchronousWriteAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.SYNCHRONOUS_WRITE);
  }

  public void addReadAutolock(String methodPattern) {
    addAutolock(methodPattern, ConfigLockLevel.READ);
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

  public boolean shouldBeAdapted(ClassInfo classInfo) {
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
    if (classContainsAnyRoots(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }
    // custom adapters trump config.
    if (customAdapters.containsKey(fullClassName)) { return cacheIsAdaptable(fullClassName, true); }
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
    synchronized (this.instrumentationDescriptors) {
      for (Iterator i = this.instrumentationDescriptors.iterator(); i.hasNext();) {
        InstrumentationDescriptor rv = (InstrumentationDescriptor) i.next();
        if (rv.matches(classInfo)) { return rv; }
      }
    }
    return DEAFULT_INSTRUMENTATION_DESCRIPTOR;
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

    TransparencyClassSpec spec = getSpec(className);
    if (spec != null) { return spec.getTransients().contains(field); }
    return false;
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
                                                           final boolean forcePortable) {
    String className = classInfo.getName();
    ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
    TransparencyClassSpec spec = getOrCreateSpec(className);

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
    ClassAdapterFactory adapter = (ClassAdapterFactory) this.customAdapters.get(classInfo.getName());
    if (adapter != null) {
      return adapter.create(writer, caller);
    } else {
      ManagerHelper mgrHelper = mgrHelperFactory.createHelper();
      TransparencyClassSpec spec = getOrCreateSpec(classInfo.getName());

      if (forcePortable) {
        if (spec.getInstrumentationAction() == TransparencyClassSpec.NOT_SET) {
          spec.setInstrumentationAction(TransparencyClassSpec.PORTABLE);
        } else {
          logger.info("Not making " + classInfo.getName() + " forcefully portable");
        }
      }

      ClassAdapter dsoAdapter = new TransparencyClassAdapter(classInfo, spec, writer, mgrHelper, lgr, caller,
                                                             portability);
      ClassAdapterFactory factory = spec.getCustomClassAdapter();
      ClassVisitor cv;
      if (factory == null) {
        cv = dsoAdapter;
      } else {
        cv = factory.create(dsoAdapter, caller);
      }

      return new SerialVersionUIDAdder(cv);
    }
  }

  private TransparencyClassSpec basicGetOrCreateSpec(String className, String applicator, boolean rememberSpec) {
    synchronized (classSpecs) {
      TransparencyClassSpec spec = getSpec(className);
      if (spec == null) {
        if (applicator != null) {
          spec = new TransparencyClassSpec(className, this, applicator);
        } else {
          spec = new TransparencyClassSpec(className, this);
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
    synchronized (classSpecs) {
      Assert.eval(!classSpecs.containsKey(spec.getClassName()));
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

  public void verifyBootJarContents() throws IncompleteBootJarException, UnverifiedBootJarException {
    logger.info("Verifying boot jar contents...");
    int missingCount = 0;
    int preInstrumentedCount = 0;
    int bootJarPopulation = 0;
    try {
      BootJar bootJar = BootJar.getDefaultBootJarForReading();
      Set bjClasses = bootJar.getAllPreInstrumentedClasses();
      bootJarPopulation = bjClasses.size();
      TransparencyClassSpec[] allSpecs = getAllSpecs();
      for (int i = 0; i < allSpecs.length; i++) {
        TransparencyClassSpec classSpec = allSpecs[i];
        String message = "";
        if (classSpec.isPreInstrumented()) {
          message = "* " + classSpec.getClassName() + "... ";
          preInstrumentedCount++;
          if (bjClasses.contains(classSpec.getClassName()) || classSpec.isHonorJDKSubVersionSpecific()) {
            message += "ok";
          } else {
            message += "missing";
            missingCount++;
          }
          logger.info(message);
        }
      }
    } catch (BootJarException bjex) {
      throw new UnverifiedBootJarException(
                                           "BootJarException occurred while attempting to verify the contents of the boot jar.",
                                           bjex);
    } catch (IOException ioex) {
      throw new UnverifiedBootJarException(
                                           "IOException occurred while attempting to verify the contents of the boot jar.",
                                           ioex);
    }
    logger.info("Number of classes in the DSO boot jar:" + bootJarPopulation);
    logger.info("Number of classes expected to be in the DSO boot jar:" + preInstrumentedCount);
    logger.info("Number of classes found missing from the DSO boot jar:" + missingCount);
    if (missingCount > 0) { throw new IncompleteBootJarException("Incomplete DSO boot jar; " + missingCount
                                                                 + " pre-instrumented class(es) found missing."); }
  }

  public synchronized TransparencyClassSpec[] getAllSpecs() {
    TransparencyClassSpec[] allspecs = new TransparencyClassSpec[classSpecs.values().size()];
    classSpecs.values().toArray(allspecs);
    return allspecs;
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

  public void addApplicationName(String name) {
    applicationNames.add(name);
  }

  public void addSynchronousWriteApplication(String name) {
    this.synchronousWriteApplications.add(name);
  }

  public void addUserDefinedBootSpec(String className, TransparencyClassSpec spec) {
    userDefinedBootSpecs.put(className, spec);
  }

  public void addNewModule(String name, String version) {
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

}
