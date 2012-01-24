/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang.StringUtils;

import com.tc.config.schema.IllegalConfigurationChangeHandler;
import com.tc.logging.TCLogger;

import java.io.File;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} &mdash; uses
 * system properties and command-line arguments to create the relevant factories.
 */
public class StandardConfigurationSetupManagerFactory extends BaseConfigurationSetupManagerFactory {

  private static final String     CONFIG_SPEC_ARGUMENT_NAME = "config";
  /**
   * <code>public</code> for <strong>TESTS AND DOCUMENTATION ONLY</strong>.
   */
  public static final String      CONFIG_SPEC_ARGUMENT_WORD = "--" + CONFIG_SPEC_ARGUMENT_NAME;
  public static final String      SERVER_NAME_ARGUMENT_WORD = "-n";

  private static final String     L2_NAME_PROPERTY_NAME     = "tc.server.name";
  public static final String      DEFAULT_CONFIG_SPEC       = "tc-config.xml";
  public static final String      DEFAULT_CONFIG_PATH       = "default-config.xml";
  public static final String      DEFAULT_CONFIG_URI        = "resource:///"
                                                              + StandardConfigurationSetupManagerFactory.class
                                                                  .getPackage().getName().replace('.', '/') + "/"
                                                              + DEFAULT_CONFIG_PATH;

  private final String[]          args;
  private final String            defaultL2Identifier;
  private final ConfigurationSpec configurationSpec;

  public static enum ConfigMode {
    L2, CUSTOM_L1, EXPRESS_L1
  }

  public StandardConfigurationSetupManagerFactory(ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this((String[]) null, configMode, illegalChangeHandler);
  }

  public StandardConfigurationSetupManagerFactory(String[] args, ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this(args, parseDefaultCommandLine(args, configMode), configMode, illegalChangeHandler);
  }

  public StandardConfigurationSetupManagerFactory(CommandLine commandLine, ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this((String[]) null, commandLine, configMode, illegalChangeHandler);
  }

  public StandardConfigurationSetupManagerFactory(String[] args, CommandLine commandLine, ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler)
      throws ConfigurationSetupException {
    this(args, commandLine, configMode, illegalChangeHandler, System
        .getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME));
  }

  public StandardConfigurationSetupManagerFactory(String[] args, ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler,
                                                  String configSpec) throws ConfigurationSetupException {
    this(args, parseDefaultCommandLine(args, configMode), configMode, illegalChangeHandler, configSpec);
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

  public StandardConfigurationSetupManagerFactory(String[] args, CommandLine commandLine, ConfigMode configMode,
                                                  IllegalConfigurationChangeHandler illegalChangeHandler,
                                                  String configSpec) throws ConfigurationSetupException {
    super(illegalChangeHandler);
    String effectiveConfigSpec = getEffectiveConfigSpec(configSpec, commandLine, configMode);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null"
                                                                                          : "'" + cwdAsString + "'")
                                                                                      + ".)"); }
    this.args = args;
    this.configurationSpec = new ConfigurationSpec(
                                                   effectiveConfigSpec,
                                                   System
                                                       .getProperty(ConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                   configMode, new File(cwdAsString));
    this.defaultL2Identifier = getDefaultL2Identifier(commandLine);
  }

  private String getDefaultL2Identifier(final CommandLine commandLine) {
    String l2NameOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('n'));
    String specifiedL2Identifier = StringUtils.trimToNull(l2NameOnCommandLine != null ? l2NameOnCommandLine : System
        .getProperty(L2_NAME_PROPERTY_NAME));
    return specifiedL2Identifier;
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

  public static Options createOptions(ConfigMode configMode) {
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

  public L1ConfigurationSetupManager createL1TVSConfigurationSetupManager(TCLogger logger)
      throws ConfigurationSetupException {
    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(logger, this.configurationSpec,
                                                                                        this.beanFactory);

    L1ConfigurationSetupManager setupManager = new L1ConfigurationSetupManagerImpl(configurationCreator,
                                                                                   this.defaultValueProvider,
                                                                                   this.xmlObjectComparator,
                                                                                   this.illegalChangeHandler);

    return setupManager;
  }

  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager() throws ConfigurationSetupException {
    ConfigurationCreator configurationCreator = new StandardXMLFileConfigurationCreator(this.configurationSpec,
                                                                                        this.beanFactory);

    L1ConfigurationSetupManager setupManager = new L1ConfigurationSetupManagerImpl(configurationCreator,
                                                                                   this.defaultValueProvider,
                                                                                   this.xmlObjectComparator,
                                                                                   this.illegalChangeHandler);

    return setupManager;
  }

  public L2ConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Name)
      throws ConfigurationSetupException {
    if (l2Name == null) l2Name = this.defaultL2Identifier;

    ConfigurationCreator configurationCreator;
    configurationCreator = new StandardXMLFileConfigurationCreator(this.configurationSpec, this.beanFactory);

    return new L2ConfigurationSetupManagerImpl(args, configurationCreator, l2Name, this.defaultValueProvider,
                                               this.xmlObjectComparator, this.illegalChangeHandler);
  }

  public String[] getArguments() {
    return args;
  }
}
