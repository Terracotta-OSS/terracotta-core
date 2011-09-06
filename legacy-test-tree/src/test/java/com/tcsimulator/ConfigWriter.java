/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcsimulator;

import com.tc.config.schema.builder.DSOApplicationConfigBuilder;
import com.tc.config.schema.test.ConfigBuilderFactoryImpl;
import com.tc.config.schema.test.L1ConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.SystemConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOApplicationConfig;
import com.tc.object.config.DSOApplicationConfigImpl;
import com.tc.simulator.distrunner.ArgParser;
import com.tcsimulator.distrunner.ServerSpec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

public final class ConfigWriter {

  private static final String LICENSE_FILENAME = "license.lic";

  private final Collection    classesToVisit;
  private final ServerSpec    sSpec;
  private final File          licenseFile;
  private final File          serverHome;
  private final File          configFile;

  public ConfigWriter(ServerSpec sSpec, Collection classesToVisit, Sandbox sandbox) {
    this.sSpec = sSpec;
    this.classesToVisit = classesToVisit;
    this.serverHome = sandbox.getServerHome();
    this.configFile = sandbox.getConfigFile();

    this.licenseFile = new File(sSpec.getTestHome(), LICENSE_FILENAME);
    if (!licenseFile.exists()) { throw new RuntimeException(
                                                            "No license file: "
                                                                + licenseFile
                                                                + ".  Try this: cp src/test.data/licenses/license-for-tests.lic "
                                                                + licenseFile); }
  }

  public void writeConfigFile() throws IOException {
    String fileSeparator = System.getProperty("file.separator");

    TerracottaConfigBuilder configBuilder = TerracottaConfigBuilder.newMinimalInstance();

    SystemConfigBuilder system = configBuilder.getSystem();
    system.setConfigurationModel(SystemConfigBuilder.CONFIG_MODEL_PRODUCTION);

    L1ConfigBuilder l1Builder = L1ConfigBuilder.newMinimalInstance();
    l1Builder.setLogs("client-logs-%i");
    configBuilder.setClient(l1Builder);

    L2ConfigBuilder l2Builder = L2ConfigBuilder.newMinimalInstance();
    l2Builder.setName(sSpec.getHostName());
    l2Builder.setData(this.serverHome + fileSeparator + "server-data");
    l2Builder.setLogs(this.serverHome + fileSeparator + "server-logs");
    int undefined = ArgParser.getUndefinedNumber();
    if (sSpec.getJmxPort() != undefined) {
      l2Builder.setJMXPort(sSpec.getJmxPort());
    }
    if (sSpec.getDsoPort() != undefined) {
      l2Builder.setDSOPort(sSpec.getDsoPort());
    }
    configBuilder.getServers().setL2s(new L2ConfigBuilder[] { l2Builder });

    DSOApplicationConfigBuilder appConfigBuilder = configBuilder.getApplication().getDSO();

    DSOApplicationConfig cfg = new DSOApplicationConfigImpl(new ConfigBuilderFactoryImpl());

    // NOTE: am I missing something? Why would you instrument/autolock everything then visit each app config to dicide what to instrument
    //cfg.addWriteAutolock("* *..*.*(..)");
    //cfg.addIncludePattern("*..*");

    ConfigVisitor configVisitor = new ConfigVisitor();
    for (Iterator i = classesToVisit.iterator(); i.hasNext();) {
      configVisitor.visitDSOApplicationConfig(cfg, (Class) i.next());
    }

    cfg.writeTo(appConfigBuilder);

    FileWriter cfgOut = new FileWriter(configFile);
    System.out.println("*******************************************************************************************");
    System.out.println(configBuilder.toString());
    System.out.println("*******************************************************************************************");
    cfgOut.write(configBuilder.toString());
    cfgOut.flush();
    cfgOut.close();
  }

}