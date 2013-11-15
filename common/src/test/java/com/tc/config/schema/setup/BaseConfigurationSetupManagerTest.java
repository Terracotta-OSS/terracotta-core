/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.beanfactory.TerracottaDomainConfigurationDocumentBeanFactory;
import com.tc.config.schema.defaults.SchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory.ConfigMode;
import com.tc.config.schema.utils.StandardXmlObjectComparator;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.runtime.Os;
import com.terracottatech.config.Client;
import com.terracottatech.config.MirrorGroup;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class BaseConfigurationSetupManagerTest extends TCTestCase {

  private static final String DEFAULT_CONFIG_SPEC       = "tc-config.xml";
  private static final String CONFIG_SPEC_ARGUMENT_NAME = "config";
  private static final String CONFIG_FILE_PROPERTY_NAME = "tc.config";
  private static final String DEFAULT_CONFIG_PATH       = "default-config.xml";
  private static final String DEFAULT_CONFIG_URI        = "resource:///"
                                                          + BaseConfigurationSetupManagerTest.class.getPackage()
                                                              .getName().replace('.', '/') + "/" + DEFAULT_CONFIG_PATH;
  private File                tcConfig                  = null;

  public void testServerDefaults1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getIntValue(),
                        server.getName());

    Assert.assertEquals(9510, server.getTsaPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getTsaPort().getBind());

    int tempGroupPort = 9510 + L2DSOConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
    int defaultGroupPort = ((tempGroupPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    int tempJmxPort = 9510 + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT;
    int defaultJmxPort = ((tempJmxPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());
  }

  public void testServerDefaults2() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port>8513</tsa-port>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getIntValue(),
                        server.getName());

    int tsaPort = 8513;

    Assert.assertEquals(tsaPort, server.getTsaPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getTsaPort().getBind());

    int tempGroupPort = tsaPort + L2DSOConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
    int defaultGroupPort = ((tempGroupPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    int tempJmxPort = tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT;
    int defaultJmxPort = ((tempJmxPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());

  }

  public void testServerDefaults3() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>" + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getIntValue(),
                        server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getIntValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    int tempGroupPort = tsaPort + L2DSOConfigObject.DEFAULT_GROUPPORT_OFFSET_FROM_TSAPORT;
    int defaultGroupPort = ((tempGroupPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? (tempGroupPort)
        : (tempGroupPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    int tempJmxPort = tsaPort + L2DSOConfigObject.DEFAULT_JMXPORT_OFFSET_FROM_TSAPORT;
    int defaultJmxPort = ((tempJmxPort <= L2DSOConfigObject.MAX_PORTNUMBER) ? tempJmxPort
        : (tempJmxPort % L2DSOConfigObject.MAX_PORTNUMBER) + L2DSOConfigObject.MIN_PORTNUMBER);

    Assert.assertEquals(defaultJmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getJmxPort().getBind());

    Assert.assertEquals(defaultGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(server.getBind(), server.getTsaGroupPort().getBind());

  }

  public void testServerDefaults4() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getIntValue(),
                        server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getIntValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int tsaGroupPort = 7513;
    String tsaGroupBind = "5.6.7.8";
    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());
  }

  public void testServerDefaults5() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>"
                    + "<server host=\"testHost2\" name=\"server2\" bind=\"4.5.6.7\">"
                    + "<tsa-port bind=\"1.2.3.4\">8513</tsa-port>" + "<jmx-port bind=\"4.3.2.1\">9513</jmx-port>"
                    + "<tsa-group-port bind=\"5.6.7.8\">7513</tsa-group-port>" + "</server>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(2, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress(), server.getHost());
    Assert.assertEquals("0.0.0.0", server.getBind());
    Assert.assertEquals(InetAddress.getLocalHost().getHostAddress() + ":" + server.getTsaPort().getIntValue(),
                        server.getName());

    int tsaPort = 8513;
    String tsaBind = "1.2.3.4";

    Assert.assertEquals(tsaPort, server.getTsaPort().getIntValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    int jmxPort = 9513;
    String jmxBind = "4.3.2.1";
    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    int tsaGroupPort = 7513;
    String tsaGroupBind = "5.6.7.8";
    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());

    server = servers.getMirrorGroupArray(0).getServerArray(1);
    String host = "testHost2";
    String name = "server2";
    String bind = "4.5.6.7";

    Assert.assertEquals(host, server.getHost());
    Assert.assertEquals(bind, server.getBind());
    Assert.assertEquals(name, server.getName());

    Assert.assertEquals(tsaPort, server.getTsaPort().getIntValue());
    Assert.assertEquals(tsaBind, server.getTsaPort().getBind());

    Assert.assertEquals(jmxPort, server.getJmxPort().getIntValue());
    Assert.assertEquals(jmxBind, server.getJmxPort().getBind());

    Assert.assertEquals(tsaGroupPort, server.getTsaGroupPort().getIntValue());
    Assert.assertEquals(tsaGroupBind, server.getTsaGroupPort().getBind());

  }

  public void testServerDirectoryDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + " </server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "data")
        .getAbsolutePath(), server.getData());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "logs")
        .getAbsolutePath(), server.getLogs());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + "data-backup").getAbsolutePath(), server.getDataBackup());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "data"
                                 + File.separator + "index").getAbsolutePath(), server.getIndex());
  }

  public void testServerDirectoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<data>abc/xyz/123</data>" + "<logs>xyz/abc/451</logs>"
                    + "<data-backup>/qrt/opt/pqr</data-backup>" + "<index>/rta/try/456</index>" + "</server>"
                    + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert
        .assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "abc"
                               + File.separator + "xyz" + File.separator + "123").getAbsolutePath(), server.getData());
    Assert
        .assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "xyz"
                               + File.separator + "abc" + File.separator + "451").getAbsolutePath(), server.getLogs());
    if (Os.isWindows()) {
      // for windows box
      Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "qrt"
                                   + File.separator + "opt" + File.separator + "pqr").getAbsolutePath(),
                          server.getDataBackup());
      Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator + "rta"
                                   + File.separator + "try" + File.separator + "456").getAbsolutePath(),
                          server.getIndex());
    } else {
      Assert.assertEquals("/qrt/opt/pqr", server.getDataBackup());
      Assert.assertEquals("/rta/try/456", server.getIndex());
    }
  }

  public void testServerSubsitutedDirectoryPaths() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<data>%h</data>" + "<logs>%i</logs>" + "<data-backup>%H</data-backup>" + "</server>"
                    + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + InetAddress.getLocalHost().getHostName()).getAbsolutePath(), server.getData());
    Assert.assertEquals(new File(BaseConfigurationSetupManagerTest.class.getSimpleName() + File.separator
                                 + InetAddress.getLocalHost().getHostAddress()).getAbsolutePath(), server.getLogs());
    Assert.assertEquals(System.getProperty("user.home"), server.getDataBackup());
  }

  public void testDefaultDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);

    Assert.assertFalse(servers.getRestartable().getEnabled());
    Assert.assertEquals(120, servers.getClientReconnectWindow());
    // DGC is disabled by default
    Assert.assertFalse(servers.getGarbageCollection().getEnabled());
    Assert.assertEquals(false, servers.getGarbageCollection().getVerbose());
    Assert.assertEquals(3600, servers.getGarbageCollection().getInterval());
  }

  public void testDefaultPersistence() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);
    Assert.assertFalse(server.isSetOffheap());
    Assert.assertFalse(servers.getRestartable().getEnabled());
  }

  public void testDefaultOffHeap() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
                    + "<garbage-collection>" + "<enabled>false</enabled>" + "<verbose>true</verbose>"
                    + "<interval>1234</interval>" + "</garbage-collection>"
                    + "<client-reconnect-window>9876</client-reconnect-window>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(9876, servers.getClientReconnectWindow());
    Assert.assertEquals(false, servers.getGarbageCollection().getEnabled());
    Assert.assertEquals(true, servers.getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, servers.getGarbageCollection().getInterval());
    Assert.assertFalse(server.isSetOffheap());
  }

  public void testOffHeap1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<offheap>" + "<enabled>true</enabled>" + "<maxDataSize>5628m</maxDataSize>" + "</offheap>"
                    + "</server>" + "<garbage-collection>" + "<enabled>false</enabled>" + "<verbose>true</verbose>"
                    + "<interval>1234</interval>" + "</garbage-collection>"
                    + "<client-reconnect-window>9876</client-reconnect-window>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);
    Server server = servers.getMirrorGroupArray(0).getServerArray(0);

    Assert.assertEquals(9876, servers.getClientReconnectWindow());
    Assert.assertEquals(false, servers.getGarbageCollection().getEnabled());
    Assert.assertEquals(true, servers.getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, servers.getGarbageCollection().getInterval());

    Assert.assertTrue(server.getDataStorage().isSetOffheap());
    Assert.assertEquals("5628m", server.getDataStorage().getOffheap().getSize());
  }

  public void testOffHeap2() throws IOException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "<offheap>" + "<enabled>true</enabled>" + "</offheap>" + "</server>"
                    + "<client-reconnect-window>9876</client-reconnect-window>" + "<garbage-collection>"
                    + "<enabled>false</enabled>" + "<verbose>true</verbose>" + "<interval>1234</interval>"
                    + "</garbage-collection>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    try {
      initializeAndGetBaseTVSConfigSetupManager(false);
      Assert.fail("parsing should fail since maxDataSize was missing");

    } catch (ConfigurationSetupException e) {
      // do noting expected exception
    }
  }

  public void testDso() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
                    + "<garbage-collection>" + "<enabled>false</enabled>" + "<verbose>true</verbose>"
                    + "<interval>1234</interval>" + "</garbage-collection>" + "<restartable enabled=\"true\"/>"
                    + "<client-reconnect-window>9876</client-reconnect-window>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);

    Assert.assertEquals(9876, servers.getClientReconnectWindow());
    Assert.assertEquals(false, servers.getGarbageCollection().getEnabled());
    Assert.assertEquals(true, servers.getGarbageCollection().getVerbose());
    Assert.assertEquals(1234, servers.getGarbageCollection().getInterval());
    Assert.assertTrue(servers.getRestartable().getEnabled());
  }

  public void testMirrorGroupDefaults() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(1, servers.getMirrorGroupArray().length);
    Assert.assertEquals(1, servers.getMirrorGroupArray(0).getServerArray().length);

    MirrorGroup mirrorGroup = servers.getMirrorGroupArray(0);
    Assert.assertEquals(1, mirrorGroup.sizeOfServerArray());
    Assert.assertEquals(5, mirrorGroup.getElectionTime());
    Assert.assertEquals("default-group", mirrorGroup.getGroupName());
  }

  public void testMirrorGroupWithDefaultHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
                    + "<mirror-group group-name=\"group1\">" + "<server host=\"eng01\" name=\"server1\"></server>"
                    + "<server host=\"eng02\" name=\"server2\"></server>" + "</mirror-group>"
                    + "<mirror-group group-name=\"group2\">" + "<server host=\"eng03\" name=\"server3\"></server>"
                    + "<server host=\"eng04\" name=\"server4\"></server>" + "</mirror-group>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(2, servers.getMirrorGroupArray().length);
    Assert.assertEquals(2, servers.getMirrorGroupArray(0).getServerArray().length);
    Assert.assertEquals(2, servers.getMirrorGroupArray(1).getServerArray().length);

    MirrorGroup mirrorGroup = servers.getMirrorGroupArray(0);
    Assert.assertEquals("group1", mirrorGroup.getGroupName());
    Assert.assertEquals("server1", mirrorGroup.getServerArray(0).getName());
    Assert.assertEquals("server2", mirrorGroup.getServerArray(1).getName());
    Assert.assertEquals(5, mirrorGroup.getElectionTime());

    mirrorGroup = servers.getMirrorGroupArray(1);
    Assert.assertEquals("group2", mirrorGroup.getGroupName());
    Assert.assertEquals("server3", mirrorGroup.getServerArray(0).getName());
    Assert.assertEquals("server4", mirrorGroup.getServerArray(1).getName());
    Assert.assertEquals(5, mirrorGroup.getElectionTime());
  }

  public void testMirrorGroupWithGivenHa() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>"
                    + "<mirror-group group-name=\"group1\">" + "<server host=\"eng01\" name=\"server1\"></server>"
                    + "<server host=\"eng02\" name=\"server2\"></server>" + "</mirror-group>"
                    + "<mirror-group group-name=\"group2\" election-time=\"15\">"
                    + "<server host=\"eng03\" name=\"server3\"></server>"
                    + "<server host=\"eng04\" name=\"server4\"></server>" + "</mirror-group>" + "</servers>"
                    + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertEquals(2, servers.getMirrorGroupArray().length);
    Assert.assertEquals(2, servers.getMirrorGroupArray(0).getServerArray().length);
    Assert.assertEquals(2, servers.getMirrorGroupArray(1).getServerArray().length);

    MirrorGroup mirrorGroup = servers.getMirrorGroupArray(0);
    Assert.assertEquals("group1", mirrorGroup.getGroupName());
    Assert.assertEquals("server1", mirrorGroup.getServerArray(0).getName());
    Assert.assertEquals("server2", mirrorGroup.getServerArray(1).getName());
    Assert.assertEquals(5, mirrorGroup.getElectionTime());

    mirrorGroup = servers.getMirrorGroupArray(1);
    Assert.assertEquals("group2", mirrorGroup.getGroupName());
    Assert.assertEquals("server3", mirrorGroup.getServerArray(0).getName());
    Assert.assertEquals("server4", mirrorGroup.getServerArray(1).getName());
    Assert.assertEquals(15, mirrorGroup.getElectionTime());
  }

  public void testUpdateCheckDefault() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(true, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(7, servers.getUpdateCheck().getPeriodDays());
  }

  public void testUpdateCheck() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<servers>" + "<server>"
                    + "</server>" + "<update-check>" + "<enabled>false</enabled>" + "<period-days>14</period-days>"
                    + "</update-check>" + "</servers>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Servers servers = (Servers) configSetupMgr.serversBeanRepository().bean();

    Assert.assertTrue(servers.isSetUpdateCheck());
    Assert.assertEquals(false, servers.getUpdateCheck().getEnabled());
    Assert.assertEquals(14, servers.getUpdateCheck().getPeriodDays());
  }

  public void testClientNotInitialized() throws Exception {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(false);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertNull(client);

    configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);
    client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertNotNull(client);
  }

  public void testDefaultClientLogDirectory() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals(new File(ParameterSubstituter.substitute("logs-%i")).getAbsolutePath(), client.getLogs());
  }

  public void testClientLogDirectory() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<clients>"
                    + "<logs>/abc/xyz/tra</logs>" + "</clients>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals("/abc/xyz/tra", client.getLogs());
  }

  public void testClientLogDirectory1() throws IOException, ConfigurationSetupException {
    this.tcConfig = getTempFile("default-config.xml");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<clients>" + "<logs>%i</logs>"
                    + "</clients>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals(ParameterSubstituter.substitute("%i"), client.getLogs());
  }

  public void testTcNodeNameClientLogDirectory() throws Exception {
    this.tcConfig = getTempFile("default-config.xml");
    System.setProperty("tc.nodeName", "node123");
    String config = "<tc:tc-config xmlns:tc=\"http://www.terracotta.org/config\">" + "<clients>"
                    + "<logs>%(tc.nodeName)</logs>" + "</clients>" + "</tc:tc-config>";

    writeConfigFile(config);

    BaseConfigurationSetupManager configSetupMgr = initializeAndGetBaseTVSConfigSetupManager(true);

    Client client = (Client) configSetupMgr.clientBeanRepository().bean();
    Assert.assertEquals("node123", client.getLogs());
  }

  private BaseConfigurationSetupManager initializeAndGetBaseTVSConfigSetupManager(boolean isClient)
      throws ConfigurationSetupException {
    String[] args = new String[] { "-f", tcConfig.getAbsolutePath() };

    String effectiveConfigSpec = getEffectiveConfigSpec(System
                                                            .getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME),
                                                        parseDefaultCommandLine(args,
                                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2),
                                                        StandardConfigurationSetupManagerFactory.ConfigMode.L2);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null"
                                                                                          : "'" + cwdAsString + "'")
                                                                                      + ".)"); }
    ConfigurationSpec configurationSpec = new ConfigurationSpec(
                                                                effectiveConfigSpec,
                                                                System
                                                                    .getProperty(ConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                                StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                new File(cwdAsString));

    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(
                                                                                        configurationSpec,
                                                                                        new TerracottaDomainConfigurationDocumentBeanFactory());

    BaseConfigurationSetupManager configSetupMgr = new BaseConfigurationSetupManager(
                                                                                     configurationCreator,
                                                                                     new SchemaDefaultValueProvider(),
                                                                                     new StandardXmlObjectComparator(),
                                                                                     new FatalIllegalConfigurationChangeHandler());
    configSetupMgr.runConfigurationCreator(isClient);

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

  private String getEffectiveConfigSpec(final String configSpec, final CommandLine commandLine,
                                        final ConfigMode configMode) throws ConfigurationSetupException {

    String configFileOnCommandLine = null;
    String effectiveConfigSpec;

    configFileOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('f'));
    effectiveConfigSpec = StringUtils
        .trimToNull(configFileOnCommandLine != null ? configFileOnCommandLine : configSpec);

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
                                            + "configuration file for this process, using the " + "'"
                                            + CONFIG_FILE_PROPERTY_NAME + "' system property.");
    }

    return effectiveConfigSpec;
  }

  private static CommandLine parseDefaultCommandLine(String[] args, ConfigMode configMode)
      throws ConfigurationSetupException {
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
