/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config;

import com.tc.config.schema.CommonL1Config;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.net.core.SecurityInfo;
import com.tc.object.Portability;
import com.tc.object.config.schema.DSORuntimeLoggingOptions;
import com.tc.object.config.schema.DSORuntimeOutputOptions;
import com.tc.properties.ReconnectConfig;
import com.tc.security.PwProvider;

import java.util.Iterator;
import java.util.Map;

/**
 * Knows how to interpret the DSO client config and tell you things like whether a class is portable. This interface
 * extends DSOApplicationConfig which is a much simpler interface suitable for manipulating the config from the
 * perspective of generating a configuration file.
 */
public interface DSOClientConfigHelper extends DSOMBeanConfig, ModuleConfiguration {
  String[] processArguments();

  String rawConfigText();

  boolean isLogical(String theClass);

  TransparencyClassSpec[] getAllSpecs();

  Iterator getAllUserDefinedBootSpecs();

  Class getChangeApplicator(Class clazz);

  @Override
  boolean addTunneledMBeanDomain(String tunneledMBeanDomain);

  // HACK: is also in IStandardDSOClientConfigHelper
  TransparencyClassSpec getOrCreateSpec(String className);

  // HACK: is also in IStandardDSOClientConfigHelper
  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  TransparencyClassSpec getSpec(String className);

  DSORuntimeLoggingOptions runtimeLoggingOptions();

  DSORuntimeOutputOptions runtimeOutputOptions();

  int getFaultCount();

  @Override
  void addWriteAutolock(String methodPattern);

  void addWriteAutolock(String methodPattern, String lockContextInfo);

  void addSynchronousWriteAutolock(String methodPattern);

  void addLock(String methodPattern, LockDefinition lockDefinition);

  @Override
  void addReadAutolock(String methodPattern);

  void addAutolock(String methodPattern, ConfigLockLevel type);

  void addAutolock(String methodPattern, ConfigLockLevel type, String configurationText);

  void addReadAutoSynchronize(String methodPattern);

  void addWriteAutoSynchronize(String methodPattern);

  void setFaultCount(int count);

  void addTransient(String className, String fieldName);

  String getPreCreateMethodIfDefined(String className);

  String getPostCreateMethodIfDefined(String className);

  boolean isUseNonDefaultConstructor(Class clazz);

  CommonL1Config getNewCommonL1Config();

  void addAspectModule(String pattern, String moduleName);

  Map getAspectModules();

  void addDistributedMethodCall(DistributedMethodSpec dms);

  Portability getPortability();

  void removeSpec(String className);

  String getLogicalExtendingClassName(String className);

  void addUserDefinedBootSpec(String className, TransparencyClassSpec spec);

  public ReconnectConfig getL1ReconnectProperties(final PwProvider pwProvider) throws ConfigurationSetupException;

  public void validateGroupInfo(final PwProvider pwProvider) throws ConfigurationSetupException;

  boolean useResolveLockWhenClearing(Class clazz);

  L1ConfigurationSetupManager reloadServersConfiguration() throws ConfigurationSetupException;

  SecurityInfo getSecurityInfo();
}
