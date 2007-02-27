/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;

import com.tc.asm.ClassReader;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.asm.commons.SerialVersionUIDAdder;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.InnerClassNode;
import com.tc.asm.tree.MethodNode;
import com.tc.cluster.ClusterEventListener;
import com.tc.config.Directories;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCNotSupportedMethodException;
import com.tc.exception.TCRuntimeException;
import com.tc.geronimo.GeronimoLoaderNaming;
import com.tc.jboss.JBossLoaderNaming;
import com.tc.logging.CustomerLogging;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TerracottaMBean;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.net.NIOWorkarounds;
import com.tc.object.ObjectID;
import com.tc.object.Portability;
import com.tc.object.PortabilityImpl;
import com.tc.object.SerializationUtil;
import com.tc.object.TCClass;
import com.tc.object.TCObject;
import com.tc.object.TraverseTest;
import com.tc.object.bytecode.AbstractStringBuilderAdapter;
import com.tc.object.bytecode.BufferedWriterAdapter;
import com.tc.object.bytecode.ChangeClassNameHierarchyAdapter;
import com.tc.object.bytecode.ChangeClassNameRootAdapter;
import com.tc.object.bytecode.ChangePackageClassAdapter;
import com.tc.object.bytecode.Clearable;
import com.tc.object.bytecode.DataOutputStreamAdapter;
import com.tc.object.bytecode.DuplicateMethodAdapter;
import com.tc.object.bytecode.HashtableClassAdapter;
import com.tc.object.bytecode.JavaLangReflectProxyClassAdapter;
import com.tc.object.bytecode.JavaLangStringAdapter;
import com.tc.object.bytecode.JavaLangThrowableAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentCyclicBarrierDebugClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentHashMapSegmentAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter;
import com.tc.object.bytecode.JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter;
import com.tc.object.bytecode.JavaUtilTreeMapAdapter;
import com.tc.object.bytecode.LinkedListAdapter;
import com.tc.object.bytecode.LogicalClassSerializationAdapter;
import com.tc.object.bytecode.Manageable;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.ManagerHelperFactory;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.bytecode.MergeTCToJavaClassAdapter;
import com.tc.object.bytecode.NullManager;
import com.tc.object.bytecode.NullTCObject;
import com.tc.object.bytecode.StringBufferAdapter;
import com.tc.object.bytecode.StringGetCharsAdapter;
import com.tc.object.bytecode.TCMap;
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
import com.tc.object.bytecode.hook.impl.SessionsHelper;
import com.tc.object.cache.Cacheable;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.dna.impl.ProxyInstance;
import com.tc.object.field.TCField;
import com.tc.object.loaders.BytecodeProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.loaders.Namespace;
import com.tc.object.loaders.StandardClassLoaderAdapter;
import com.tc.object.loaders.StandardClassProvider;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.InstrumentationLoggerImpl;
import com.tc.plugins.PluginsLoader;
import com.tc.properties.TCProperties;
import com.tc.session.SessionSupport;
import com.tc.text.Banner;
import com.tc.util.AbstractIdentifier;
import com.tc.util.Assert;
import com.tc.util.DebugUtil;
import com.tc.util.EnumerationWrapper;
import com.tc.util.FieldUtils;
import com.tc.util.HashtableKeySetWrapper;
import com.tc.util.HashtableValuesWrapper;
import com.tc.util.ListIteratorWrapper;
import com.tc.util.SetIteratorWrapper;
import com.tc.util.THashMapCollectionWrapper;
import com.tc.util.UnsafeUtil;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;
import com.tcclient.util.HashtableEntrySetWrapper;
import com.tcclient.util.MapEntrySetWrapper;

import gnu.trove.TLinkable;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Tool for creating the DSO boot jar
 */
public class BootJarTool {
  private final static String         OUTPUT_FILE_OPTION           = "o";
  private final static boolean        WRITE_OUT_TEMP_FILE          = true;

  private static final String         DEFAULT_CONFIG_PATH          = "default-config.xml";
  private static final String         DEFAULT_CONFIG_SPEC          = "tc-config.xml";

  private final ClassBytesProvider    tcBytesProvider;
  private final ClassBytesProvider    systemBytesProvider;
  private final DSOClientConfigHelper config;
  private final File                  outputFile;
  private final Portability           portability;

  // various sets that are populated while massaging user defined boot jar specs
  private final Set                   notBootstrapClasses          = new HashSet();
  private final Set                   notAdaptableClasses          = new HashSet();
  private final Set                   logicalSubclasses            = new HashSet();
  private final Set                   autoIncludedBootstrapClasses = new HashSet();
  private final Set                   nonExistingClasses           = new HashSet();

  private InstrumentationLogger       instrumentationLogger;
  private BootJar                     bootJar;
  private BootJarHandler              bootJarHandler;
  private boolean                     quiet;

  public BootJarTool(DSOClientConfigHelper configuration, File outputFile, ClassBytesProvider systemProvider,
                     boolean quiet) {
    this.config = configuration;
    this.outputFile = outputFile;
    this.systemBytesProvider = systemProvider;
    this.tcBytesProvider = new ClassLoaderBytesProvider(getClass().getClassLoader());
    this.bootJarHandler = new BootJarHandler(WRITE_OUT_TEMP_FILE, this.outputFile);
    this.quiet = quiet;
    this.portability = new PortabilityImpl(this.config);
    PluginsLoader.initPlugins(this.config, true);
  }

  public BootJarTool(DSOClientConfigHelper configuration, File outputFile, ClassBytesProvider systemProvider) {
    this(configuration, outputFile, systemProvider, false);
  }

