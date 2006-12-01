/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang.ArrayUtils;

import com.tc.config.schema.NewCommonL2Config;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory;
import com.tc.management.TerracottaManagement;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;

import java.io.File;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class TCStop {

  private String             m_host;
  private int                m_port;

  public static final String DEFAULT_HOST = "localhost";
  public static final int    DEFAULT_PORT = 9520;
  
  public static final void main(String[] args) throws Exception {
    Options options = StandardTVSConfigurationSetupManagerFactory.createOptions(true);
    CommandLine commandLine = null;
    
    Option helpOption = new Option("h", "help");
    helpOption.setType(String.class);
    helpOption.setRequired(false);
    options.addOption(helpOption);
    
    try {
      commandLine = new GnuParser().parse(options, args);
    } catch(UnrecognizedOptionException e) {
      System.err.println(e.getMessage());
      usageAndDie(options);
    }

    if(commandLine == null || commandLine.getArgs().length > 2) {
      usageAndDie(options);
    }
    
    String host = null;
    int port = -1;

    if (commandLine.hasOption("h")) {
      new HelpFormatter().printHelp("java " + TCStop.class.getName(), options);
      System.exit(1);
    }
    
    String  defaultName     = StandardTVSConfigurationSetupManagerFactory.DEFAULT_CONFIG_SPEC_FOR_L2;
    File    configFile      = new File(System.getProperty("user.dir"), defaultName);
    boolean configSpecified = commandLine.hasOption('f');
    boolean nameSpecified   = commandLine.hasOption('n');
    
    if (configSpecified || System.getProperty("tc.config") != null || configFile.exists()) {
      if (!configSpecified && System.getProperty("tc.config") == null) {
        ArrayList tmpArgs = new ArrayList(Arrays.asList(args));

        tmpArgs.add("-f");
        tmpArgs.add(configFile.getAbsolutePath());
        args = (String[])tmpArgs.toArray(new String[tmpArgs.size()]);
      }
      
      FatalIllegalConfigurationChangeHandler changeHandler = new FatalIllegalConfigurationChangeHandler();
      TVSConfigurationSetupManagerFactory factory = new StandardTVSConfigurationSetupManagerFactory(args, true, changeHandler);
      
      String name = null;
      if (nameSpecified) {
        name = commandLine.getOptionValue('n');
      }

      L2TVSConfigurationSetupManager manager = factory.createL2TVSConfigurationSetupManager(name);
      String[] servers = manager.allCurrentlyKnownServers();
      
      if (nameSpecified && !Arrays.asList(servers).contains(name)) {
        System.err.println("The specified Terracotta server configuration '"+name+"' does not exist; exiting.");
        System.exit(1);
      }
      
      if (name == null && servers != null && servers.length == 1) {
        name = servers[0];
        System.err.println("There is only one Terracotta server in this configuration file ("+name+"); stopping it.");
      } else if (name == null && servers != null && servers.length > 1) {
        System.err.println("There are multiple Terracotta servers defined in this configuration file; please specify "
                           + "which one you want to stop, using the '-n' command-line option. Available servers are:\n"
                           + "    " + ArrayUtils.toString(servers));
        System.exit(1);
      }

      NewCommonL2Config serverConfig = manager.commonL2ConfigFor(name);

      host = serverConfig.host().getString();
      if (host == null) host = name;
      if (host == null) host = DEFAULT_HOST;
      port = serverConfig.jmxPort().getInt();
      System.err.println("Host: " + host + ", port: " + port);
    } else {
      if (commandLine.getArgs().length == 0) {
        host = DEFAULT_HOST;
        port = DEFAULT_PORT;
        System.err
            .println("No host or port provided. Stopping the Terracotta server at '" + host + "', port " + port + " by default.");
      } else if (commandLine.getArgs().length == 1) {
        host = DEFAULT_HOST;
        port = Integer.parseInt(commandLine.getArgs()[0]);
        System.err.println("Stopping the Terracotta server at '" + host + "', port " + port + ".");
      } else {
        host = commandLine.getArgs()[0];
        port = Integer.parseInt(commandLine.getArgs()[1]);
        System.err.println("Stopping the Terracotta server at '" + host + "', port " + port + ".");
      }
    }

    try {
      new TCStop(host, port).stop();
    } catch (ConnectException ce) {
      System.err.println("Unable to connect to host '" + host + "', port " + port
                         + ". Are you sure there is a Terracotta server running there?");
    }
  }

  private static void usageAndDie(Options options) throws Exception {
    new HelpFormatter().printHelp("java " + TCStop.class.getName(), options);
    System.exit(1);
  }

  public TCStop(String host, int port) {
    m_host = host;
    m_port = port;
  }

  public void stop() throws Exception {
    JMXConnector jmxc = getJMXConnector();
    MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();

    if (mbsc != null) {
      TCServerInfoMBean tcServerInfo = (TCServerInfoMBean) TerracottaManagement.findMBean(L2MBeanNames.TC_SERVER_INFO,
                                                                                TCServerInfoMBean.class, mbsc);
      try {
        tcServerInfo.shutdown();
      } catch (Exception e) { /* ignore */
        e.printStackTrace();
      } finally {
        jmxc.close();
      }
    }
  }

  private JMXConnector getJMXConnector() throws Exception {
    String uri = "service:jmx:jmxmp://" + m_host + ":" + m_port;
    JMXServiceURL url = new JMXServiceURL(uri);

    return JMXConnectorFactory.connect(url, null);
  }
}
