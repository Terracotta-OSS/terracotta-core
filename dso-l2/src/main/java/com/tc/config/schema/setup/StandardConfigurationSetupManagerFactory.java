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

import com.tc.config.Directories;
import com.tc.text.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * The standard implementation of {@link com.tc.config.schema.setup.ConfigurationSetupManagerFactory} &mdash; uses system properties and
 * command-line arguments to create the relevant factories.
 */
public class StandardConfigurationSetupManagerFactory extends BaseConfigurationSetupManagerFactory {

  private static final String CONFIG_SPEC_ARGUMENT_NAME = "config";
  /**
   * <code>public</code> for <strong>TESTS AND DOCUMENTATION ONLY</strong>.
   */
  public static final String CONFIG_SPEC_ARGUMENT_WORD = "--" + CONFIG_SPEC_ARGUMENT_NAME;
  public static final String SERVER_NAME_ARGUMENT_WORD = "-n";

  private static final String L2_NAME_PROPERTY_NAME = "tc.server.name";
  public static final String DEFAULT_CONFIG_SPEC = "tc-config.xml";

  private final String[] args;
  private final String defaultL2Identifier;
  private final ConfigurationSpec configurationSpec;

  public static enum ConfigMode {
    L2, CUSTOM_L1, EXPRESS_L1
  }

  public StandardConfigurationSetupManagerFactory(String[] args, ConfigMode configMode)
      throws ConfigurationSetupException {
    this(args, parseDefaultCommandLine(args, configMode), configMode);
  }

  public StandardConfigurationSetupManagerFactory(String[] args, CommandLine commandLine, ConfigMode configMode)
      throws ConfigurationSetupException {
    this(args, commandLine, configMode, System.getProperty(ConfigurationSetupManagerFactory.CONFIG_FILE_PROPERTY_NAME));
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

  public StandardConfigurationSetupManagerFactory(String[] args, CommandLine commandLine, ConfigMode configMode, String configSpec) throws ConfigurationSetupException {
    String effectiveConfigSpec = getEffectiveConfigSpec(configSpec, commandLine, configMode);
    String cwdAsString = System.getProperty("user.dir");
    if (StringUtils.isBlank(cwdAsString)) { throw new ConfigurationSetupException(
                                                                                  "We can't find the working directory of the process; we need this to continue. "
                                                                                      + "(The system property 'user.dir' was "
                                                                                      + (cwdAsString == null ? "null" : "'" + cwdAsString
                                                                                                                        + "'") + ".)"); }
    this.args = args;
    this.configurationSpec = new ConfigurationSpec(effectiveConfigSpec,
                                                   System.getProperty(ConfigurationSetupManagerFactory.SERVER_CONFIG_FILE_PROPERTY_NAME),
                                                   configMode, new File(cwdAsString));
    this.defaultL2Identifier = getDefaultL2Identifier(commandLine);
  }

  private String getDefaultL2Identifier(CommandLine commandLine) {
    String l2NameOnCommandLine = StringUtils.trimToNull(commandLine.getOptionValue('n'));
    String specifiedL2Identifier = StringUtils.trimToNull(l2NameOnCommandLine != null ? l2NameOnCommandLine : System
        .getProperty(L2_NAME_PROPERTY_NAME));
    return specifiedL2Identifier;
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
        try {
          File defaultConfigFile = Directories.getDefaultConfigFile();
          if (defaultConfigFile.exists()) {
            effectiveConfigSpec = defaultConfigFile.getAbsolutePath();
          }
        } catch (FileNotFoundException e) {
          // Ignore
        }
      }
    }

    if (StringUtils.isBlank(effectiveConfigSpec)) {
      // formatting
      throw new ConfigurationSetupException("You must specify the location of the Terracotta "
                                            + "configuration file for this process.\n" +
                                            "You can do this by:\n" +
                                            "\t* using the '-f <file>' flag on the command line\n" +
                                            "\t* using the " + "'" + CONFIG_FILE_PROPERTY_NAME + "' system property\n" +
                                            "\t* placing the file in '${user.dir}/tc-config.xml'\n" +
                                            "\t* placing the file in '<install_root>/conf/tc-config.xml\n" +
                                            "The above options are in order of precedence.");
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

  @Override
  public L2ConfigurationSetupManager createL2TVSConfigurationSetupManager(String l2Name, ClassLoader loader)
      throws ConfigurationSetupException {
    if (l2Name == null) l2Name = this.defaultL2Identifier;

    ConfigurationCreator configurationCreator;
    configurationCreator = new StandardXMLFileConfigurationCreator(this.configurationSpec, this.beanFactory);

    return new L2ConfigurationSetupManagerImpl(args, configurationCreator, l2Name, loader);
  }

  public String[] getArguments() {
    return args;
  }
}
