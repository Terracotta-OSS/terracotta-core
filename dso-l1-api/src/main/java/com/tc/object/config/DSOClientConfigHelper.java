/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.object.Portability;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.ReconnectConfig;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Knows how to interpret the DSO client config and tell you things like whether a class is portable. This interface
 * extends DSOApplicationConfig which is a much simpler interface suitable for manipulating the config from the
 * perspective of generating a configuration file.
 */
public interface DSOClientConfigHelper extends DSOMBeanConfig, ModuleConfiguration {
  void addRoot(String rootName, String rootFieldName);

  void addPermanentExcludePattern(String pattern);

  void addNonportablePattern(String pattern);

  void addIncludePattern(String expression, boolean honorTransient, String methodToCallOnLoad, boolean honorVolatile);

  String[] processArguments();

  String rawConfigText();

  String[] getMissingRootDeclarations(ClassInfo classInfo);

  boolean shouldBeAdapted(ClassInfo classInfo);

  boolean isNeverAdaptable(ClassInfo classInfo);

  boolean isLogical(String theClass);

  DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions();

  TransparencyClassSpec[] getAllSpecs();

  Iterator getAllUserDefinedBootSpecs();

  TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, ClassInfo classInfo,
                                                    InstrumentationLogger lgr, ClassLoader caller,
                                                    final boolean forcePortable, boolean honorTransient);

  ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                     ClassLoader caller);

  ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                     ClassLoader caller, boolean disableSuperClassTypeChecking);

  boolean isCallConstructorOnLoad(ClassInfo classInfo);

  // String getChangeApplicatorClassNameFor(String className);
  Class getChangeApplicator(Class clazz);

  boolean addTunneledMBeanDomain(String tunneledMBeanDomain);

  // HACK: is also in IStandardDSOClientConfigHelper
  TransparencyClassSpec getOrCreateSpec(String className);

  // HACK: is also in IStandardDSOClientConfigHelper
  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo);

  boolean isRoot(FieldInfo fi);

  boolean isRootDSOFinal(FieldInfo fi);

  boolean isTransient(int modifiers, ClassInfo classInfo, String field);

  /**
   * Indicates whether a particular field of a class will be injected by DSO.
   * 
   * @return {@code true} when the field is injected; or {@code false} otherwise
   */
  boolean isInjectedField(String className, String fieldName);

  String getInjectedFieldType(ClassInfo classInfo, String field);

  boolean isVolatile(int modifiers, ClassInfo classInfo, String field);

  String rootNameFor(FieldInfo fi);

  boolean isLockMethod(MemberInfo memberInfo);

  DistributedMethodSpec getDmiSpec(MemberInfo memberInfo);

  TransparencyClassSpec getSpec(String className);

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

  DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  int getFaultCount();

  void addWriteAutolock(String methodPattern);

  void addWriteAutolock(String methodPattern, String lockContextInfo);

  void addSynchronousWriteAutolock(String methodPattern);

  void addLock(String methodPattern, LockDefinition lockDefinition);

  void addReadAutolock(String methodPattern);

  void addAutolock(String methodPattern, ConfigLockLevel type);

  void addAutolock(String methodPattern, ConfigLockLevel type, String configurationText);

  void addAutoLockExcludePattern(String expression);

  void addReadAutoSynchronize(String methodPattern);

  void addWriteAutoSynchronize(String methodPattern);

  void setFaultCount(int count);

  void addRoot(Root root, boolean addSpecForClass);

  boolean matches(final Lock lock, final MemberInfo methodInfo);

  boolean matches(final String expression, final MemberInfo methodInfo);

  void addTransient(String className, String fieldName);

  void addInjectedField(String className, String fieldName, String instanceType);

  boolean hasOnLoadInjection(ClassInfo classInfo);

  String getOnLoadScriptIfDefined(ClassInfo classInfo);

  String getPreCreateMethodIfDefined(String className);

  String getPostCreateMethodIfDefined(String className);

  String getOnLoadMethodIfDefined(ClassInfo classInfo);

  boolean isUseNonDefaultConstructor(Class clazz);

  // HACK: is also in IStandardDSOClientConfigHelper
  void addIncludePattern(String expression);

  CommonL1Config getNewCommonL1Config();

  // Used for testing
  void addIncludePattern(String expression, boolean honorTransient);

  void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                         boolean honorVolatile);

  // Used for testing and Spring
  void addIncludeAndLockIfRequired(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                                   boolean honorVolatile, String lockExpression, ClassInfo classInfo);

  // Used for testing
  void addExcludePattern(String expression);

  boolean hasIncludeExcludePatterns();

  boolean hasIncludeExcludePattern(ClassInfo classInfo);

  void addAspectModule(String pattern, String moduleName);

  Map getAspectModules();

  void addDistributedMethodCall(DistributedMethodSpec dms);

  Portability getPortability();

  void removeSpec(String className);

  String getLogicalExtendingClassName(String className);

  void addUserDefinedBootSpec(String className, TransparencyClassSpec spec);

  void addInstrumentationDescriptor(InstrumentedClass classDesc);

  /**
   * If an adapter with the same name was already present, this new one will not be added, and the operation will simply
   * return as a no-op.
   */
  void addCustomAdapter(String name, ClassAdapterFactory adapterFactory);

  boolean hasCustomAdapters(ClassInfo classInfo);

  Collection<ClassAdapterFactory> getCustomAdapters(ClassInfo classInfo);

  public ReconnectConfig getL1ReconnectProperties() throws ConfigurationSetupException;

  public void validateGroupInfo() throws ConfigurationSetupException;

  boolean useResolveLockWhenClearing(Class clazz);

  /**
   * Add class adapters based on configuration that are present on the class
   * 
   * @return {@code true} when custom adapters were added; or {@code false} otherwise
   */
  boolean addClassConfigBasedAdapters(ClassInfo classInfo);

  L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException;

  Collection<ClassAdapterFactory> getAfterDSOAdapters(ClassInfo classInfo);
}
