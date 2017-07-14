/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.terracotta.config.Server;
import org.terracotta.config.Servers;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.setup.BaseConfigurationSetupManager;
import com.tc.config.schema.setup.ConfigurationCreator;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.ConfigurationSpec;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.setup.StandardXMLFileConfigurationCreator;
import com.tc.object.config.schema.L2ConfigObject;
import com.tc.test.TCTestCase;
import com.tc.text.StringUtils;
import com.tc.util.Assert;

import org.terracotta.config.TCConfigDefaults;
import org.terracotta.config.TcConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class BaseConfigurationSetupManagerTest extends TCTestCase {

  private static final String DEFAULT_CONFIG_SPEC = "tc-config.xml";
  private static final String CONFIG_SPEC_ARGUMENT_NAME = "config";
  private static final String CONFIG_FILE_PROPERTY_NAME = "tc.config";
  private static final String DEFAULT_CONFIG_PATH = "default-config.xml";
  private static final String DEFAULT_CONFIG_URI = "resource:///"
                                                   + BaseConfigurationSetupManagerTest.class.getPackage().getName().replace('.', '/') + "/"
                                                   + DEFAULT_CONFIG_PATH;
  private File tcConfig = null;

  public void testServerDefaults1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>" + "</server>" + "</servers>"
                    + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();
    Server server = servers.getServer().get(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getValue(), server.getName());

    Assert.assertEquals(TCConfigDefaults.TSA_PORT, server.getTsaPort().getValue());
    Assert.assertEquals(server.getBind(), server.getTsaPort().getBind());
    Assert.assertEquals(TCConfigDefaults.GROUP_PORT, server.getTsaGroupPort().getValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());
  }

  public void testServerDefaults2() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>" + "<tsa-port>8513</tsa-port>"
                    + "</server>" + "</servers>" + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Server server = servers.getServer().get(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getValue(), server.getName());

    int tsaPort = 8513;

    Assert.assertEquals(tsaPort, server.getTsaPort().getValue());
    Assert.assertEquals(server.getBind(), server.getTsaPort().getBind());

    int tempGroupPort = tsaPort + L2ConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
    int defaultGroupPort = ((tempGroupPort <= L2ConfigObject.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % L2ConfigObject.MAX_PORTNUMBER) + L2ConfigObject.MIN_PORTNUMBER);

    Assert.assertEquals(defaultGroupPort, server.getTsaGroupPort().getValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());

  }

  public void testServerDefaults3() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>" + "</server>" + "</servers>" + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    Server server = servers.getServer().get(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getValue(), server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    int tempGroupPort = tsaPort + L2ConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
    int defaultGroupPort = ((tempGroupPort <= L2ConfigObject.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % L2ConfigObject.MAX_PORTNUMBER) + L2ConfigObject.MIN_PORTNUMBER);


    Assert.assertEquals(defaultGroupPort, server.getTsaGroupPort().getValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());

  }

  public void testServerDefaults4() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>" + "</servers>" + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    Server server = servers.getServer().get(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getValue(), server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());


    int tsaGroupPort = 7513;
    String tsaGroupBind = "5.6.7.8";
    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());
  }

  public void testServerDefaults5() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>"
                    + "<server host=\"testHost2\" name=\"server2\" bind=\"4.5.6.7\">" + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>"
                    + "</servers>" + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(2, servers.getServer().size());
    Server server = servers.getServer().get(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getValue(), server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    int tsaGroupPort = 7513;
    String tsaGroupBind = "5.6.7.8";
    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());

    server = servers.getServer().get(1);
    String host = "testHost2";
    String name = "server2";
    String bind = "4.5.6.7";

    Assert.assertEquals(host, server.getHost());
    Assert.assertEquals(bind, server.getBind());
    Assert.assertEquals(name, server.getName());

    Assert.assertEquals(tsaPort, server.getTsaPort().getValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());

  }

  public void testServerDirectoryDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>" + " </server>" + "</servers>"
                    + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    Server server = servers.getServer().get(0);

    TcConfiguration tcConfiguration = configSetupMgr.tcConfigurationRepository();

//    System.out.println("XXXXXX  "+System.getProperty("user.dir"));
//    Assert.assertEquals(new File(this.tcConfig.getParent() + File.separator + "data").getPath(),
//        tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getData());
//    Assert.assertEquals(new File(this.tcConfig.getParent() + File.separator + "data-backup")
//        .getPath(), tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getDataBackup());

    //Changing default behaviour of the locations
    String defaultPath = new File(this.tcConfig.getParent() + File.separator + "logs" + File.separator +
                                InetAddress.getLocalHost().getHostName() + "-" + TCConfigDefaults.TSA_PORT).getAbsolutePath();
    Assert.assertEquals(defaultPath, server.getLogs());
  }

  public void testServerDirectoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<logs>xyz/abc/451</logs>" + "</server>"
                    + "</servers>" + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    Server server = servers.getServer().get(0);

    TcConfiguration tcConfiguration = configSetupMgr.tcConfigurationRepository();
