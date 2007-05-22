/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.object.Portability;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.terracottatech.config.Modules;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Knows how to interpret the DSO client config and tell you things like whether a class is portable. This interface
 * extends DSOApplicationConfig which is a much simpler interface suitable for manipulating the config from the
 * perspective of generating a configuration file.
 */
public interface DSOClientConfigHelper extends DSOApplicationConfig {

  boolean shouldBeAdapted(ClassInfo classInfo);

  boolean isNeverAdaptable(ClassInfo classInfo);

  boolean isLogical(String theClass);

  DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions();

  void verifyBootJarContents() throws IncompleteBootJarException, UnverifiedBootJarException;

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

  boolean isPortableModuleClass(Class clazz);

  void setModuleSpecs(ModuleSpec[] pluginSpecs);

  TransparencyClassSpec getOrCreateSpec(String className);

  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo);

  boolean isRoot(String className, String fieldName);

  boolean isRootDSOFinal(String className, String fieldName, boolean isPrimitive);

  boolean isTransient(int modifiers, ClassInfo classInfo, String field);

  boolean isVolatile(int modifiers, ClassInfo classInfo, String field);

  String rootNameFor(String className, String fieldName);

  boolean isLockMethod(MemberInfo memberInfo);

  DistributedMethodSpec getDmiSpec(MemberInfo memberInfo);

  TransparencyClassSpec getSpec(String className);

  boolean isDSOSessions(String name);

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

  DSOInstrumentationLoggingOptions instrumentationLoggingOptions();

  int getFaultCount();

  void addWriteAutolock(String methodPattern);

  void addSynchronousWriteAutolock(String methodPattern);

  void addLock(String methodPattern, LockDefinition lockDefinition);

  void addReadAutolock(String methodPattern);

  void addAutolock(String methodPattern, ConfigLockLevel type);
  
  void addAutoSynchronize(String methodPattern, ConfigLockLevel type);
  
  void addWriteAutoSynchronize(String methodPattern);

  void setFaultCount(int count);

  void addRoot(String className, String fieldName, String rootName, boolean addSpecForClass);

  void addRoot(String className, String fieldName, String rootName, boolean dsoFinal, boolean addSpecForClass);

  boolean matches(final Lock lock, final MemberInfo methodInfo);

  boolean matches(final String expression, final MemberInfo methodInfo);

  void addTransient(String className, String fieldName);

  String getOnLoadScriptIfDefined(ClassInfo classInfo);
  
  String getPreCreateMethodIfDefined(String className);

  String getPostCreateMethodIfDefined(String className);

  String getOnLoadMethodIfDefined(ClassInfo classInfo);

  boolean isUseNonDefaultConstructor(Class clazz);

  void addIncludePattern(String expression);

  NewCommonL1Config getNewCommonL1Config();

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

  void addDSOSpringConfig(DSOSpringConfigHelper config);

  Collection getDSOSpringConfigs();

  void addDistributedMethodCall(DistributedMethodSpec dms);

  Portability getPortability();

  void removeSpec(String className);

  String getLogicalExtendingClassName(String className);

  void addUserDefinedBootSpec(String className, TransparencyClassSpec spec);

  void addApplicationName(String name);

  void addSynchronousWriteApplication(String name);

  void addInstrumentationDescriptor(InstrumentedClass classDesc);

  Modules getModulesForInitialization();

  void addNewModule(String name, String version);

  boolean removeCustomAdapter(String name);

  /**
   * @return true if the adapter was added (ie. it was not already present)
   */
  boolean addCustomAdapter(String name, ClassAdapterFactory adapterFactory);

  int getSessionLockType(String appName);

  Class getTCPeerClass(Class clazz);

}
