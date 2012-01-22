/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.config.schema;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlString;

import com.tc.config.schema.BaseConfigObject;
import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.util.Assert;
import com.terracottatech.config.Client;
import com.terracottatech.config.DsoClientData;
import com.terracottatech.config.DsoClientDebugging;
import com.terracottatech.config.InstrumentationLogging;
import com.terracottatech.config.Modules;
import com.terracottatech.config.RuntimeLogging;
import com.terracottatech.config.RuntimeOutputOptions;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;

public class L1DSOConfigObject extends BaseConfigObject implements L1DSOConfig {

  public static final String                     DSO_INSTRUMENTATION_LOGGING_OPTIONS_SUB_XPATH = "";

  private final int                              faultCount;

  private final DSOInstrumentationLoggingOptions instrumentationLoggingOptions;
  private final DSORuntimeLoggingOptions         runtimeLoggingOptions;
  private final DSORuntimeOutputOptions          runtimeOutputOptions;

  public L1DSOConfigObject(ConfigContext context) {
    super(context);

    this.context.ensureRepositoryProvides(DsoClientData.class);
    DsoClientData dsoClientData = (DsoClientData) this.context.bean();

    this.faultCount = dsoClientData.getFaultCount();
    this.instrumentationLoggingOptions = new StandardDSOInstrumentationLoggingOptions(this.context);
    this.runtimeLoggingOptions = new StandardDSORuntimeLoggingOptions(this.context);
    this.runtimeOutputOptions = new StandardDSORuntimeOutputOptions(this.context);
  }

  public DSOInstrumentationLoggingOptions instrumentationLoggingOptions() {
    return this.instrumentationLoggingOptions;
  }

  public DSORuntimeLoggingOptions runtimeLoggingOptions() {
    return this.runtimeLoggingOptions;
  }

  public DSORuntimeOutputOptions runtimeOutputOptions() {
    return this.runtimeOutputOptions;
  }

  public int faultCount() {
    return faultCount;
  }

  public static void initializeClients(TcConfig config, DefaultValueProvider defaultValueProvider) throws XmlException {
    Client client;
    if (!config.isSetClients()) {
      client = config.addNewClients();
    } else {
      client = config.getClients();
    }
    initializeLogsDirectory(client, defaultValueProvider);
    initializeModules(client, defaultValueProvider);
    initiailizeDsoClient(client, defaultValueProvider);
  }

  private static void initializeLogsDirectory(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    Assert.assertNotNull(client);
    if (!client.isSetLogs()) {
      final XmlString defaultValue = (XmlString) defaultValueProvider.defaultFor(client.schemaType(), "logs");
      String substitutedString = ParameterSubstituter.substitute(defaultValue.getStringValue());

      client.setLogs(new File(substitutedString).getAbsolutePath());
    } else {
      Assert.assertNotNull(client.getLogs());
      client.setLogs(ParameterSubstituter.substitute(client.getLogs()));
    }
  }

  private static void initializeModules(Client client, DefaultValueProvider defaultValueProvider) {
    if (client != null && client.isSetModules()) {
      Modules modules = client.getModules();
      for (int i = 0; i < modules.sizeOfRepositoryArray(); i++) {
        String location = modules.getRepositoryArray(i);
        modules.setRepositoryArray(i, ParameterSubstituter.substitute(location));
      }
    }
  }

  private static void initiailizeDsoClient(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    if (!client.isSetDso()) {
      client.addNewDso();
    }

    initializeFaultCount(client, defaultValueProvider);
    initializeDebugging(client, defaultValueProvider);
  }

  private static void initializeFaultCount(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    DsoClientData dso = client.getDso();
    Assert.assertNotNull(dso);

    if (!dso.isSetFaultCount()) {
      dso.setFaultCount(getDefaultFaultCount(client, defaultValueProvider));
    }

  }