  private boolean isAtLeastJDK15() {
    try {
      systemBytesProvider.getBytesForClass("java.lang.StringBuilder");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private void addJdk15SpecificPreInstrumentedClasses() {
    if (isAtLeastJDK15()) {
      TransparencyClassSpec spec = config.getOrCreateSpec("java.math.MathContext");
      spec.markPreInstrumented();

      addInstrumentedJavaUtilConcurrentLocksReentrantLock();
      addInstrumentedJavaUtilConcurrentLinkedBlockingQueue();
      addInstrumentedJavaUtilConcurrentHashMap();
      addInstrumentedJavaUtilConcurrentCyclicBarrier();
      addInstrumentedJavaUtilConcurrentFutureTask();
    }
  }

  public void generateJar() {
    instrumentationLogger = new InstrumentationLoggerImpl(config.getInstrumentationLoggingOptions());

    //bootJarHandler.setOverwriteMode(commandLine.hasOption("x"));
    try {
      bootJarHandler.validateDirectoryExists();
    } catch (BootJarHandlerException e) {
      exit(e.getMessage(), e.getCause());
    }

    if (!quiet) {
      bootJarHandler.announceCreationStart();
    }

    try {

      bootJar = bootJarHandler.getBootJar();

      addInstrumentedHashMap();
      addInstrumentedHashtable();
      addInstrumentedJavaUtilCollection();

      addJdk15SpecificPreInstrumentedClasses();

      loadTerracottaClass(DebugUtil.class.getName());
      loadTerracottaClass(SessionSupport.class.getName());
      loadTerracottaClass(TCMap.class.getName());
      loadTerracottaClass(com.tc.util.Stack.class.getName());

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
      loadTerracottaClass(Clearable.class.getName());
      loadTerracottaClass(Manager.class.getName());
      loadTerracottaClass(NullManager.class.getName());
      loadTerracottaClass(ManagerUtil.class.getName());
      loadTerracottaClass(ManagerUtil.class.getName() + "$GlobalManagerHolder");
      loadTerracottaClass(TCObject.class.getName());
      loadTerracottaClass(TCClass.class.getName());
      loadTerracottaClass(TCField.class.getName());
      loadTerracottaClass(NullTCObject.class.getName());
      loadTerracottaClass(Cacheable.class.getName());
      loadTerracottaClass(ObjectID.class.getName());
      loadTerracottaClass(AbstractIdentifier.class.getName());
      loadTerracottaClass(TLinkable.class.getName());
      loadTerracottaClass(SessionsHelper.class.getName());
      loadTerracottaClass(GeronimoLoaderNaming.class.getName());
      loadTerracottaClass(JBossLoaderNaming.class.getName());
      loadTerracottaClass(TCLogger.class.getName());
      loadTerracottaClass(Banner.class.getName());
      loadTerracottaClass(StandardClassProvider.class.getName());
      loadTerracottaClass(Namespace.class.getName());
      loadTerracottaClass(ClassProcessorHelper.class.getName());
      loadTerracottaClass(ClassProcessorHelperJDK15.class.getName());
      loadTerracottaClass(ClassProcessorHelper.State.class.getName());
      loadTerracottaClass(ClassProcessorHelper.JarFilter.class.getName());
      loadTerracottaClass(ClassProcessor.class.getName());
      loadTerracottaClass(ClassPreProcessor.class.getName());
      loadTerracottaClass(ClassPostProcessor.class.getName());
      loadTerracottaClass(DSOSpringConfigHelper.class.getName());
      loadTerracottaClass(DSOContext.class.getName());
      loadTerracottaClass(ClassProvider.class.getName());
      loadTerracottaClass(TCRuntimeException.class.getName());
      loadTerracottaClass(FieldUtils.class.getName());
      loadTerracottaClass(UnsafeUtil.class.getName());
      loadTerracottaClass(TCNotSupportedMethodException.class.getName());
      loadTerracottaClass(ExceptionWrapper.class.getName());
      loadTerracottaClass(ExceptionWrapperImpl.class.getName());
      loadTerracottaClass(TraverseTest.class.getName());
      loadTerracottaClass(Os.class.getName());
      loadTerracottaClass(com.tc.util.Util.class.getName());
      loadTerracottaClass(NIOWorkarounds.class.getName());
      loadTerracottaClass(TCProperties.class.getName());
      loadTerracottaClass(ClusterEventListener.class.getName());

      // These two classes need to be specified as literal in order to prevent
      // the static block of IdentityWeakHashMap from executing during generating
      // the boot jar.
      loadTerracottaClass("com.tc.object.util.IdentityWeakHashMap");
      loadTerracottaClass("com.tc.object.util.IdentityWeakHashSet");
      loadTerracottaClass("com.tc.object.bytecode.hook.impl.ArrayManager");

      loadTerracottaClass("com.tc.object.bytecode.NonDistributableObjectRegistry");

      loadTerracottaClass(JavaLangArrayHelpers.class.getName());

      loadTerracottaClass(ProxyInstance.class.getName());

      addManagementClasses();

      addSpringClasses();

      addSunStandardLoaders();
      addInstrumentedJavaLangThrowable();
      addInstrumentedJavaLangStringBuffer();
      addInstrumentedClassLoader();
      addInstrumentedJavaLangString();
      addInstrumentedProxy();
      addTreeMap();

      Map internalSpecs = getTCSpecs();
      loadBootJarClasses(removeAlreadyLoaded(massageSpecs(internalSpecs, true)));

      Map userSpecs = massageSpecs(getUserDefinedSpecs(internalSpecs), false);
      issueWarningsAndErrors();

      // user defined specs should ALWAYS be after internal specs
      loadBootJarClasses(removeAlreadyLoaded(userSpecs));

      adaptClassIfNotAlreadyIncluded(BufferedWriter.class.getName(), BufferedWriterAdapter.class);
      adaptClassIfNotAlreadyIncluded(DataOutputStream.class.getName(), DataOutputStreamAdapter.class);

    } catch (Exception e) {
      exit(bootJarHandler.getCreationErrorMessage(), e);
    }

    try {
      bootJar.close();
      bootJarHandler.announceCreationEnd();
    } catch (IOException e) {
      exit(bootJarHandler.getCloseErrorMessage(), e);
    } catch (BootJarHandlerException e) {
      exit(e.getMessage(), e.getCause());
    }

  }

  private void addManagementClasses() {
    loadTerracottaClass(SessionMonitorMBean.class.getName());
    loadTerracottaClass(SessionMonitorMBean.class.getName() + "$SessionsComptroller");
    loadTerracottaClass(TerracottaMBean.class.getName());
  }

  private boolean shouldIncludeStringBufferAndFriends() {
    Map userSpecs = getUserDefinedSpecs(getTCSpecs());
    return userSpecs.containsKey("java.lang.StringBuffer") || userSpecs.containsKey("java.lang.AbstractStringBuilder")
           || userSpecs.containsKey("java.lang.StringBuilder");

  }

  private void addSpringClasses() {
    /*
     * // Used by aspect definitions at deployment time
     * loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.JoinPointType");
     * loadTerracottaClass("com.tc.aspectwerkz.joinpoint.Signature");
     * loadTerracottaClass("com.tc.aspectwerkz.joinpoint.EnclosingStaticJoinPoint");
     * loadTerracottaClass("com.tc.aspectwerkz.joinpoint.StaticJoinPoint");
     */

    // FIXME extract AW runtime classes
    loadTerracottaClass("com.tc.aspectwerkz.AspectContext");
    loadTerracottaClass("com.tc.aspectwerkz.DeploymentModel$PointcutControlledDeploymentModel");
    loadTerracottaClass("com.tc.aspectwerkz.DeploymentModel");
    loadTerracottaClass("com.tc.aspectwerkz.WeavedTestCase$WeaverTestRunner");
    loadTerracottaClass("com.tc.aspectwerkz.WeavedTestCase");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.After");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AfterFinally");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AfterReturning");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AfterThrowing");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AnnotationConstants");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AnnotationInfo");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Around");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AsmAnnotations");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Aspect");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.AspectAnnotationParser");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Before");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Expression");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Introduce");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.Mixin");
    loadTerracottaClass("com.tc.aspectwerkz.annotation.MixinAnnotationParser");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.AbstractAspectContainer");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.AbstractMixinFactory");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.AdviceInfo");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.AdviceType");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.Aspect");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.AspectContainer");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.DefaultAspectContainerStrategy");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.DefaultMixinFactory");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.MixinFactory");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.AbstractAspectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.Artifact");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.AspectFactoryManager");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.LazyPerXFactoryCompiler$PerClassAspectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.LazyPerXFactoryCompiler$PerThreadAspectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.LazyPerXFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.PerCflowXAspectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.PerJVMAspectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.PerObjectFactoryCompiler$PerInstanceFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.container.PerObjectFactoryCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.management.Aspects");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.management.HasInstanceLevelAspect");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.management.Mixins");
    loadTerracottaClass("com.tc.aspectwerkz.aspect.management.NoAspectBoundException");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.AbstractCflowSystemAspect$1");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.AbstractCflowSystemAspect$Cflow_sample");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.AbstractCflowSystemAspect");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.CflowAspectExpressionVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.CflowBinding");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.CflowCompiler$CompiledCflowAspect");
    loadTerracottaClass("com.tc.aspectwerkz.cflow.CflowCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.compiler.AspectWerkzC");
    loadTerracottaClass("com.tc.aspectwerkz.compiler.AspectWerkzCTask");
    loadTerracottaClass("com.tc.aspectwerkz.compiler.CompileException");
    loadTerracottaClass("com.tc.aspectwerkz.compiler.Utility");
    loadTerracottaClass("com.tc.aspectwerkz.compiler.VerifierClassLoader");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.Command");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.Invoker");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.RemoteProxy");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.RemoteProxyServer");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.RemoteProxyServerManager$1");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.RemoteProxyServerManager");
    loadTerracottaClass("com.tc.aspectwerkz.connectivity.RemoteProxyServerThread");
    loadTerracottaClass("com.tc.aspectwerkz.definition.AdviceDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.AspectDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.DefinitionParserHelper");
    loadTerracottaClass("com.tc.aspectwerkz.definition.DeploymentScope");
    loadTerracottaClass("com.tc.aspectwerkz.definition.DescriptorUtil");
    loadTerracottaClass("com.tc.aspectwerkz.definition.DocumentParser$PointcutInfo");
    loadTerracottaClass("com.tc.aspectwerkz.definition.DocumentParser");
    loadTerracottaClass("com.tc.aspectwerkz.definition.InterfaceIntroductionDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.MixinDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.Pointcut");
    loadTerracottaClass("com.tc.aspectwerkz.definition.PointcutDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.SystemDefinition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.SystemDefinitionContainer");
    loadTerracottaClass("com.tc.aspectwerkz.definition.Virtual");
    loadTerracottaClass("com.tc.aspectwerkz.definition.XmlParser$1");
    loadTerracottaClass("com.tc.aspectwerkz.definition.XmlParser");
    loadTerracottaClass("com.tc.aspectwerkz.definition.aspectj5.Definition$ConcreteAspect");
    loadTerracottaClass("com.tc.aspectwerkz.definition.aspectj5.Definition$Pointcut");
    loadTerracottaClass("com.tc.aspectwerkz.definition.aspectj5.Definition");
    loadTerracottaClass("com.tc.aspectwerkz.definition.aspectj5.DocumentParser");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.AdviceDefinitionBuilder");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.AspectDefinitionBuilder");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.AspectModule");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.AspectModuleDeployer");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.DefinitionBuilder");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.MixinDefinitionBuilder");
    loadTerracottaClass("com.tc.aspectwerkz.definition.deployer.PointcutDefinitionBuilder");
    loadTerracottaClass("com.tc.aspectwerkz.env.EnvironmentDetector");
    loadTerracottaClass("com.tc.aspectwerkz.exception.DefinitionException");
    loadTerracottaClass("com.tc.aspectwerkz.exception.DefinitionNotFoundException");
    loadTerracottaClass("com.tc.aspectwerkz.exception.ExpressionException");
    loadTerracottaClass("com.tc.aspectwerkz.exception.WrappedRuntimeException");
    loadTerracottaClass("com.tc.aspectwerkz.expression.AdvisedClassFilterExpressionVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ArgsIndexVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.DumpVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionContext");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionException");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionInfo");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionNamespace");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionValidateVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ExpressionVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.PointcutType");
    loadTerracottaClass("com.tc.aspectwerkz.expression.SubtypePatternType");
    loadTerracottaClass("com.tc.aspectwerkz.expression.Undeterministic");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTAnd");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTArgParameter");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTArgs");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTAttribute");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTCall");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTCflow");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTCflowBelow");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTClassPattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTConstructorPattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTExecution");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTExpression");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTFieldPattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTGet");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTHandler");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTHasField");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTHasMethod");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTIf");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTMethodPattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTModifier");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTNot");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTOr");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTParameter");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTPointcutReference");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTRoot");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTSet");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTStaticInitialization");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTTarget");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTThis");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTWithin");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ASTWithinCode");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParser$JJCalls");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParser$LookaheadSuccess");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParser");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParserConstants");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParserTokenManager");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParserTreeConstants");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ExpressionParserVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.JJTExpressionParserState");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.Node");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.ParseException");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.SimpleCharStream");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.SimpleNode");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.Token");
    loadTerracottaClass("com.tc.aspectwerkz.expression.ast.TokenMgrError");
    loadTerracottaClass("com.tc.aspectwerkz.expression.regexp.NamePattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.regexp.Pattern");
    loadTerracottaClass("com.tc.aspectwerkz.expression.regexp.TypePattern");
    loadTerracottaClass("com.tc.aspectwerkz.hook.AbstractStarter");
    loadTerracottaClass("com.tc.aspectwerkz.hook.BootClasspathStarter");
    loadTerracottaClass("com.tc.aspectwerkz.hook.ClassLoaderPatcher");
    loadTerracottaClass("com.tc.aspectwerkz.hook.ClassLoaderPreProcessor");
    loadTerracottaClass("com.tc.aspectwerkz.hook.ClassPreProcessor");
    loadTerracottaClass("com.tc.aspectwerkz.hook.StreamRedirectThread");
    loadTerracottaClass("com.tc.aspectwerkz.hook.impl.ClassPreProcessorHelper");
    loadTerracottaClass("com.tc.aspectwerkz.hook.impl.StdoutPreProcessor");
    loadTerracottaClass("com.tc.aspectwerkz.hook.impl.WeavingClassLoader");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.Advice");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.Advisable");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.AdvisableImpl");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.AfterAdvice");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.AfterReturningAdvice");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.AfterThrowingAdvice");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.AroundAdvice");
    loadTerracottaClass("com.tc.aspectwerkz.intercept.BeforeAdvice");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.CatchClauseRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.CatchClauseSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.CodeRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.CodeSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.ConstructorRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.ConstructorSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.EnclosingStaticJoinPoint");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.FieldRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.FieldSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.JoinPoint");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.MemberRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.MemberSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.MethodRtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.MethodSignature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.Rtti");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.Signature");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.StaticJoinPoint");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.CatchClauseRttiImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.CatchClauseSignatureImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.ConstructorRttiImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.ConstructorSignatureImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.EnclosingStaticJoinPointImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.FieldRttiImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.FieldSignatureImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.MethodRttiImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.MethodSignatureImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.MethodTuple");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.StaticInitializationRttiImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.impl.StaticInitializerSignatureImpl");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.AdviceInfoContainer");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.JoinPointManager$CompiledJoinPoint");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.JoinPointManager");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.JoinPointType");
    loadTerracottaClass("com.tc.aspectwerkz.joinpoint.management.SignatureFactory");
    loadTerracottaClass("com.tc.aspectwerkz.perx.PerObjectAspect");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ClassBytecodeRepository");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.Proxy");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyCompilerHelper$1");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyCompilerHelper");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyDelegationCompiler$ProxyCompilerClassVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyDelegationCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyDelegationStrategy$CompositeClassKey");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxyDelegationStrategy");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxySubclassingCompiler$ProxyCompilerClassVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxySubclassingCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.ProxySubclassingStrategy");
    loadTerracottaClass("com.tc.aspectwerkz.proxy.Uuid");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.CflowMetaData");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ClassInfo$NullClassInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ClassInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ClassInfoHelper");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ClassInfoRepository");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ClassList");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ConstructorInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.FieldInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.MemberInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.MetaDataInspector");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.MethodComparator");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.MethodInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ReflectHelper");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.ReflectionInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.StaticInitializationInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.StaticInitializationInfoImpl");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.TypeConverter");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo$ClassInfoClassAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo$ClassNameRetrievalClassAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo$MethodParameterNamesCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfoRepository");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmConstructorInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmFieldInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmMemberInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.AsmMethodInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.FieldStruct");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.MemberStruct");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.asm.MethodStruct");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaClassInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaClassInfoRepository");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaConstructorInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaFieldInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaMemberInfo");
    loadTerracottaClass("com.tc.aspectwerkz.reflect.impl.java.JavaMethodInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.AspectWerkzPreProcessor$Output");
    loadTerracottaClass("com.tc.aspectwerkz.transform.AspectWerkzPreProcessor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.ByteArray");
    loadTerracottaClass("com.tc.aspectwerkz.transform.ClassCacheTuple");
    loadTerracottaClass("com.tc.aspectwerkz.transform.InstrumentationContext");
    loadTerracottaClass("com.tc.aspectwerkz.transform.JoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.Properties");
    loadTerracottaClass("com.tc.aspectwerkz.transform.TransformationConstants");
    loadTerracottaClass("com.tc.aspectwerkz.transform.TransformationUtil");
    loadTerracottaClass("com.tc.aspectwerkz.transform.WeavingStrategy");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AdviceMethodInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmCopyAdapter$CopyAnnotationAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmCopyAdapter$CopyMethodAnnotationElseNullAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmCopyAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmHelper$1");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmHelper$2");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmHelper");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmNullAdapter$NullAnnotationVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmNullAdapter$NullClassAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmNullAdapter$NullFieldAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmNullAdapter$NullMethodAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AsmNullAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AspectInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.AspectModelManager");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.EmittedJoinPoint");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.ProxyWeavingStrategy");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.AbstractJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo$Model");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.CompilationInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.CompilerHelper");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.CompilerInput");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.ConstructorCallJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.ConstructorCallJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.ConstructorExecutionJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.ConstructorExecutionJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.FieldGetJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.FieldGetJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.FieldSetJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.FieldSetJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.HandlerJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.HandlerJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.MatchingJoinPointInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.MethodCallJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.MethodCallJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.MethodExecutionJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.MethodExecutionJoinPointRedefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.RuntimeCheckVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.compiler.StaticInitializationJoinPointCompiler");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.ChangeSet$Element");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.ChangeSet");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.Deployer");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.DeploymentHandle$DefinitionChangeElement");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.DeploymentHandle");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.Redefiner");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.RedefinerFactory$Type");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.deployer.RedefinerFactory");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.model.AopAllianceAspectModel");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.model.AspectWerkzAspectModel$CustomProceedMethodStruct");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.model.AspectWerkzAspectModel");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.model.SpringAspectModel");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.spi.AspectModel$AroundClosureClassInfo$Type");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.spi.AspectModel$AroundClosureClassInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.spi.AspectModel");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddInterfaceVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddMixinMethodsVisitor$AppendToInitMethodCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddMixinMethodsVisitor$MixinFieldInfo");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddMixinMethodsVisitor$PrependToClinitMethodCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddMixinMethodsVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AddWrapperVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AfterObjectInitializationCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AlreadyAddedMethodAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.AlreadyAddedMethodVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorBodyVisitor$DispatchCtorBodyCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorBodyVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorCallVisitor$LookaheadNewDupInvokeSpecialInstructionClassAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorCallVisitor$LookaheadNewDupInvokeSpecialInstructionCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorCallVisitor$NewInvocationStruct");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorCallVisitor$ReplaceNewInstructionCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.ConstructorCallVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.FieldSetFieldGetVisitor$ReplacePutFieldAndGetFieldInstructionCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.FieldSetFieldGetVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.HandlerVisitor$CatchClauseCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.HandlerVisitor$CatchLabelStruct");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.HandlerVisitor$LookaheadCatchLabelsClassAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.HandlerVisitor$LookaheadCatchLabelsMethodAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.HandlerVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.InstanceLevelAspectVisitor$AppendToInitMethodCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.InstanceLevelAspectVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.JoinPointInitVisitor$InsertBeforeClinitCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.JoinPointInitVisitor$InsertBeforeInitJoinPointsCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.JoinPointInitVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.LabelToLineNumberVisitor$1");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.LabelToLineNumberVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.MethodCallVisitor$ReplaceInvokeInstructionCodeAdapter");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.MethodCallVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.MethodExecutionVisitor$1");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.MethodExecutionVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.SerialVersionUidVisitor$Add");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.SerialVersionUidVisitor$FieldItem");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.SerialVersionUidVisitor$Item");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.SerialVersionUidVisitor$MethodItem");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.SerialVersionUidVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.transform.inlining.weaver.StaticInitializationVisitor");
    loadTerracottaClass("com.tc.aspectwerkz.util.ContextClassLoader");
    loadTerracottaClass("com.tc.aspectwerkz.util.EnvironmentDetect");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap$1");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap$2");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap$3");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap$Entry");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap$OrderedIterator");
    loadTerracottaClass("com.tc.aspectwerkz.util.SequencedHashMap");
    loadTerracottaClass("com.tc.aspectwerkz.util.SerializableThreadLocal");
    loadTerracottaClass("com.tc.aspectwerkz.util.Strings");
    loadTerracottaClass("com.tc.aspectwerkz.util.Util");
    loadTerracottaClass("com.tc.aspectwerkz.util.UuidGenerator");

    loadTerracottaClass("com.tc.asm.AnnotationVisitor");
    loadTerracottaClass("com.tc.asm.AnnotationWriter");
    loadTerracottaClass("com.tc.asm.Attribute");
    loadTerracottaClass("com.tc.asm.ByteVector");
    loadTerracottaClass("com.tc.asm.ClassAdapter");
    loadTerracottaClass("com.tc.asm.ClassReader");
    loadTerracottaClass("com.tc.asm.ClassVisitor");
    loadTerracottaClass("com.tc.asm.ClassWriter");
    loadTerracottaClass("com.tc.asm.Edge");
    loadTerracottaClass("com.tc.asm.FieldVisitor");
    loadTerracottaClass("com.tc.asm.FieldWriter");
    loadTerracottaClass("com.tc.asm.Handler");
    loadTerracottaClass("com.tc.asm.Item");
    loadTerracottaClass("com.tc.asm.Label");
    loadTerracottaClass("com.tc.asm.MethodAdapter");
    loadTerracottaClass("com.tc.asm.MethodVisitor");
    loadTerracottaClass("com.tc.asm.MethodWriter");
    loadTerracottaClass("com.tc.asm.Opcodes");
    loadTerracottaClass("com.tc.asm.Type");

    loadTerracottaClass("com.tc.asm.signature.SignatureReader");
    loadTerracottaClass("com.tc.asm.signature.SignatureVisitor");
    loadTerracottaClass("com.tc.asm.signature.SignatureWriter");

    loadTerracottaClass("com.tc.asm.commons.SerialVersionUIDAdder");
    loadTerracottaClass("com.tc.asm.commons.SerialVersionUIDAdder$Item");

    loadTerracottaClass("com.tc.backport175.Annotation");
    loadTerracottaClass("com.tc.backport175.Annotations");
    loadTerracottaClass("com.tc.backport175.ReaderException");

    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationDefaults");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationDefaults$AnnotationDefaultsClassVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationDefaults$AnnotationDefaultsMethodVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationDefaults$DefaultAnnotationBuilderVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$Annotation");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$Array");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$Enum");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$NamedValue");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$NestedAnnotationElement");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationElement$Type");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$AnnotationBuilderVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$AnnotationRetrievingConstructorVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$AnnotationRetrievingFieldVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$AnnotationRetrievingMethodVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$AnnotationRetrievingVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$ClassKey");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$MemberKey");
    loadTerracottaClass("com.tc.backport175.bytecode.AnnotationReader$TraceAnnotationVisitor");
    loadTerracottaClass("com.tc.backport175.bytecode.DefaultBytecodeProvider");
    loadTerracottaClass("com.tc.backport175.bytecode.SignatureHelper");

    loadTerracottaClass("com.tc.backport175.bytecode.spi.BytecodeProvider");

    loadTerracottaClass("com.tc.backport175.proxy.JavaDocAnnotationInvocationHander");
    loadTerracottaClass("com.tc.backport175.proxy.ProxyFactory");
    loadTerracottaClass("com.tc.backport175.proxy.ResolveAnnotationException");

    // FIXME remove unused stuff and move to com.tc. namespace
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$IChangedListener");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$IState");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$IStateChangedListener");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$IStateVisitedListener");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$ITransitionVisitedListener");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$LinkedSet_State");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$State$Transition");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$State");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton$Wrapper_State");
    loadTerracottaClass("com.tc.jrexx.automaton.Automaton");
    loadTerracottaClass("com.tc.jrexx.automaton.IProperties");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$IPState");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$LinkedSet_PState");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$PProperties");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$PState");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_GroupBegin");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_GroupEnd");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_LABEL");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_LITERAL");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_LITERALSET");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_RegExp");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern$TerminalFormat_REPETITION");
    loadTerracottaClass("com.tc.jrexx.regex.Automaton_Pattern");
    loadTerracottaClass("com.tc.jrexx.regex.InvalidExpression");
    loadTerracottaClass("com.tc.jrexx.regex.ParseException");
    loadTerracottaClass("com.tc.jrexx.regex.Pattern");
    loadTerracottaClass("com.tc.jrexx.regex.PatternPro");
    loadTerracottaClass("com.tc.jrexx.regex.PAutomaton$1");
    loadTerracottaClass("com.tc.jrexx.regex.PAutomaton$2");
    loadTerracottaClass("com.tc.jrexx.regex.PAutomaton");
    loadTerracottaClass("com.tc.jrexx.regex.PScanner");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_AndOp");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_EOF");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_GroupBegin");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_GroupEnd");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_NotOp");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_OrOp");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_RegExp");
    loadTerracottaClass("com.tc.jrexx.regex.Terminal_Repetition");

    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$EClosureSet");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$Tupel");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$EClosure");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$Transition");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$ISState");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$ISStateChangedListener");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$LinkedSet_SState");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$SProperties");
    loadTerracottaClass("com.tc.jrexx.set.AutomatonSet_String$SState");
    loadTerracottaClass("com.tc.jrexx.set.CharSet");
    loadTerracottaClass("com.tc.jrexx.set.CharSet$IAbstract");
    loadTerracottaClass("com.tc.jrexx.set.CharSet$LongMap");
    loadTerracottaClass("com.tc.jrexx.set.CharSet$LongMapIterator");
    loadTerracottaClass("com.tc.jrexx.set.CharSet$Wrapper");
    loadTerracottaClass("com.tc.jrexx.set.DFASet");
    loadTerracottaClass("com.tc.jrexx.set.DFASet$State");
    loadTerracottaClass("com.tc.jrexx.set.DFASet$State$Transition");
    loadTerracottaClass("com.tc.jrexx.set.FSAData");
    loadTerracottaClass("com.tc.jrexx.set.FSAData$State");
    loadTerracottaClass("com.tc.jrexx.set.FSAData$State$Transition");
    loadTerracottaClass("com.tc.jrexx.set.ISet_char");
    loadTerracottaClass("com.tc.jrexx.set.ISet_char$Iterator");
    loadTerracottaClass("com.tc.jrexx.set.IState");
    loadTerracottaClass("com.tc.jrexx.set.IStatePro");
    loadTerracottaClass("com.tc.jrexx.set.IStatePro$IChangeListener");
    loadTerracottaClass("com.tc.jrexx.set.IStatePro$ITransition");
    loadTerracottaClass("com.tc.jrexx.set.IStatePro$IVisitListener");

    loadTerracottaClass("com.tc.jrexx.set.SAutomaton");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$IChangeListener");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$SAutomatonChangeListener");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$State");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$StatePro");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$StateProChangedListener");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$StateProVisitedListener");
    loadTerracottaClass("com.tc.jrexx.set.SAutomaton$Transition");

    loadTerracottaClass("com.tc.jrexx.set.SAutomatonData");
    loadTerracottaClass("com.tc.jrexx.set.SAutomatonData$State");
    loadTerracottaClass("com.tc.jrexx.set.SAutomatonData$State$Transition");
    loadTerracottaClass("com.tc.jrexx.set.StateProSet");
    loadTerracottaClass("com.tc.jrexx.set.StateProSet$Iterator");
    loadTerracottaClass("com.tc.jrexx.set.StateProSet$Wrapper_State");
    loadTerracottaClass("com.tc.jrexx.set.XML");
  }

  private void addTreeMap() {
    String className = "java.util.TreeMap";
    byte[] orig = getSystemBytes(className);

    TransparencyClassSpec spec = config.getSpec(className);

    byte[] transformed = doDSOTransform(className, orig);

    ClassReader cr = new ClassReader(transformed);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = new JavaUtilTreeMapAdapter(cw);
    cr.accept(cv, false);

    bootJar.loadClassIntoJar(className, cw.toByteArray(), spec.isPreInstrumented());
  }

  private void issueWarningsAndErrors() {
    issueErrors(nonExistingClasses, "could not be found", "remove or correct",
                "Attempt to add classes that cannot be found: ");
    issueErrors(notBootstrapClasses,
                "are not loaded by the bootstap classloader and have not been included in the boot jar", "remove",
                "Attempt to add classes that are not loaded by bootstrap classloader: ");
    issueErrors(notAdaptableClasses, "are non-adaptable types and have not been included in the boot jar", "remove",
                "Attempt to add non-adaptable classes: ");
    issueErrors(logicalSubclasses,
                "are subclasses of logically managed types and have not been included in the boot jar", "remove",
                "Attemp to add subclasses of logically manages classes: ");
    issueWarnings(autoIncludedBootstrapClasses,
                  "were automatically included in the boot jar since they are required super classes", "add");
  }

  private void issueErrors(Set classes, String desc, String verb, String shortDesc) {
    if (!classes.isEmpty()) {
      Banner.errorBanner("Boot jar creation failed.  The following set of classes " + desc + ". Please " + verb
                         + " them in the <additional-boot-jar-classes> section of the terracotta config: " + classes);
      exit(shortDesc + classes, null);
    }
  }

  private void issueWarnings(Set classes, String desc, String verb) {
    if (!classes.isEmpty()) {
      Banner.warnBanner("The following set of classes " + desc + ". Please " + verb
                        + " them in the <additional-boot-jar-classes> section of the terracotta config: " + classes);
    }
  }

  private Map removeAlreadyLoaded(Map specs) {
    Map rv = new HashMap(specs);
    for (Iterator i = rv.keySet().iterator(); i.hasNext();) {
      String className = (String) i.next();
      if (bootJar.classLoaded(className)) {
        i.remove();
      }
    }

    return Collections.unmodifiableMap(rv);
  }

  private Map massageSpecs(Map specs, boolean tcSpecs) {
    Map rv = new HashMap();

    for (Iterator i = specs.values().iterator(); i.hasNext();) {
      final TransparencyClassSpec spec = (TransparencyClassSpec) i.next();

      final Class topClass = getBootstrapClass(spec.getClassName());
      if (topClass == null) {
        if (tcSpecs && !spec.isHonorJDKSubVersionSpecific()) { throw new AssertionError("Class not found: "
                                                                                        + spec.getClassName()); }
        if (!tcSpecs) {
          nonExistingClasses.add(spec.getClassName());
        }
        continue;
      } else if (topClass.getClassLoader() != null) {
        if (!tcSpecs) {
          notBootstrapClasses.add(topClass.getName());
          continue;
        }
      }

      Set supers = new HashSet();
      boolean add = true;
      if (!config.isLogical(topClass.getName())) {
        Class clazz = topClass;
        while ((clazz != null) && (!portability.isInstrumentationNotNeeded(clazz.getName()))) {
          if (config.isNeverAdaptable(clazz.getName())) {
            if (tcSpecs) { throw new AssertionError("Not adaptable: " + clazz); }

            add = false;
            notAdaptableClasses.add(topClass.getName());
            break;
          }

          if ((clazz != topClass) && config.isLogical(clazz.getName())) {
            if (tcSpecs) { throw new AssertionError(topClass + " is subclass of logical type " + clazz.getName()); }

            add = false;
            logicalSubclasses.add(topClass.getName());
            break;
          }

          if (!specs.containsKey(clazz.getName()) && !bootJar.classLoaded(clazz.getName())) {
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
        for (Iterator supes = supers.iterator(); supes.hasNext();) {
          String name = (String) supes.next();
          autoIncludedBootstrapClasses.add(name);
          TransparencyClassSpec superSpec = config.getOrCreateSpec(name);
          superSpec.markPreInstrumented();
          rv.put(name, superSpec);
        }
      }
    }

    return Collections.unmodifiableMap(rv);
  }

  private static Class getBootstrapClass(String className) {
    try {
      return Class.forName(className, false, ClassLoader.getSystemClassLoader());
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private void loadBootJarClasses(Map specs) {
    for (Iterator iter = specs.values().iterator(); iter.hasNext();) {
      TransparencyClassSpec spec = (TransparencyClassSpec) iter.next();
      byte[] classBytes = doDSOTransform(spec.getClassName(), getSystemBytes(spec.getClassName()));
      announce("Adapting: " + spec.getClassName());
      bootJar.loadClassIntoJar(spec.getClassName(), classBytes, spec.isPreInstrumented());
    }
  }

  private Map getTCSpecs() {
    Map map = new HashMap();

    for (Iterator i = config.getAllSpecs(); i.hasNext();) {
      TransparencyClassSpec spec = (TransparencyClassSpec) i.next();

      if (!spec.isPreInstrumented()) {
        continue;
      }
      map.put(spec.getClassName(), spec);
    }

    return Collections.unmodifiableMap(map);
  }

  private Map getUserDefinedSpecs(Map internalSpecs) {
    Map rv = new HashMap();

    for (Iterator i = config.getAllUserDefinedBootSpecs(); i.hasNext();) {
      TransparencyClassSpec spec = (TransparencyClassSpec) i.next();
      Assert.assertTrue(spec.isPreInstrumented());

      // Take out classes that don't need instrumentation (but the user included anyway)
      if (!portability.isInstrumentationNotNeeded(spec.getClassName())) {
        rv.put(spec.getClassName(), spec);
      }
    }

    // substract TC specs from the user set (overlaps are bad)
    rv.keySet().removeAll(internalSpecs.keySet());

    return Collections.unmodifiableMap(rv);
  }

  private void loadTerracottaClass(String className) {
    bootJar.loadClassIntoJar(className, getTerracottaBytes(className), false);
  }

  private byte[] getTerracottaBytes(String className) {
    return getBytes(className, tcBytesProvider);
  }

  private byte[] getSystemBytes(String className) {
    return getBytes(className, systemBytesProvider);
  }

  private byte[] getBytes(String className, ClassBytesProvider provider) {
    try {
      return provider.getBytesForClass(className);
    } catch (ClassNotFoundException e) {
      throw exit("Error sourcing bytes for class " + className, e);
    }
  }

  private RuntimeException exit(String msg, Throwable t) {
    if (!WRITE_OUT_TEMP_FILE) {
      bootJar.setCreationErrorOccurred(true);
    }

    if (bootJar != null) {
      try {
        bootJar.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);

    if (t != null) {
      t.printStackTrace(pw);
    }

    if (msg != null) {
      pw.println("\n*************** ERROR ***************\n* " + msg + "\n*************************************");
    }

    String output = sw.toString();
    if (output.length() == 0) {
      new Throwable("Unspecified error creating boot jar").printStackTrace(pw);
      output = sw.toString();
    }

    System.err.println(output);
    System.err.flush();
    System.exit(1);

    return new RuntimeException("VM Should have exited");
  }

  private void addInstrumentedJavaLangStringBuffer() {
    boolean makePortable = shouldIncludeStringBufferAndFriends();

    if (makePortable) {
      addPortableStringBuffer();
    } else {
      addNonPortableStringBuffer();
    }
  }

  private void addPortableStringBuffer() {
    boolean isJDK15 = isAtLeastJDK15();
    if (isJDK15) {
      addAbstractStringBuilder();
    }

    byte[] bytes = getSystemBytes("java.lang.StringBuffer");

    // 1st pass
    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new StringBufferAdapter(cw, Vm.VERSION);
    cr.accept(cv, false);
    bytes = cw.toByteArray();

    // 2nd pass
    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);
    cv = new DuplicateMethodAdapter(cw, Collections.singleton("getChars(II[CI)V"));
    cr.accept(cv, false);
    bytes = cw.toByteArray();

    // 3rd pass (regular DSO instrumentation)
    TransparencyClassSpec spec = config.getOrCreateSpec("java.lang.StringBuffer");
    spec.markPreInstrumented();
    config.addWriteAutolock("* java.lang.StringBuffer.*(..)");
    bytes = doDSOTransform(spec.getClassName(), bytes);

    // 4th pass (String.getChars(..) calls)
    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);
    cv = new StringGetCharsAdapter(cw, new String[] { "^" + DuplicateMethodAdapter.UNMANAGED_PREFIX + ".*" });
    cr.accept(cv, false);
    bytes = cw.toByteArray();

    // 5th pass (fixups)
    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);
    cv = new StringBufferAdapter.FixUp(cw, Vm.VERSION);
    cr.accept(cv, false);
    bytes = cw.toByteArray();

    bootJar.loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());
  }

  private void addNonPortableStringBuffer() {
    // even if we aren't making StringBu[ild|ff]er portable, we still need to make
    // sure it calls the fast getChars() methods on String

    boolean isJDK15 = isAtLeastJDK15();

    String className = isJDK15 ? "java.lang.AbstractStringBuilder" : "java.lang.StringBuffer";
    TransparencyClassSpec spec = config.getOrCreateSpec(className);
    spec.markPreInstrumented();

    byte[] bytes = getSystemBytes(className);

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new StringGetCharsAdapter(cw, new String[] { ".*" });
    cr.accept(cv, false);
    bytes = cw.toByteArray();

    bootJar.loadClassIntoJar(className, bytes, spec.isPreInstrumented());
  }

  private void addAbstractStringBuilder() {
    String className = "java.lang.AbstractStringBuilder";

    byte[] classBytes = getSystemBytes(className);

    ClassReader cr = new ClassReader(classBytes);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new DuplicateMethodAdapter(cw, Collections.singleton("getChars(II[CI)V"));
    cr.accept(cv, false);
    classBytes = cw.toByteArray();

    TransparencyClassSpec spec = config.getOrCreateSpec(className);
    spec.markPreInstrumented();

    classBytes = doDSOTransform(className, classBytes);

    cr = new ClassReader(classBytes);
    cw = new ClassWriter(true);
    cv = new AbstractStringBuilderAdapter(cw);
    cr.accept(cv, false);

    cr = new ClassReader(cw.toByteArray());
    cw = new ClassWriter(true);
    cv = new StringGetCharsAdapter(cw, new String[] { "^" + DuplicateMethodAdapter.UNMANAGED_PREFIX + ".*" });
    cr.accept(cv, false);

    bootJar.loadClassIntoJar(className, cw.toByteArray(), spec.isPreInstrumented());
  }

  private void addInstrumentedProxy() {
    String className = "java.lang.reflect.Proxy";
    byte[] bytes = getSystemBytes(className);

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = new JavaLangReflectProxyClassAdapter(cw);
    cr.accept(cv, false);

    bytes = cw.toByteArray();

    TransparencyClassSpec spec = config.getOrCreateSpec(className);
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar(className, bytes, true);
  }

  private void addInstrumentedJavaLangString() {
    byte[] orig = getSystemBytes("java.lang.String");

    ClassReader cr = new ClassReader(orig);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = new JavaLangStringAdapter(cw, Vm.VERSION, shouldIncludeStringBufferAndFriends());
    cr.accept(cv, false);

    bootJar.loadClassIntoJar("java.lang.String", cw.toByteArray(), false);
  }

  private void addSunStandardLoaders() {
    byte[] orig = getSystemBytes("sun.misc.Launcher$AppClassLoader");
    ClassReader cr = new ClassReader(orig);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new StandardClassLoaderAdapter(cw, Namespace.getStandardSystemLoaderName());
    cr.accept(cv, false);
    bootJar.loadClassIntoJar("sun.misc.Launcher$AppClassLoader", cw.toByteArray(), false);

    orig = getSystemBytes("sun.misc.Launcher$ExtClassLoader");
    cr = new ClassReader(orig);
    cw = new ClassWriter(true);
    cv = new StandardClassLoaderAdapter(cw, Namespace.getStandardExtensionsLoaderName());
    cr.accept(cv, false);
    bootJar.loadClassIntoJar("sun.misc.Launcher$ExtClassLoader", cw.toByteArray(), false);
  }

  private void addInstrumentedJavaLangThrowable() {
    String className = "java.lang.Throwable";
    byte[] orig = getSystemBytes(className);

    TransparencyClassSpec spec = config.getOrCreateSpec(className);
    spec.markPreInstrumented();
    spec.setHonorTransient(true);

    ClassReader cr = new ClassReader(orig);
    ClassWriter cw = new ClassWriter(true);
    ManagerHelperFactory mgrFactory = new ManagerHelperFactory();

    ClassVisitor cv = new JavaLangThrowableAdapter(spec, cw, mgrFactory.createHelper(), instrumentationLogger,
                                                   getClass().getClassLoader(), portability);
    cr.accept(cv, false);

    bootJar.loadClassIntoJar(className, cw.toByteArray(), spec.isPreInstrumented());
  }

  /**
   * This instrumentation is temporary to add debug statements to the CyclicBarrier class.
   */
  private void addInstrumentedJavaUtilConcurrentCyclicBarrier() {
    if (!isAtLeastJDK15()) { return; }

    byte[] bytes = getSystemBytes("java.util.concurrent.CyclicBarrier");

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new JavaUtilConcurrentCyclicBarrierDebugClassAdapter(cw);

    cr.accept(cv, false);

    bytes = cw.toByteArray();

    TransparencyClassSpec spec = config.getOrCreateSpec("java.util.concurrent.CyclicBarrier");
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.CyclicBarrier", bytes, true);
  }

  private void addInstrumentedJavaUtilConcurrentHashMap() {
    if (!isAtLeastJDK15()) { return; }

    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapEntrySetWrapper");
    loadTerracottaClass("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$IteratorWrapper");

    byte[] bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap");

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);
    ClassVisitor cv = new JavaUtilConcurrentHashMapAdapter(cw);

    cr.accept(cv, false);

    bytes = cw.toByteArray();

    TransparencyClassSpec spec = config.getOrCreateSpec("java.util.concurrent.ConcurrentHashMap",
                                                        "com.tc.object.applicator.ConcurrentHashMapApplicator");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.ConcurrentHashMap", bytes, true);

    bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$Segment");
    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);
    cv = new JavaUtilConcurrentHashMapSegmentAdapter(cw);

    cr.accept(cv, false);

    bytes = cw.toByteArray();

    spec = config.getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$Segment");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    spec.setCallConstructorOnLoad(true);
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$Segment", bytes, spec.isPreInstrumented());

    bytes = getTerracottaBytes("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper");
    spec = config.getOrCreateSpec("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper");
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("com.tcclient.util.ConcurrentHashMapEntrySetWrapper$EntryWrapper", bytes, spec
        .isPreInstrumented());

    bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$Values");
    spec = config.getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$Values");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$Values", bytes, spec.isPreInstrumented());

    bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$KeySet");
    spec = config.getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$KeySet");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$KeySet", bytes, spec.isPreInstrumented());

    bytes = getSystemBytes("java.util.concurrent.ConcurrentHashMap$HashIterator");
    spec = config.getOrCreateSpec("java.util.concurrent.ConcurrentHashMap$HashIterator");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.ConcurrentHashMap$HashIterator", bytes, spec.isPreInstrumented());
  }

  private void addInstrumentedJavaUtilConcurrentLinkedBlockingQueue() {
    if (!isAtLeastJDK15()) { return; }

    // Instrumentation for Itr inner class
    byte[] bytes = getSystemBytes("java.util.concurrent.LinkedBlockingQueue$Itr");

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = new JavaUtilConcurrentLinkedBlockingQueueIteratorClassAdapter(cw);
    cr.accept(cv, false);

    bytes = cw.toByteArray();
    bootJar.loadClassIntoJar("java.util.concurrent.LinkedBlockingQueue$Itr", bytes, true);

    // Instrumentation for Node inner class
    bytes = getSystemBytes("java.util.concurrent.LinkedBlockingQueue$Node");

    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);

    cv = new JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter(cw);
    cr.accept(cv, false);

    bytes = cw.toByteArray();
    bootJar.loadClassIntoJar("java.util.concurrent.LinkedBlockingQueue$Node", bytes, true);

    // Instrumentation for LinkedBlockingQueue class
    bytes = getSystemBytes("java.util.concurrent.LinkedBlockingQueue");

    cr = new ClassReader(bytes);
    cw = new ClassWriter(true);

    cv = new JavaUtilConcurrentLinkedBlockingQueueClassAdapter(cw);
    cr.accept(cv, false);

    bytes = cw.toByteArray();

    TransparencyClassSpec spec = config.getOrCreateSpec("java.util.concurrent.LinkedBlockingQueue",
                                                        "com.tc.object.applicator.LinkedBlockingQueueApplicator");
    spec.addMethodAdapter(SerializationUtil.TAKE_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.TakeAdapter());
    spec.addMethodAdapter(SerializationUtil.POLL_TIMEOUT_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.TakeAdapter());
    spec.addMethodAdapter(SerializationUtil.POLL_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.TakeAdapter());
    spec.addMethodAdapter(SerializationUtil.CLEAR_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.ClearAdapter());
    spec.addMethodAdapter(SerializationUtil.DRAIN_TO_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.ClearAdapter());
    spec.addMethodAdapter(SerializationUtil.DRAIN_TO_N_SIGNATURE,
                          new JavaUtilConcurrentLinkedBlockingQueueAdapter.RemoveFirstNAdapter());
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);
    bootJar.loadClassIntoJar("java.util.concurrent.LinkedBlockingQueue", bytes, spec.isPreInstrumented());
  }
  
  private void addInstrumentedJavaUtilConcurrentFutureTask() {

    if (!isAtLeastJDK15()) { return; }
    Map instrumentedContext = new HashMap();
    
    TransparencyClassSpec spec = config.getOrCreateSpec("java.util.concurrent.FutureTask");
    spec.setHonorTransient(true);
    spec.setCallConstructorOnLoad(true);
    spec.markPreInstrumented();
    changeClassName("java.util.concurrent.FutureTaskTC", "java.util.concurrent.FutureTaskTC", "java.util.concurrent.FutureTask", instrumentedContext);

    config.addWriteAutolock("* java.util.concurrent.FutureTask$Sync.*(..)");

    spec = config.getOrCreateSpec("java.util.concurrent.FutureTask$Sync");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    spec.addDistributedMethodCall("managedInnerCancel", "()V");
    changeClassName("java.util.concurrent.FutureTaskTC$Sync", "java.util.concurrent.FutureTaskTC", "java.util.concurrent.FutureTask", instrumentedContext);
  }

  private void addInstrumentedJavaUtilCollection() {
    TransparencyClassSpec spec = config.getOrCreateSpec("java.util.HashSet",
                                                        "com.tc.object.applicator.HashSetApplicator");
    spec.addIfTrueLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addIfTrueLogSpec(SerializationUtil.REMOVE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addSetIteratorWrapperSpec(SerializationUtil.ITERATOR_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = config.getOrCreateSpec("java.util.LinkedHashSet", "com.tc.object.applicator.HashSetApplicator");
    addSerializationInstrumentedCode(spec);

    spec = config.getOrCreateSpec("java.util.TreeSet", "com.tc.object.applicator.TreeSetApplicator");
    spec.addIfTrueLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addIfTrueLogSpec(SerializationUtil.REMOVE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addSetIteratorWrapperSpec(SerializationUtil.ITERATOR_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.SUBSET_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.HEADSET_SIGNATURE);
    spec.addViewSetWrapperSpec(SerializationUtil.TAILSET_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = config.getOrCreateSpec("java.util.LinkedList", "com.tc.object.applicator.ListApplicator");
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
    spec.addIfTrueLogSpec(SerializationUtil.REMOVE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_RANGE_SIGNATURE);
    spec.addMethodAdapter("listIterator(I)Ljava/util/ListIterator;", new LinkedListAdapter.ListIteratorAdapter());
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = config.getOrCreateSpec("java.util.Vector", "com.tc.object.applicator.ListApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.INSERT_ELEMENT_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_SIGNATURE);
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

    spec = config.getOrCreateSpec("java.util.Stack", "com.tc.object.applicator.ListApplicator");
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);

    spec = config.getOrCreateSpec("java.util.ArrayList", "com.tc.object.applicator.ListApplicator");
    spec.addAlwaysLogSpec(SerializationUtil.ADD_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_AT_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.ADD_ALL_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_AT_SIGNATURE);
    spec.addIfTrueLogSpec(SerializationUtil.REMOVE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.REMOVE_RANGE_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.SET_SIGNATURE);
    spec.addAlwaysLogSpec(SerializationUtil.CLEAR_SIGNATURE);
    spec.addArrayCopyMethodCodeSpec(SerializationUtil.TO_ARRAY_SIGNATURE);
    addSerializationInstrumentedCode(spec);
  }

  private void addSerializationInstrumentedCode(TransparencyClassSpec spec) {
    byte[] bytes = getSystemBytes(spec.getClassName());
    spec.markPreInstrumented();
    bytes = doDSOTransform(spec.getClassName(), bytes);

    ClassReader cr = new ClassReader(bytes);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = new LogicalClassSerializationAdapter.LogicalClassSerializationClassAdapter(cw, spec
        .getClassName());

    cr.accept(cv, false);
    bytes = cw.toByteArray();
    bootJar.loadClassIntoJar(spec.getClassName(), bytes, spec.isPreInstrumented());

  }

  private void addInstrumentedHashtable() {
    String jMapClassNameDots = "java.util.Hashtable";
    String tcMapClassNameDots = "java.util.HashtableTC";
    Map instrumentedContext = new HashMap();
    mergeClass(tcMapClassNameDots, jMapClassNameDots, instrumentedContext, HashtableClassAdapter.createMethod());
  }

  private void addInstrumentedLinkedHashMap(Map instrumentedContext) {
    String jMapClassNameDots = "java.util.LinkedHashMap";
    String tcMapClassNameDots = "java.util.LinkedHashMapTC";
    mergeClass(tcMapClassNameDots, jMapClassNameDots, instrumentedContext, null);
  }

  private void addInstrumentedHashMap() {
    String jMapClassNameDots = "java.util.HashMap";
    String tcMapClassNameDots = "java.util.HashMapTC";
    Map instrumentedContext = new HashMap();
    mergeClass(tcMapClassNameDots, jMapClassNameDots, instrumentedContext, null);

    addInstrumentedLinkedHashMap(instrumentedContext);
  }

  private void replaceMethod(List methods, MethodNode replacedMethod) {
    for (Iterator i = methods.iterator(); i.hasNext();) {
      MethodNode m = (MethodNode) i.next();
      if (m.name.equals(replacedMethod.name) && m.desc.equals(replacedMethod.desc)) {
        i.remove();
        break;
      }
    }
    methods.add(replacedMethod);
  }

  private void mergeClass(String tcClassNameDots, String jClassNameDots, Map instrumentedContext,
                          MethodNode replacedMethod) {
    byte[] tcData = getSystemBytes(tcClassNameDots);

    ClassReader tcCR = new ClassReader(tcData);
    ClassNode tcCN = new ClassNode();
    tcCR.accept(tcCN, true);
    if (replacedMethod != null) {
      replaceMethod(tcCN.methods, replacedMethod);
    }

    byte[] jData = getSystemBytes(jClassNameDots);
    ClassReader jCR = new ClassReader(jData);
    ClassWriter cw = new ClassWriter(true);

    TransparencyClassAdapter dsoAdapter = config.createDsoClassAdapterFor(cw, jClassNameDots, instrumentationLogger,
                                                                          getClass().getClassLoader(), true);
    ClassVisitor cv = new SerialVersionUIDAdder(new MergeTCToJavaClassAdapter(cw, dsoAdapter, jClassNameDots,
                                                                              tcClassNameDots, tcCN,
                                                                              instrumentedContext));

    jCR.accept(cv, false);
    jData = cw.toByteArray();

    bootJar.loadClassIntoJar(jClassNameDots, jData, true);

    List innerClasses = tcCN.innerClasses;
    for (Iterator i = innerClasses.iterator(); i.hasNext();) {
      InnerClassNode innerClass = (InnerClassNode) i.next();
      if (!innerClass.outerName.equals(tcClassNameDots.replace(ChangeClassNameHierarchyAdapter.DOT_DELIMITER,
                                                               ChangeClassNameHierarchyAdapter.SLASH_DELIMITER))) {
        continue;
      }
      changeClassName(innerClass.name, tcClassNameDots, jClassNameDots, instrumentedContext);
    }
  }

  private void changeClassName(String fullClassNameDots, String classNameDotsToBeChanged, String classNameDotsReplaced,
                               Map instrumentedContext) {
    byte[] data = getSystemBytes(fullClassNameDots);
    ClassReader cr = new ClassReader(data);
    ClassWriter cw = new ClassWriter(true);

    String replacedClassNameDots = ChangeClassNameRootAdapter.replaceClassName(fullClassNameDots, classNameDotsToBeChanged,
                                                                               classNameDotsReplaced);
    TransparencyClassAdapter dsoAdapter = config.createDsoClassAdapterFor(cw, replacedClassNameDots,
                                                                          instrumentationLogger, getClass()
                                                                              .getClassLoader(), true);
    ClassVisitor cv = new ChangeClassNameRootAdapter(dsoAdapter, fullClassNameDots, classNameDotsToBeChanged,
                                                     classNameDotsReplaced, instrumentedContext, null);

    cr.accept(cv, false);
    data = cw.toByteArray();
    bootJar.loadClassIntoJar(replacedClassNameDots, data, true);

  }

  private byte[] changePackageAndGetBytes(String className, byte[] data, String targetClassName,
                                          String targetPackageName, String newPackageName) {
    ClassReader cr = new ClassReader(data);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = config.createClassAdapterFor(cw, className, instrumentationLogger, getClass().getClassLoader(),
                                                   true);
    cv = new ChangePackageClassAdapter(cv, targetClassName, targetPackageName, newPackageName, null);
    cr.accept(cv, false);

    return cw.toByteArray();
  }

  private void addInstrumentedJavaUtilConcurrentLocksReentrantLock() {
    if (!isAtLeastJDK15()) { return; }

    byte[] bytes = getSystemBytes("com.tc.util.concurrent.locks.ReentrantLock");
    TransparencyClassSpec spec = config.getOrCreateSpec("com.tc.util.concurrent.locks.ReentrantLock");
    spec.setHonorTransient(true);
    spec.setCallConstructorOnLoad(true);
    spec.markPreInstrumented();
    bytes = changePackageAndGetBytes("com.tc.util.concurrent.locks.ReentrantLock", bytes, "ReentrantLock",
                                     "com.tc.util.concurrent.locks", "java.util.concurrent.locks");
    bootJar.loadClassIntoJar("java.util.concurrent.locks.ReentrantLock", bytes, spec.isPreInstrumented());

    // we need to remove this spec once the package name is changed because we no longer
    // have com.tc.util.concurrent.locks.ReentrantLock and the massageSpec will complain if
    // we do not remove this spec.
    config.removeSpec("com.tc.util.concurrent.locks.ReentrantLock");

    bytes = getSystemBytes("com.tc.util.concurrent.locks.ReentrantLock$ConditionObject");
    spec = config.getOrCreateSpec("com.tc.util.concurrent.locks.ReentrantLock$ConditionObject");
    spec.disableWaitNotifyCodeSpec("signal()V");
    spec.disableWaitNotifyCodeSpec("signalAll()V");
    spec.setHonorTransient(true);
    spec.markPreInstrumented();
    bytes = changePackageAndGetBytes("com.tc.util.concurrent.locks.ReentrantLock$ConditionObject", bytes,
                                     "ReentrantLock", "com.tc.util.concurrent.locks", "java.util.concurrent.locks");
    bootJar.loadClassIntoJar("java.util.concurrent.locks.ReentrantLock$ConditionObject", bytes, spec
        .isPreInstrumented());

    config.removeSpec("com.tc.util.concurrent.locks.ReentrantLock$ConditionObject");

    bytes = getSystemBytes("com.tc.util.concurrent.locks.ReentrantLock$SyncCondition");
    spec = config.getOrCreateSpec("com.tc.util.concurrent.locks.ReentrantLock$SyncCondition");
    spec.markPreInstrumented();
    bytes = changePackageAndGetBytes("com.tc.util.concurrent.locks.ReentrantLock$SyncCondition", bytes,
                                     "ReentrantLock", "com.tc.util.concurrent.locks", "java.util.concurrent.locks");
    bootJar.loadClassIntoJar("java.util.concurrent.locks.ReentrantLock$SyncCondition", bytes, spec.isPreInstrumented());

    config.removeSpec("com.tc.util.concurrent.locks.ReentrantLock$SyncCondition");

  }

  private void addInstrumentedClassLoader() {
    // patch the java.lang.ClassLoader
    ClassLoaderPreProcessorImpl adapter = new ClassLoaderPreProcessorImpl();
    byte[] patched = adapter.preProcess(getSystemBytes("java.lang.ClassLoader"));

    bootJar.loadClassIntoJar("java.lang.ClassLoader", patched, false);
  }

  protected byte[] doDSOTransform(String name, byte[] data) {
    // adapts the class on the fly
    ClassReader cr = new ClassReader(data);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv = config.createClassAdapterFor(cw, name, instrumentationLogger, getClass().getClassLoader(), true);
    cr.accept(cv, false);

    return cw.toByteArray();
  }

  private void adaptClassIfNotAlreadyIncluded(String className, Class adapter) {
    if (bootJar.classLoaded(className)) { return; }

    byte[] orig = getSystemBytes(className);

    ClassReader cr = new ClassReader(orig);
    ClassWriter cw = new ClassWriter(true);

    ClassVisitor cv;
    try {
      cv = (ClassVisitor) adapter.getConstructor(new Class[] { ClassVisitor.class }).newInstance(new Object[] { cw });
    } catch (Exception e) {
      throw exit("Can't instaniate class apapter using class " + adapter, e);
    }
    cr.accept(cv, false);

    bootJar.loadClassIntoJar(className, cw.toByteArray(), false);
  }

  protected void announce(String msg) {
    if (!quiet) System.out.println(msg);
  }

  private static File getInstallationDir() {
    try {
      return Directories.getInstallationRoot();
    } catch (FileNotFoundException fnfe) {
      return null;
    }
  }

  public static void main(String[] args) throws Exception {
    File installDir = getInstallationDir();
    String outputFileOptionMsg = "path to store boot JAR"
                                 + (installDir != null ? "\ndefault: [TC_INSTALL_DIR]/lib/dso-boot" : "");
    Option targetFileOption = new Option(OUTPUT_FILE_OPTION, true, outputFileOptionMsg);
    targetFileOption.setArgName("file");
    targetFileOption.setLongOpt("output-file");
    targetFileOption.setArgs(1);
    targetFileOption.setRequired(installDir == null);
    targetFileOption.setType(String.class);

    Option configFileOption = new Option("f", "config", true, "configuration file (optional)");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);
    configFileOption.setRequired(false);

    Option verboseOption = new Option("v", "verbose");
    verboseOption.setType(String.class);
    verboseOption.setRequired(false);

    Option helpOption = new Option("h", "help");
    helpOption.setType(String.class);
    helpOption.setRequired(false);

    Options options = new Options();
    options.addOption(targetFileOption);
    options.addOption(configFileOption);
    options.addOption(verboseOption);
    options.addOption(helpOption);

    CommandLine commandLine = null;

    try {
      commandLine = new PosixParser().parse(options, args);
    } catch (ParseException pe) {
      new HelpFormatter().printHelp("java " + BootJarTool.class.getName(), options);
      System.exit(1);
    }

    if (commandLine.hasOption("h")) {
      new HelpFormatter().printHelp("java " + BootJarTool.class.getName(), options);
      System.exit(1);
    }

    if (!commandLine.hasOption("f")) {
      String cwd = System.getProperty("user.dir");
      File localConfig = new File(cwd, DEFAULT_CONFIG_SPEC);
      String configSpec;

      if (localConfig.exists()) {
        configSpec = localConfig.getAbsolutePath();
      } else {
        String packageName = BootJarTool.class.getPackage().getName();
        configSpec = "resource:///" + packageName.replace('.', '/') + "/" + DEFAULT_CONFIG_PATH;
      }

      String[] newArgs = new String[args.length + 2];
      System.arraycopy(args, 0, newArgs, 0, args.length);
      newArgs[newArgs.length - 2] = "-f";
      newArgs[newArgs.length - 1] = configSpec;

      commandLine = new PosixParser().parse(options, newArgs);
    }

    StandardTVSConfigurationSetupManagerFactory factory;
    factory = new StandardTVSConfigurationSetupManagerFactory(commandLine, false,
                                                              new FatalIllegalConfigurationChangeHandler());
    boolean verbose = commandLine.hasOption("v");
    TCLogger logger = verbose ? CustomerLogging.getConsoleLogger() : new NullTCLogger();
    L1TVSConfigurationSetupManager config = factory.createL1TVSConfigurationSetupManager(logger);

    File outputFile;

    if (!commandLine.hasOption(OUTPUT_FILE_OPTION)) {
      File libDir = new File(installDir, "lib");
      outputFile = new File(libDir, "dso-boot");
      if(!outputFile.exists()) {
        outputFile.mkdirs();
      }
    } else {
      outputFile = new File(commandLine.getOptionValue(OUTPUT_FILE_OPTION)).getAbsoluteFile();
    }

    if (outputFile.isDirectory()) {
      outputFile = new File(outputFile, BootJarSignature.getBootJarNameForThisVM());
    }
    
    // This used to be a provider that read from a specified rt.jar (to let us create boot jars for other platforms).
    // That requirement is no more, but might come back, so I'm leaving at least this much scaffolding in place
    // WAS: systemProvider = new RuntimeJarBytesProvider(...)

    ClassBytesProvider systemProvider = new ClassLoaderBytesProvider(ClassLoader.getSystemClassLoader());
    new BootJarTool(new StandardDSOClientConfigHelper(config, false), outputFile, systemProvider, !verbose).generateJar();
  }

  public static class RuntimeJarBytesProvider implements ClassBytesProvider {
    private final JarFile jar;

    public RuntimeJarBytesProvider(File runtimeJar) throws IOException {
      this.jar = new JarFile(runtimeJar);
    }

    public byte[] getBytesForClass(String className) throws ClassNotFoundException {
      ZipEntry entry = jar.getEntry(BootJar.classNameToFileName(className));
      if (entry == null) { throw new ClassNotFoundException(className); }

      try {
        return IOUtils.toByteArray(jar.getInputStream(entry));
      } catch (IOException e) {
        throw new ClassNotFoundException(className, e);
      }
    }
  }

}