//  not valid with current config.  see config project
//    Assert.assertEquals("abc" + File.separator + "xyz"
//                                 + File.separator + "123", tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getData());
//    Assert.assertEquals(("xyz" + File.separator + "abc"
//                                 + File.separator + "451"), tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getLogs());
//
//    if (Os.isWindows()) {
//      // for windows box
//      Assert.assertEquals(("qrt" + File.separator
//                                   + "opt" + File.separator + "pqr"), tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getDataBackup());
//    } else {
//      Assert.assertEquals("/qrt/opt/pqr", tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getDataBackup());
//    }
  }

  public void testServerSubsitutedDirectoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<logs>%i</logs>" + "</server>" + "</servers>"
                    + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    Server server = servers.getServer().get(0);

    TcConfiguration tcConfiguration = configSetupMgr.tcConfigurationRepository();
//  not valid with current config.  see config project
//    Assert.assertEquals(InetAddress.getLocalHost().getHostName(), tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getData());
//    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getLogs());
//    Assert.assertEquals(System.getProperty("user.home"), tcConfiguration.getPlatformConfiguration().getServers().getServer().get(0).getDataBackup());
  }

  public void testDefaultDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc-config xmlns=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>" + "</server>" + "</servers>"
                    + "</tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = configSetupMgr.serversBeanRepository();

    Assert.assertEquals(1, servers.getServer().size());
    TcConfiguration tcConfiguration = configSetupMgr.tcConfigurationRepository();


//    Assert.assertFalse(storageConfig.getRestartable().isEnabled());
    Assert.assertEquals(120, (int) servers.getClientReconnectWindow());
  }

  private BaseConfigurationSetupManager initializeAndGetBaseTVSConfigSetupManager(boolean isClient) throws ConfigurationSetupException {
    String[] args = new String[] { "-f", tcConfig.getAbsolutePath() };

    String effectiveConfigSpec = getEffectiveConfigSpec(System.getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME),
                                                        parseDefaultCommandLine(args,
                                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2),
                                                        StandardConfigurationSetupManagerFactory.ConfigMode.L2);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null" : "'" + cwdAsString
                                                                                                                        + "'") + ".)"); }
    ConfigurationSpec configurationSpec = new ConfigurationSpec(
                                                                effectiveConfigSpec,
                                                                System
                                                                    .getProperty(ConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new File(cwdAsString));

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    BaseConfigurationSetupManager configSetupMgr = new BaseConfigurationSetupManager(configurationCreator);
    configSetupMgr.runConfigurationCreator(getClass().getClassLoader());

    return configSetupMgr;
  }

  @Override
  protected File getTempFile(String fileName) throws IOException {
    return getTempDirectoryHelper().getFile(fileName);
  }

  private synchronized void writeConfigFile(String fileContents) {
    try {
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(fileContents, out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  private String getEffectiveConfigSpec(String configSpec, CommandLine commandLine, ConfigMode configMode)
      throws ConfigurationSetupException {

    String configFileOnCommandLine = null;
    String effectiveConfigSpec;

    configFileOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('f'));
    effectiveConfigSpec = StringUtils.trimToNull(configFileOnCommandLine != null ? configFileOnCommandLine : configSpec);

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      File localConfig = new File(System.getProperty("user.dir"), DEFAULT_CONFIG_SPEC);

      if (localConfig.exists()) {
        effectiveConfigSpec = localConfig.getAbsolutePath();
      } else if (configMode == ConfigMode.L2) {
        effectiveConfigSpec = DEFAULT_CONFIG_URI;
      }
    }

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      // formatting
      throw new ConfigurationSetupException("You must specify the location of the Terracotta "
                                            + "configuration file for this process, using the " + "'" + CONFIG_FILE_PROPERTY_NAME
                                            + "' system property.");
    }

    return effectiveConfigSpec;
  }

  private static CommandLine parseDefaultCommandLine(String[] args, ConfigMode configMode) throws ConfigurationSetupException {
    try {
      if (args == null || args.length == 0) {
        return new PosixParser().parse(new Options(), new String[0]);
      } else {
        Options options = createOptions(configMode);

        return new PosixParser().parse(options, args);
      }
    } catch (ParseException pe) {
      throw new ConfigurationSetupException(pe.getLocalizedMessage(), pe);
    }
  }

  private static Options createOptions(ConfigMode configMode) {
    Options options = new Options();

    Option configFileOption = new Option("f", CONFIG_SPEC_ARGUMENT_NAME, true,
                                         "the configuration file to use, specified as a file path or URL");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);

    if (configMode == ConfigMode.L2) {
      configFileOption.setRequired(false);
      options.addOption(configFileOption);

      Option l2NameOption = new Option("n", "name", true, "the name of this L2; defaults to the host name");
      l2NameOption.setRequired(false);
      l2NameOption.setArgName("l2-name");
      options.addOption(l2NameOption);
    } else {
      configFileOption.setRequired(true);
      options.addOption(configFileOption);
    }

    return options;
  }
}