  private static void initializeDebugging(Client client, DefaultValueProvider defaultValueProvider) throws XmlException {
    DsoClientData dso = client.getDso();
    Assert.assertNotNull(dso);

    if (!dso.isSetDebugging()) {
      dso.addNewDebugging();
    }

    DsoClientDebugging debugging = dso.getDebugging();
    Assert.assertNotNull(debugging);

    initializeInstrumentationLogging(client, defaultValueProvider);
    initializeRunTimeLogging(client, defaultValueProvider);
    initailizeRunTimeOutputOptions(client, defaultValueProvider);
  }

  private static void initializeInstrumentationLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    DsoClientDebugging debugging = client.getDso().getDebugging();
    Assert.assertNotNull(debugging);

    if (!debugging.isSetInstrumentationLogging()) {
      debugging.addNewInstrumentationLogging();
    }

    InstrumentationLogging instrumentationLogging = debugging.getInstrumentationLogging();
    Assert.assertNotNull(instrumentationLogging);

    if (!instrumentationLogging.isSetClass1()) {
      instrumentationLogging.setClass1(getDefaultClassInstrumentationLogging(client, defaultValueProvider));
    }

    if (!instrumentationLogging.isSetHierarchy()) {
      instrumentationLogging.setHierarchy(getDefaultHierarchyInstrumentationLogging(client, defaultValueProvider));
    }

    if (!instrumentationLogging.isSetLocks()) {
      instrumentationLogging.setLocks(getDefaultLocksInstrumentationLoggings(client, defaultValueProvider));
    }

    if (!instrumentationLogging.isSetTransientRoot()) {
      instrumentationLogging.setTransientRoot(getDefaultTransientRootInstrumentationLogging(client,
                                                                                            defaultValueProvider));
    }

    if (!instrumentationLogging.isSetRoots()) {
      instrumentationLogging.setRoots(getDefaultRootsInstrumentationLogging(client, defaultValueProvider));
    }

