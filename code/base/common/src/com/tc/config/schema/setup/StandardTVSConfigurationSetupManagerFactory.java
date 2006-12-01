/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.IllegalConfigurationChangeHandler;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.TVSConfigurationSetupManagerFactory} &mdash; uses
 * system properties and command-line arguments to create the relevant factories.
 */
public class StandardTVSConfigurationSetupManagerFactory extends BaseTVSConfigurationSetupManagerFactory {

  private static final String   CONFIG_SPEC_ARGUMENT_NAME        = "config";
  /**
   * <code>public</code> for <strong>TESTS AND DOCUMENTATION ONLY</strong>.
   */
  public static final String    CONFIG_SPEC_ARGUMENT_WORD        = "--" + CONFIG_SPEC_ARGUMENT_NAME;

  private static final String   L2_NAME_PROPERTY_NAME            = "tc.server.name";
  public static final String    DEFAULT_CONFIG_SPEC_FOR_L2       = "tc-config.xml";
  public static final String    DEFAULT_CONFIG_PATH              = "default-config.xml";

  private final String          defaultL2Identifier;
  private final String          configSpec;
  private final File            cwd;

  public StandardTVSConfigurationSetupManagerFactory(boolean isForL2,
                                                     IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this((String[]) null, isForL2, illegalChangeHandler);
  }

  public StandardTVSConfigurationSetupManagerFactory(String[] args, boolean isForL2,
                                                     IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this(parseDefaultCommandLine(args, isForL2), isForL2, illegalChangeHandler);
  }

  private static CommandLine parseDefaultCommandLine(String[] args, boolean isForL2) throws ConfigurationSetupException {
    try {
      if (args == null || args.length == 0) {
        return new PosixParser().parse(new Options(), new String[0]);
      } else {
        Options options = createOptions(isForL2);

        return new PosixParser().parse(options, args);
      }
    } catch (ParseException pe) {
      throw new ConfigurationSetupException(pe.getLocalizedMessage(), pe);
    }
  }

  public StandardTVSConfigurationSetupManagerFactory(CommandLine commandLine, boolean isForL2,
                                                     IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    super(illegalChangeHandler);

    String configFileOnCommandLine = null;
    String l2NameOnCommandLine = null;

    configFileOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('f'));
    l2NameOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('n'));

    this.configSpec = StringUtils.trimToNull(configFileOnCommandLine != null ? configFileOnCommandLine : System
        .getProperty(TVSConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME));
    String specifiedL2Identifier = StringUtils.trimToNull(l2NameOnCommandLine != null ? l2NameOnCommandLine : System
        .getProperty(L2_NAME_PROPERTY_NAME));

    if ((!isForL2) && StringUtils.isBlank(this.configSpec)) {
      // formatting
      throw new ConfigurationSetupException("You must specify the location of the Terracotta "
                                            + "configuration file for this process, using the " + "'"
                                            + CONFIG_FILE_PROPERTY_NAME + "' system property.");
    }

    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) {
      // formatting
      throw new ConfigurationSetupException(
                                            "We can't find the working directory of the process; we need this to continue. "
                                            + "(The system property 'user.dir' was "
                                            + (cwdAsString == null ? "null" : "'" + cwdAsString + "'") + ".)");
    }

    this.cwd = new File(cwdAsString);

    if (specifiedL2Identifier != null) {
      this.defaultL2Identifier = specifiedL2Identifier;
    } else {
      String hostName = null;

      try {
        hostName = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException uhe) {
        /**/
      }

      String potentialName = hostName;

      if (potentialName != null && potentialName.indexOf(".") >= 0) potentialName = potentialName
          .substring(0, potentialName.indexOf("."));
      if (potentialName != null) potentialName = potentialName.trim();

      if (!StringUtils.isBlank(potentialName) && (!potentialName.equalsIgnoreCase("localhost"))) {
        this.defaultL2Identifier = potentialName;
      } else {
        this.defaultL2Identifier = null;
      }
    }
  }

  public static Options createOptions(boolean isForL2) {
    Options options = new Options();

    Option configFileOption = new Option("f", CONFIG_SPEC_ARGUMENT_NAME, true,
                                         "the configuration file to use, specified as a file path or URL");
    configFileOption.setArgName("file-or-URL");
    configFileOption.setType(String.class);

    Option l2NameOption = new Option("n", "name", true, "the name of this L2; defaults to the host name");
    l2NameOption.setRequired(false);
    l2NameOption.setArgName("l2-name");    

    if (isForL2) {
      configFileOption.setRequired(false);
      options.addOption(configFileOption);
      options.addOption(l2NameOption);
    } else {
      configFileOption.setRequired(true);
      options.addOption(configFileOption);
    }

    return options;
  }


  public L1TVSConfigurationSetupManager createL1TVSConfigurationSetupManager()
    throws ConfigurationSetupException
  {
    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(this.configSpec,
                                                                                        this.cwd,
                                                                                        this.beanFactory);

    L1TVSConfigurationSetupManager setupManager = new StandardL1TVSConfigurationSetupManager(configurationCreator,
                                                                                             this.defaultValueProvider,
                                                                                             this.xmlObjectComparator,
                                                                                             this.illegalChangeHandler);
    
    return setupManager;
  }

  public L2TVSConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Name)
    throws ConfigurationSetupException
  {
    if (l2Name == null) l2Name = this.defaultL2Identifier;

    String effectiveConfigSpec;

    if (this.configSpec == null) {
      File localConfig = new File(System.getProperty("user.dir"),
                                  DEFAULT_CONFIG_SPEC_FOR_L2);
      
      if(localConfig.exists()) {
        effectiveConfigSpec = localConfig.getAbsolutePath();
      }
      else {
        String packageName = getClass().getPackage().getName();
        effectiveConfigSpec = "resource:///" + packageName.replace('.', '/') + "/" + DEFAULT_CONFIG_PATH;
      }
    } else {
      effectiveConfigSpec = this.configSpec;
    }
    
    ConfigurationCreator configurationCreator;
    
    configurationCreator = new StandardXMLFileConfigurationCreator(effectiveConfigSpec,
                                                                   this.cwd,
                                                                   this.beanFactory);

    return new StandardL2TVSConfigurationSetupManager(configurationCreator,
                                                      l2Name,
                                                      this.defaultValueProvider,
                                                      this.xmlObjectComparator,
                                                      this.illegalChangeHandler);
  }

}
