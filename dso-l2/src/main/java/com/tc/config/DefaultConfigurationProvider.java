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
package com.tc.config;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfig;
import org.terracotta.config.TcConfiguration;
import org.terracotta.config.service.ServiceConfigParser;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.classloader.ServiceLocator;
import com.tc.server.ServiceClassLoader;
import com.terracotta.config.Configuration;
import com.terracotta.config.ConfigurationException;
import com.terracotta.config.ConfigurationProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.tc.config.DefaultConfigurationProvider.Opt.CONFIG_PATH;
import com.tc.services.MappedStateCollector;
import com.tc.text.PrettyPrintable;
import java.util.Map;

public class DefaultConfigurationProvider implements ConfigurationProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConfigurationProvider.class);

  static final String DEFAULT_CONFIG_NAME = "tc-config.xml";

  static final String CONFIG_FILE_PROPERTY_NAME = "tc.config";

  enum Opt {
    CONFIG_PATH("f", "config");

    String shortName;
    String longName;

    Opt(String shortName, String longName) {
      Objects.requireNonNull(shortName);
      Objects.requireNonNull(longName);
      this.shortName = shortName;
      this.longName = longName;
    }

    public String getShortName() {
      return shortName;
    }

    public String getLongName() {
      return longName;
    }

    public String getShortOption() {
      return "-" + shortName;
    }

    public String getLongOption() {
      return "--" + longName;
    }
  }

  private volatile TcConfiguration configuration;

  @Override
  public void initialize(List<String> configurationParams) throws ConfigurationException {
    try {
      Path configurationPath = getConfiguration(configurationParams.toArray(new String[0])).toAbsolutePath();

      LOGGER.info("Attempting to load configuration from the file at '{}'...", configurationPath);

      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      ServiceLocator serviceLocator = new ServiceLocator(classLoader);
      ServiceClassLoader serviceClassLoader =
          new ServiceClassLoader(serviceLocator.getImplementations(ServiceConfigParser.class, classLoader));

      this.configuration = getTcConfiguration(configurationPath, serviceClassLoader);

      LOGGER.info("Successfully loaded configuration from the file at '{}'", configurationPath);

      LOGGER.info("The configuration specified by the configuration file at '{}': \n\n{}",
                  configurationPath,
                  configuration.toString());

    } catch (Exception e) {
      throw new ConfigurationException("Unable to initialize DefaultConfigurationProvider with " + configurationParams,
                                       e);
    }
  }

  @Override
  public Configuration getConfiguration() {
    return new TcConfigurationWrapper(configuration);
  }

  @Override
  public String getConfigurationParamsDescription() {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    new HelpFormatter().printOptions(printWriter, HelpFormatter.DEFAULT_WIDTH, createOptions(), 1, 5);
    printWriter.close();
    return stringWriter.toString();
  }

  @Override
  public void close() {

  }

  protected TcConfiguration getTcConfiguration(Path configurationPath, ClassLoader serviceClassLoader) throws Exception {
    return TCConfigurationParser.parse(
        Files.newInputStream(configurationPath),
        new ArrayList<>(),
        configurationPath.getParent().toString(),
        serviceClassLoader
    );
  }

  private Path getConfiguration(String[] args) throws Exception {
    CommandLine commandLine = new DefaultParser().parse(createOptions(), args);

    String cmdConfigurationFileName = commandLine.getOptionValue(CONFIG_PATH.getShortName());
    if (cmdConfigurationFileName != null && !cmdConfigurationFileName.isEmpty()) {
      Path path = Paths.get(cmdConfigurationFileName);
      if (Files.exists(path)) {
        return path;
      } else {
        String errorMessage = String.format(
            "Specified configuration file '%s' using the '%s' option is not found",
            cmdConfigurationFileName,
            CONFIG_PATH.getShortOption() + "/" + CONFIG_PATH.getLongOption()
        );
        throw new RuntimeException(errorMessage);
      }
    }

    String systemPropertyConfigurationFileName = System.getProperty(CONFIG_FILE_PROPERTY_NAME);
    if (systemPropertyConfigurationFileName != null && !systemPropertyConfigurationFileName.isEmpty()) {
      Path path = Paths.get(systemPropertyConfigurationFileName);
      if (Files.exists(path)) {
        return path;
      } else {
        String errorMessage = String.format(
            "Specified configuration file '%s' using the system property '%s' is not found",
            systemPropertyConfigurationFileName,
            CONFIG_FILE_PROPERTY_NAME
        );
        throw new RuntimeException(errorMessage);
      }
    }

    Path defaultUserDirConfigurationPath = Paths.get(System.getProperty("user.dir")).resolve(DEFAULT_CONFIG_NAME);
    if (Files.exists(defaultUserDirConfigurationPath)) {
      return defaultUserDirConfigurationPath;
    }

    Path defaultUserHomeConfigurationPath = Paths.get(System.getProperty("user.home")).resolve(DEFAULT_CONFIG_NAME);
    if (Files.exists(defaultUserHomeConfigurationPath)) {
      return defaultUserHomeConfigurationPath;
    }

    return Directories.getDefaultConfigFile().toPath();
  }

  private static Options createOptions() {
    Options options = new Options();

    options.addOption(
        Option.builder(CONFIG_PATH.getShortName())
              .longOpt(CONFIG_PATH.getLongName())
              .hasArg()
              .argName("config-file")
              .desc("specifies the server configuration file path")
              .build()
    );

    return options;
  }
  
  private static class TcConfigurationWrapper implements Configuration, PrettyPrintable {
    private final TcConfiguration  configuration;

    public TcConfigurationWrapper(TcConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public TcConfig getPlatformConfiguration() {
      return this.configuration.getPlatformConfiguration();
    }

    @Override
    public List<ServiceProviderConfiguration> getServiceConfigurations() {
      return this.configuration.getServiceConfigurations();
    }

    @Override
    public <T> List<T> getExtendedConfiguration(Class<T> type) {
      return this.configuration.getExtendedConfiguration(type);
    }

    @Override
    public String getRawConfiguration() {
      return configuration.toString();
    }
    
    @Override
    public Map<String, ?> getStateMap() {
      MappedStateCollector mappedStateCollector = new MappedStateCollector("collector");
      this.configuration.addStateTo(mappedStateCollector);
      return mappedStateCollector.getMap();
    }
  }
}
