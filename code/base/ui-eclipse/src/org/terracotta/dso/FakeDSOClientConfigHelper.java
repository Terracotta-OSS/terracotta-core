/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso;

import org.osgi.framework.Bundle;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.ClassWriter;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.FieldInfo;
import com.tc.aspectwerkz.reflect.MemberInfo;
import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.exception.ImplementMe;
import com.tc.object.Portability;
import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.bytecode.SessionConfiguration;
import com.tc.object.bytecode.TransparencyClassAdapter;
import com.tc.object.config.ClassReplacementMapping;
import com.tc.object.config.ClassReplacementTest;
import com.tc.object.config.ConfigLockLevel;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DistributedMethodSpec;
import com.tc.object.config.Lock;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.LockDefinitionImpl;
import com.tc.object.config.MBeanSpec;
import com.tc.object.config.ModuleSpec;
import com.tc.object.config.Root;
import com.tc.object.config.SRASpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TimCapability;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.config.TransparencyClassSpecImpl;
import com.tc.object.config.schema.DSOInstrumentationLoggingOptions;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.object.config.schema.InstrumentedClass;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.properties.ReconnectConfig;
import com.tc.util.UUID;
import com.terracottatech.config.Modules;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FakeDSOClientConfigHelper implements StandardDSOClientConfigHelper, DSOClientConfigHelper {
  private final Map<Bundle, URL> bundleURLs = new ConcurrentHashMap<Bundle, URL>();

  public String rawConfigText() {
    return null;
  }

  public String[] getMissingRootDeclarations(final ClassInfo classInfo) {
    return new String[0];
  }

  public void addAutoLockExcludePattern(final String expression) {
    /**/
  }

  public void addAutolock(final String methodPattern, final ConfigLockLevel type) {
    /**/
  }

  public void addClassReplacement(final String originalClassName, final String replacementClassName,
                                  final URL replacementResource) {
    /**/
  }

  public void addClassReplacement(final String originalClassName, final String replacementClassName, final URL url,
                                  final ClassReplacementTest test) {
    /**/
  }

  public void addClassResource(final String className, final URL resource, final boolean targetSystemLoader) {
    /**/
  }

  public void addCustomAdapter(final String name, final ClassAdapterFactory adapterFactory) {
    /**/
  }

  public void addIncludePattern(final String classPattern) {
    /**/
  }

  public void addIncludePattern(final String classname, final boolean honorTransient) {
    /**/
  }

  public void addIncludePattern(final String expression, final boolean honorTransient,
                                final boolean oldStyleCallConstructorOnLoad, final boolean honorVolatile) {
    /**/
  }

  public void addIncludePattern(String expression, boolean honorTransient, String methodToCallOnLoad,
                                boolean honorVolatile) {
    /**/
  }

  public void addLock(final String methodPattern, final LockDefinition lockDefinition) {
    /**/
  }

  public void addPermanentExcludePattern(final String pattern) {
    /**/
  }

  public void addReadAutolock(final String methodPattern) {
    /**/
  }

  public void addRoot(final String rootName, final String rootFieldName) {
    /**/
  }

  public void addWriteAutolock(final String methodPattern) {
    /**/
  }

  public LockDefinition createLockDefinition(final String name, final ConfigLockLevel level) {
    return new LockDefinitionImpl(name, level);
  }

  public TransparencyClassSpec getOrCreateSpec(final String className) {
    return new TransparencyClassSpecImpl(className, this);
  }

  public TransparencyClassSpec getOrCreateSpec(final String className, final String applicator) {
    return new TransparencyClassSpecImpl(className, this);
  }

  public void addAspectModule(final String classNamePrefix, final String moduleName) {
    /**/
  }

  public void addDistributedMethodCall(final DistributedMethodSpec dms) {
    /**/
  }

  public void addExcludePattern(final String expression) {
    /**/
  }

  public void addIncludeAndLockIfRequired(final String expression, final boolean honorTransient,
                                          final boolean oldStyleCallConstructorOnLoad, final boolean honorVolatile,
                                          final String lockExpression, final ClassInfo classInfo) {
    /**/
  }

  public void addInstrumentationDescriptor(final InstrumentedClass classDesc) {
    /**/
  }

  public void addRepository(final String location) {
    /**/
  }

  public void addModule(final String name, final String version) {
    /**/
  }

  public void addModule(final String groupId, final String name, final String version) {
    /**/
  }

  public void addReadAutoSynchronize(final String methodPattern) {
    /**/
  }

  public void addRoot(final Root root, final boolean addSpecForClass) {
    /**/
  }

  public void addSynchronousWriteAutolock(final String methodPattern) {
    /**/
  }

  public void addTransient(final String className, final String fieldName) {
    /**/
  }

  public void addUserDefinedBootSpec(final String className, final TransparencyClassSpec spec) {
    /**/
  }

  public void addWriteAutoSynchronize(final String methodPattern) {
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

  public Class getChangeApplicator(final Class clazz) {
    return null;
  }

  public ClassReplacementMapping getClassReplacementMapping() {
    return null;
  }

  public URL getClassResource(final String className, final ClassLoader loader, final boolean hideSystemResources) {
    return null;
  }

  public DistributedMethodSpec getDmiSpec(final MemberInfo memberInfo) {
    return null;
  }

  public int getFaultCount() {
    return 0;
  }

  public DSOInstrumentationLoggingOptions getInstrumentationLoggingOptions() {
    return null;
  }

  public String getLogicalExtendingClassName(final String className) {
    return null;
  }

  public Modules getModulesForInitialization() {
    return null;
  }

  public CommonL1Config getNewCommonL1Config() {
    return null;
  }

  public String getOnLoadMethodIfDefined(final ClassInfo classInfo) {
    return null;
  }

  public String getOnLoadScriptIfDefined(final ClassInfo classInfo) {
    return null;
  }

  public Portability getPortability() {
    return null;
  }

  public String getPostCreateMethodIfDefined(final String className) {
    return null;
  }

  public String getPreCreateMethodIfDefined(final String className) {
    return null;
  }

  public TransparencyClassSpec getSpec(final String className) {
    return null;
  }

  public Class getTCPeerClass(final Class clazz) {
    return null;
  }

  public boolean hasIncludeExcludePattern(final ClassInfo classInfo) {
    return false;
  }

  public boolean hasIncludeExcludePatterns() {
    return false;
  }

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
    return null;
  }

  public boolean isCallConstructorOnLoad(final ClassInfo classInfo) {
    return false;
  }

  public boolean isLockMethod(final MemberInfo memberInfo) {
    return false;
  }

  public boolean isLogical(final String theClass) {
    return false;
  }

  public boolean isNeverAdaptable(final ClassInfo classInfo) {
    return false;
  }

  public boolean isPortableModuleClass(final Class clazz) {
    return false;
  }

  public boolean isRoot(final FieldInfo fi) {
    return false;
  }

  public boolean isRootDSOFinal(final FieldInfo fi) {
    return false;
  }

  public boolean isTransient(final int modifiers, final ClassInfo classInfo, final String field) {
    return false;
  }

  public boolean isUseNonDefaultConstructor(final Class clazz) {
    return false;
  }

  public boolean isVolatile(final int modifiers, final ClassInfo classInfo, final String field) {
    return false;
  }

  public LockDefinition[] lockDefinitionsFor(final MemberInfo memberInfo) {
    return null;
  }

  public boolean matches(final Lock lock, final MemberInfo methodInfo) {
    return false;
  }

  public boolean matches(final String expression, final MemberInfo methodInfo) {
    return false;
  }

  public void removeSpec(final String className) {
    /**/
  }

  public String rootNameFor(final FieldInfo fi) {
    return null;
  }

  public DSORuntimeLoggingOptions runtimeLoggingOptions() {
    return null;
  }

  public DSORuntimeOutputOptions runtimeOutputOptions() {
    return null;
  }

  public void setFaultCount(final int count) {
    /**/
  }

  public void addModuleSpec(final ModuleSpec moduleSpec) {
    /**/
  }

  public void setMBeanSpecs(final MBeanSpec[] mbeanSpecs) {
    /**/
  }

  public MBeanSpec[] getMBeanSpecs() {
    return null;
  }

  public SRASpec[] getSRASpecs() {
    return null;
  }

  public void setSRASpecs(final SRASpec[] sraSpecs) {
    throw new ImplementMe();
  }

  public boolean addTunneledMBeanDomain(String tunneledMBeanDomain) {
    throw new ImplementMe();
  }

  public boolean shouldBeAdapted(final ClassInfo classInfo) {
    return false;
  }

  public void verifyBootJarContents(final File bjf) {
    /**/
  }

  public void writeTo(final DSOApplicationConfigBuilder appConfigBuilder) {
    /**/
  }

  public TransparencyClassAdapter createDsoClassAdapterFor(final ClassVisitor writer, final ClassInfo classInfo,
                                                           final InstrumentationLogger lgr, final ClassLoader caller,
                                                           final boolean forcePortable, final boolean honorTransient) {
    return null;
  }

  public ClassAdapter createClassAdapterFor(final ClassWriter writer, final ClassInfo classInfo,
                                            final InstrumentationLogger lgr, final ClassLoader caller) {
    return null;
  }

  public ClassAdapter createClassAdapterFor(final ClassWriter writer, final ClassInfo classInfo,
                                            final InstrumentationLogger lgr, final ClassLoader caller,
                                            final boolean disableSuperClassTypeChecking) {
    return null;
  }

  public void addNonportablePattern(final String pattern) {
    /**/
  }

  public Collection<ClassAdapterFactory> getCustomAdapters(final ClassInfo classInfo) {
    return null;
  }

  public boolean hasCustomAdapters(final ClassInfo classInfo) {
    return false;
  }

  public boolean reflectionEnabled() {
    return false;
  }

  public void addAutolock(final String methodPattern, final ConfigLockLevel type, final String configurationText) {
    //
  }

  public void addWriteAutolock(final String methodPattern, final String lockContextInfo) {
    //
  }

  public ReconnectConfig getL1ReconnectProperties() {
    throw new ImplementMe();
  }

  public boolean useResolveLockWhenClearing(final Class clazz) {
    return true;
  }

  public void validateGroupInfo() {
    //
  }

  public boolean addClassConfigBasedAdapters(final ClassInfo classInfo) {
    return false;
  }

  public void enableCapability(final TimCapability cap) {
    //
  }

  public void validateSessionConfig() {
    //
  }

  public void addInjectedField(final String className, final String fieldName, final String Type) {
    //

  }

  public String getInjectedFieldType(final ClassInfo classInfo, final String field) {
    return null;
  }

  public void addToAppGroup(final String appGroup, final String[] namedClassloaders, final String[] webAppNames) {
    //
  }

  public String getAppGroup(final String classLoaderName, final String appName) {
    return null;
  }

  public boolean isInjectedField(final String className, final String fieldName) {
    return false;
  }

  public boolean hasOnLoadInjection(final ClassInfo classInfo) {
    return false;
  }

  public boolean hasBootJar() {
    return false;
  }

  public void recordBundleURLs(final Map<Bundle, URL> toAdd) {
    this.bundleURLs.putAll(toAdd);
  }

  public URL getBundleURL(final Bundle bundle) {
    return this.bundleURLs.get(bundle);
  }

  public UUID getUUID() {
    return null;
  }

  public String[] getTunneledDomains() {
    return null;
  }

  public SessionConfiguration getSessionConfiguration(final String appName) {
    return null;
  }

  public void addWebApplication(final String pattern, final SessionConfiguration config) {
    //
  }

  public L1ConfigurationSetupManager reloadServersConfiguration() {
    return null;
  }

  public String[] processArguments() {
    return null;
  }

  public Collection<ClassAdapterFactory> getAfterDSOAdapters(ClassInfo classInfo) {
    return null;
  }
}
