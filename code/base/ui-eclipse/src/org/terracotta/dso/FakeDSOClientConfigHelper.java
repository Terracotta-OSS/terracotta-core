/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.dso;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.config.schema.NewCommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.exception.ImplementMe;
import com.tc.object.Portability;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.config.ClassReplacementMapping;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOSpringConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.Lock;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.Root;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.TransparencyClassSpecImpl;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.ReconnectConfig;
import com.terracottatech.config.Modules;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class FakeDSOClientConfigHelper implements StandardDSOClientConfigHelper, DSOClientConfigHelper {

  public String rawConfigText() {
    return null;
  }

  public String[] getMissingRootDeclarations(ClassInfo classInfo) {
    return new String[0];
  }

  public void addAutoLockExcludePattern(String expression) {
    /**/
  }

  public void addAutolock(String methodPattern, ConfigLockLevel type) {
    /**/
  }

  public void addClassReplacement(String originalClassName, String replacementClassName, URL replacementResource) {
    /**/
  }

  public void addClassResource(String className, URL resource) {
    /**/
  }

  public void addCustomAdapter(String name, ClassAdapterFactory adapterFactory) {
    /**/
  }

  public void addIncludePattern(String classPattern) {
    /**/
  }

  public void addIncludePattern(String classname, boolean honorTransient) {
    /**/
  }

  public void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad, boolean honorVolatile) {
    /**/
  }

  public void addLock(String methodPattern, LockDefinition lockDefinition) {
    /**/
  }

  public void addPermanentExcludePattern(String pattern) {
    /**/
  }

  public void addReadAutolock(String methodPattern) {
    /**/
  }

  public void addRoot(String rootName, String rootFieldName) {
    /**/
  }

  public void addWriteAutolock(String methodPattern) {
    /**/
  }

  public void allowCGLIBInstrumentation() {
    /**/
  }

  public LockDefinition createLockDefinition(String name, ConfigLockLevel level) {
    return new LockDefinitionImpl(name, level);
  }

  public TransparencyClassSpec getOrCreateSpec(String className) {
    return new TransparencyClassSpecImpl(className, this);
  }

  public TransparencyClassSpec getOrCreateSpec(String className, String applicator) {
    return new TransparencyClassSpecImpl(className, this);
  }

  public void addApplicationName(String name) {
    /**/
  }

  public void addAspectModule(String classNamePrefix, String moduleName) {
    /**/
  }

  public void addDSOSpringConfig(DSOSpringConfigHelper config) {
    /**/
  }

  public void addDistributedMethodCall(DistributedMethodSpec dms) {
    /**/
  }

  public void addExcludePattern(String expression) {
    /**/
  }

  public void addIncludeAndLockIfRequired(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad, boolean honorVolatile, String lockExpression, ClassInfo classInfo) {
    /**/
  }

  public void addInstrumentationDescriptor(InstrumentedClass classDesc) {
    /**/
  }

  public void addRepository(String location) {
    /**/
  }

  public void addModule(String name, String version) {
    /**/
  }

  public void addReadAutoSynchronize(String methodPattern) {
    /**/
  }

  public void addRoot(Root root, boolean addSpecForClass) {
    /**/
  }

  public void addSynchronousWriteApplication(String name) {
    /**/
  }

  public void addSynchronousWriteAutolock(String methodPattern) {
    /**/
  }

  public void addTransient(String className, String fieldName) {
    /**/
  }

  public void addUserDefinedBootSpec(String className, TransparencyClassSpec spec) {
    /**/
  }

  public void addWriteAutoSynchronize(String methodPattern) {
    /**/
  }

  public TransparencyClassSpec[] getAllSpecs() {
    return null;
  }

  public Iterator getAllUserDefinedBootSpecs() {
    return null;
  }

  public Map getAspectModules() {
    return null;
  }

  public Class getChangeApplicator(Class clazz) {
    return null;
  }

  public ClassReplacementMapping getClassReplacementMapping() {
    return null;
  }

  public URL getClassResource(String className) {
    return null;
  }

  public Collection getDSOSpringConfigs() {
    return null;
  }

  public DistributedMethodSpec getDmiSpec(MemberInfo memberInfo) {
    return null;
  }

  public int getFaultCount() {
    return 0;
  }

  public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions() {
    return null;
  }

  public String getLogicalExtendingClassName(String className) {
    return null;
  }

  public Modules getModulesForInitialization() {
    return null;
  }

  public NewCommonL1Config getNewCommonL1Config() {
    return null;
  }

  public String getOnLoadMethodIfDefined(ClassInfo classInfo) {
    return null;
  }

  public String getOnLoadScriptIfDefined(ClassInfo classInfo) {
    return null;
  }

  public Portability getPortability() {
    return null;
  }

  public String getPostCreateMethodIfDefined(String className) {
    return null;
  }

  public String getPreCreateMethodIfDefined(String className) {
    return null;
  }

  public int getSessionLockType(String appName) {
    return 0;
  }

  public TransparencyClassSpec getSpec(String className) {
    return null;
  }

  public Class getTCPeerClass(Class clazz) {
    return null;
  }

  public boolean hasIncludeExcludePattern(ClassInfo classInfo) {
    return false;
  }

  public boolean hasIncludeExcludePatterns() {
    return false;
  }

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
    return null;
  }

  public boolean isCallConstructorOnLoad(ClassInfo classInfo) {
    return false;
  }

  public boolean isDSOSessions(String name) {
    return false;
  }

  public boolean isLockMethod(MemberInfo memberInfo) {
    return false;
  }

  public boolean isLogical(String theClass) {
    return false;
  }

  public boolean isNeverAdaptable(ClassInfo classInfo) {
    return false;
  }

  public boolean isPortableModuleClass(Class clazz) {
    return false;
  }

  public boolean isRoot(FieldInfo fi) {
    return false;
  }

  public boolean isRootDSOFinal(FieldInfo fi) {
    return false;
  }

  public boolean isTransient(int modifiers, ClassInfo classInfo, String field) {
    return false;
  }

  public boolean isUseNonDefaultConstructor(Class clazz) {
    return false;
  }

  public boolean isVolatile(int modifiers, ClassInfo classInfo, String field) {
    return false;
  }

  public LockDefinition[] lockDefinitionsFor(MemberInfo memberInfo) {
    return null;
  }

  public boolean matches(Lock lock, MemberInfo methodInfo) {
    return false;
  }

  public boolean matches(String expression, MemberInfo methodInfo) {
    return false;
  }

  public boolean removeCustomAdapter(String name) {
    return false;
  }

  public void removeSpec(String className) {
    /**/
  }

  public String rootNameFor(FieldInfo fi) {
    return null;
  }

  public DSORuntimeLoggingOptions runtimeLoggingOptions() {
    return null;
  }

  public DSORuntimeOutputOptions runtimeOutputOptions() {
    return null;
  }

  public void setFaultCount(int count) {
    /**/
  }

  public void setModuleSpecs(ModuleSpec[] pluginSpecs) {
    /**/
  }

  public boolean shouldBeAdapted(ClassInfo classInfo) {
    return false;
  }

  public void verifyBootJarContents(File bjf) {
    /**/
  }

  public void writeTo(DSOApplicationConfigBuilder appConfigBuilder) {
    /**/
  }

  public TransparencyClassAdapter createDsoClassAdapterFor(ClassVisitor writer, ClassInfo classInfo,
                                                           InstrumentationLogger lgr, ClassLoader caller,
                                                           final boolean forcePortable, boolean honorTransient) {
    return null;
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller) {
    return null;
  }

  public ClassAdapter createClassAdapterFor(ClassWriter writer, ClassInfo classInfo, InstrumentationLogger lgr,
                                            ClassLoader caller, boolean disableSuperClassTypeChecking) {
    return null;
  }

  public void addCustomAdapter(String name, String factoryName) {
    /**/
  }

  public void addNonportablePattern(String pattern) {
    /**/
  }

  public ClassAdapterFactory getCustomAdapter(ClassInfo classInfo) {
    return null;
  }

  public boolean hasCustomAdapter(ClassInfo classInfo) {
    return false;
  }

  public boolean reflectionEnabled() {
    return false;
  }

  public void addAutolock(String methodPattern, ConfigLockLevel type, String configurationText) {
    //
  }

  public void addWriteAutolock(String methodPattern, String lockContextInfo) {
    //
  }

  public ReconnectConfig getL1ReconnectProperties() {
    throw new ImplementMe();
  }

  public boolean useResolveLockWhenClearing(Class clazz) {
    return true;
  }

}