    if (!instrumentationLogging.isSetDistributedMethods()) {
      instrumentationLogging
          .setDistributedMethods(getDefaultDistributedMethodInstrumentationLogging(client, defaultValueProvider));
    }
  }

  private static void initializeRunTimeLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    DsoClientDebugging debugging = client.getDso().getDebugging();
    Assert.assertNotNull(debugging);

    if (!debugging.isSetRuntimeLogging()) {
      debugging.addNewRuntimeLogging();
    }

    RuntimeLogging runtimeLogging = debugging.getRuntimeLogging();
    Assert.assertNotNull(runtimeLogging);

    if (!runtimeLogging.isSetNonPortableDump()) {
      runtimeLogging.setNonPortableDump(getDefaultNonPortableDumpRuntimeLogging(client, defaultValueProvider));
    }

    if (!runtimeLogging.isSetLockDebug()) {
      runtimeLogging.setLockDebug(getDefaultLockDebugRuntimeLogging(client, defaultValueProvider));
    }

    if (!runtimeLogging.isSetFieldChangeDebug()) {
      runtimeLogging.setFieldChangeDebug(getDefaultFieldChangeDebugRuntimeLogging(client, defaultValueProvider));
    }

    if (!runtimeLogging.isSetWaitNotifyDebug()) {
      runtimeLogging.setWaitNotifyDebug(getDefaultWaitNotifyDebugRuntimeLogging(client, defaultValueProvider));
    }

    if (!runtimeLogging.isSetDistributedMethodDebug()) {
      runtimeLogging.setDistributedMethodDebug(getDefaultDistributedMethodDebugRuntimeLogging(client,
                                                                                              defaultValueProvider));
    }

    if (!runtimeLogging.isSetNewObjectDebug()) {
      runtimeLogging.setNewObjectDebug(getDefaultNewObjectDebugRuntimeLogging(client, defaultValueProvider));
    }

    if (!runtimeLogging.isSetNamedLoaderDebug()) {
      runtimeLogging.setNamedLoaderDebug(getDefaultNamedLoaderDebugRuntimeLogging(client, defaultValueProvider));
    }
  }

  private static void initailizeRunTimeOutputOptions(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    DsoClientDebugging debugging = client.getDso().getDebugging();
    Assert.assertNotNull(debugging);

    if (!debugging.isSetRuntimeOutputOptions()) {
      debugging.addNewRuntimeOutputOptions();
    }

    RuntimeOutputOptions runtimeOutputOptions = debugging.getRuntimeOutputOptions();
    Assert.assertNotNull(runtimeOutputOptions);

    if (!runtimeOutputOptions.isSetAutoLockDetails()) {
      runtimeOutputOptions
          .setAutoLockDetails(getDefaultAutoLockDetailsRuntimeOutputOption(client, defaultValueProvider));
    }

    if (!runtimeOutputOptions.isSetCaller()) {
      runtimeOutputOptions.setCaller(getDefaultCallerRuntimeOutputOption(client, defaultValueProvider));
    }

    if (!runtimeOutputOptions.isSetFullStack()) {
      runtimeOutputOptions.setFullStack(getDefaultFullStackRuntimeOutputOption(client, defaultValueProvider));
    }

  }

  private static int getDefaultFaultCount(Client client, DefaultValueProvider defaultValueProvider) throws XmlException {
    return ((XmlInteger) defaultValueProvider.defaultFor(client.schemaType(), "dso/fault-count")).getBigIntegerValue()
        .intValue();
  }

  private static boolean getDefaultClassInstrumentationLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/class"))
        .getBooleanValue();
  }

  private static boolean getDefaultHierarchyInstrumentationLogging(Client client,
                                                                   DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/hierarchy"))
        .getBooleanValue();
  }

  private static boolean getDefaultLocksInstrumentationLoggings(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/locks"))
        .getBooleanValue();
  }

  private static boolean getDefaultTransientRootInstrumentationLogging(Client client,
                                                                       DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/transient-root"))
        .getBooleanValue();
  }

  private static boolean getDefaultRootsInstrumentationLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/roots"))
        .getBooleanValue();
  }

  private static boolean getDefaultDistributedMethodInstrumentationLogging(Client client,
                                                                           DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/instrumentation-logging/distributed-methods"))
        .getBooleanValue();
  }

  private static boolean getDefaultNonPortableDumpRuntimeLogging(Client client,
                                                                 DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/non-portable-dump"))
        .getBooleanValue();
  }

  private static boolean getDefaultLockDebugRuntimeLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/lock-debug")).getBooleanValue();
  }

  private static boolean getDefaultFieldChangeDebugRuntimeLogging(Client client,
                                                                  DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/field-change-debug"))
        .getBooleanValue();
  }

  private static boolean getDefaultWaitNotifyDebugRuntimeLogging(Client client,
                                                                 DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/wait-notify-debug"))
        .getBooleanValue();
  }

  private static boolean getDefaultDistributedMethodDebugRuntimeLogging(Client client,
                                                                        DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/distributed-method-debug"))
        .getBooleanValue();
  }

  private static boolean getDefaultNewObjectDebugRuntimeLogging(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/new-object-debug"))
        .getBooleanValue();
  }

  private static boolean getDefaultNamedLoaderDebugRuntimeLogging(Client client,
                                                                  DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-logging/named-loader-debug"))
        .getBooleanValue();
  }

  private static boolean getDefaultAutoLockDetailsRuntimeOutputOption(Client client,
                                                                      DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-output-options/auto-lock-details"))
        .getBooleanValue();
  }

  private static boolean getDefaultCallerRuntimeOutputOption(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-output-options/caller"))
        .getBooleanValue();
  }

  private static boolean getDefaultFullStackRuntimeOutputOption(Client client, DefaultValueProvider defaultValueProvider)
      throws XmlException {
    return ((XmlBoolean) defaultValueProvider.defaultFor(client.schemaType(),
                                                         "dso/debugging/runtime-output-options/full-stack"))
        .getBooleanValue();
  }

}
