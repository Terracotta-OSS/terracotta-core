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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.config.TcConfig;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.terracotta.config.TcConfiguration;
import com.tc.config.schema.beanfactory.BeanWithErrors;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.setup.sources.ConfigurationSource;
import com.tc.config.schema.setup.sources.FileConfigurationSource;
import com.tc.config.schema.setup.sources.ResourceConfigurationSource;
import com.tc.config.schema.setup.sources.ServerConfigurationSource;
import com.tc.config.schema.setup.sources.URLConfigurationSource;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.security.PwProvider;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link ConfigurationCreator} that works off XML files, using the standard config-spec model.
 */
public class StandardXMLFileConfigurationCreator implements ConfigurationCreator {

  private static final Logger consoleLogger = TCLogging.getConsoleLogger();
  private static final long GET_CONFIGURATION_TOTAL_TIMEOUT = TCPropertiesImpl.getProperties()
      .getLong(TCPropertiesConsts.TC_CONFIG_TOTAL_TIMEOUT);

  private static final long MIN_RETRY_TIMEOUT = 5 * 1000;
  private static final Pattern SERVER_PATTERN = Pattern.compile("(.*):(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(.*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern BASE64_PATTERN = Pattern.compile("base64://(.*)", Pattern.DOTALL);
  // We require more than one character before the colon so that we don't mistake Windows-style directory paths as URLs.
  private static final Pattern URL_PATTERN = Pattern.compile("[A-Za-z][A-Za-z]+://.*");
  private static final long GET_CONFIGURATION_ONE_SOURCE_TIMEOUT = TCPropertiesImpl.getProperties()
      .getLong(TCPropertiesConsts.TC_CONFIG_SOURCEGET_TIMEOUT, 30000);

  private final ConfigurationSpec configurationSpec;
  private final ConfigBeanFactory beanFactory;
  private final Logger logger;

  private boolean baseConfigLoadedFromTrustedSource;
  private String serverOverrideConfigDescription;
  private boolean serverOverrideConfigLoadedFromTrustedSource;
  private File directoryLoadedFrom;
  private String baseConfigDescription = "";
  private TcConfiguration tcConfigDocument;
  private TcConfiguration providedTcConfigDocument;
  private final PwProvider pwProvider;
  
  private ClassLoader loader;

  public StandardXMLFileConfigurationCreator(ConfigurationSpec configurationSpec, ConfigBeanFactory beanFactory) {
    this(LoggerFactory.getLogger(StandardXMLFileConfigurationCreator.class), configurationSpec, beanFactory, null);
  }

  public StandardXMLFileConfigurationCreator(ConfigurationSpec configurationSpec, ConfigBeanFactory beanFactory,
                                             PwProvider pwProvider) {
    this(LoggerFactory.getLogger(StandardXMLFileConfigurationCreator.class), configurationSpec, beanFactory, pwProvider);
  }

  public StandardXMLFileConfigurationCreator(Logger logger, ConfigurationSpec configurationSpec,
                                             ConfigBeanFactory beanFactory, PwProvider pwProvider) {
    Assert.assertNotNull(beanFactory);
    this.logger = logger;
    this.beanFactory = beanFactory;
    this.configurationSpec = configurationSpec;
    this.pwProvider = pwProvider;
  }
  
  @Override
  public void createConfiguration() throws ConfigurationSetupException {
    this.createConfiguration(ClassLoader.getSystemClassLoader());
  }
  
  @Override
  public void createConfiguration(ClassLoader loader) throws ConfigurationSetupException {
    loadConfigAndSetIntoRepositories(loader);
    logCopyOfConfig();
  }

  protected void loadConfigAndSetIntoRepositories(ClassLoader loader) throws ConfigurationSetupException {
    this.loader = loader;
    ConfigurationSource[] sources = getConfigurationSources(this.configurationSpec.getBaseConfigSpec());
    ConfigDataSourceStream baseConfigDataSourceStream = loadConfigDataFromSources(sources, loader);
    baseConfigLoadedFromTrustedSource = baseConfigDataSourceStream.isTrustedSource();
    baseConfigDescription = baseConfigDataSourceStream.getDescription();

    if (this.configurationSpec.shouldOverrideServerTopology()) {
      sources = getConfigurationSources(this.configurationSpec.getServerTopologyOverrideConfigSpec());
      ConfigDataSourceStream serverOverrideConfigDataSourceStream = loadServerConfigDataFromSources(sources, true, false, loader);
      serverOverrideConfigLoadedFromTrustedSource = serverOverrideConfigDataSourceStream.isTrustedSource();
      serverOverrideConfigDescription = serverOverrideConfigDataSourceStream.getDescription();
    }
  }

  @Override
  public String reloadServersConfiguration(boolean shouldLogTcConfig, boolean reportToConsole)
      throws ConfigurationSetupException {
    ConfigurationSource[] sources = getConfigurationSources(this.configurationSpec.getBaseConfigSpec());
    ConfigDataSourceStream serverOverrideConfigDataSourceStream;
    if (this.configurationSpec.shouldOverrideServerTopology()) {
      sources = getConfigurationSources(this.configurationSpec.getServerTopologyOverrideConfigSpec());
      serverOverrideConfigDataSourceStream = loadServerConfigDataFromSources(sources, reportToConsole, shouldLogTcConfig, loader);
      serverOverrideConfigLoadedFromTrustedSource = serverOverrideConfigDataSourceStream.isTrustedSource();
      serverOverrideConfigDescription = serverOverrideConfigDataSourceStream.getDescription();
    } else {
      serverOverrideConfigDataSourceStream = loadServerConfigDataFromSources(sources, reportToConsole, shouldLogTcConfig, loader);
    }

    if (shouldLogTcConfig) {
      logCopyOfConfig();
    }
    return serverOverrideConfigDataSourceStream.getDescription();
  }

  @Override
  public TcConfiguration getParsedConfiguration() {
    return this.tcConfigDocument;
  }

  protected ConfigurationSource[] getConfigurationSources(String configrationSpec) throws ConfigurationSetupException {
    String[] components = configrationSpec.split(",");
    ConfigurationSource[] out = new ConfigurationSource[components.length];

    for (int i = 0; i < components.length; ++i) {
      String thisComponent = components[i];
      ConfigurationSource source = attemptToCreateServerSource(thisComponent);

      if (source == null) source = attemptToCreateResourceSource(thisComponent);
      if (source == null) source = attemptToCreateBase64Source(thisComponent);
      if (source == null) source = attemptToCreateURLSource(thisComponent);
      if (source == null) source = attemptToCreateFileSource(thisComponent);

      if (source == null) {
        // formatting
        throw new ConfigurationSetupException("The location '" + thisComponent + "' is not in any recognized format -- it doesn't "
                                              + "seem to be a server, resource, URL, or file.");
      }

      out[i] = source;
    }

    return out;
  }

  private ConfigurationSource attemptToCreateServerSource(String text) {
    Matcher matcher = SERVER_PATTERN.matcher(text);
    if (matcher.matches()) {
      boolean secure = false;
      String username = null;
      String host = matcher.group(1);
      int userSeparatorIndex = host.indexOf('@');
      if (userSeparatorIndex > -1) {
        username = host.substring(0, userSeparatorIndex);
        try {
          username = URLDecoder.decode(username, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
          // cannot happen
        }
        secure = true;
        host = host.substring(userSeparatorIndex + 1);
      }
      final SecurityInfo securityInfo = new SecurityInfo(secure, username);
      String portText = matcher.group(2);

      try {
        return new ServerConfigurationSource(host.trim(), Integer.parseInt(portText.trim()), securityInfo, pwProvider);
      } catch (Exception e) {/**/
      }
    }
    return null;
  }

  private ConfigurationSource attemptToCreateBase64Source(String text) {
    Matcher matcher = BASE64_PATTERN.matcher(text);
    if (matcher.matches()) return new Base64ConfigurationSource(matcher.group(1));
    else return null;
  }

  private ConfigurationSource attemptToCreateResourceSource(String text) {
    Matcher matcher = RESOURCE_PATTERN.matcher(text);
    if (matcher.matches()) return new ResourceConfigurationSource(matcher.group(1), getClass());
    else return null;
  }

  private ConfigurationSource attemptToCreateFileSource(String text) {
    return new FileConfigurationSource(text, this.configurationSpec.getWorkingDir());
  }

  private ConfigurationSource attemptToCreateURLSource(String text) {
    Matcher matcher = URL_PATTERN.matcher(text);
    if (matcher.matches()) return new URLConfigurationSource(text);
    else return null;
  }

  private ConfigDataSourceStream loadConfigDataFromSources(ConfigurationSource[] sources, ClassLoader loader) throws ConfigurationSetupException {
    long startTime = System.currentTimeMillis();
    ConfigDataSourceStream configDataSourceStream = getConfigDataSourceStrean(sources, startTime, "base configuration");
    if (configDataSourceStream.getSourceInputStream() == null) configurationFetchFailed(sources, startTime);
    loadConfigurationData(configDataSourceStream.getSourceInputStream(), configDataSourceStream.isTrustedSource(),
                          configDataSourceStream.getDescription(), loader);
    consoleLogger.info("Successfully loaded " + configDataSourceStream.getDescription() + ".");
    return configDataSourceStream;
  }

  private ConfigDataSourceStream loadServerConfigDataFromSources(ConfigurationSource[] sources,
                                                                 boolean reportToConsole, boolean updateTcConfig, ClassLoader loader)
      throws ConfigurationSetupException {
    long startTime = System.currentTimeMillis();
    ConfigDataSourceStream configDataSourceStream = getConfigDataSourceStrean(sources, startTime, "server topology");
    if (configDataSourceStream.getSourceInputStream() == null) configurationFetchFailed(sources, startTime);
    loadServerConfigurationData(configDataSourceStream.getSourceInputStream(), configDataSourceStream.isTrustedSource(),
                                configDataSourceStream.getDescription(), updateTcConfig, loader);
    if (reportToConsole) {
      consoleLogger.info("Successfully overridden " + configDataSourceStream.getDescription() + ".");
    }
    return configDataSourceStream;
  }

  private static class ConfigDataSourceStream {
    private final InputStream sourceInputStream;
    private final boolean trustedSource;
    private final String description;

    public ConfigDataSourceStream(InputStream sourceInputStream, boolean trustedSource, String description) {
      this.sourceInputStream = sourceInputStream;
      this.trustedSource = trustedSource;
      this.description = description;
    }

    public String getDescription() {
      return description;
    }

    public InputStream getSourceInputStream() {
      return sourceInputStream;
    }

    public boolean isTrustedSource() {
      return trustedSource;
    }
  }

  @SuppressWarnings("resource")
  private ConfigDataSourceStream getConfigDataSourceStrean(ConfigurationSource[] sources, long startTime, String description) {
    ConfigurationSource[] remainingSources = new ConfigurationSource[sources.length];
    ConfigurationSource loadedSource = null;
    System.arraycopy(sources, 0, remainingSources, 0, sources.length);
    long lastLoopStartTime = 0;
    int iteration = 0;
    InputStream out = null;
    boolean trustedSource = false;
    String descrip = null;

    while (iteration == 0 || (System.currentTimeMillis() - startTime < GET_CONFIGURATION_TOTAL_TIMEOUT)) {
      sleepIfNecessaryToAvoidPoundingSources(lastLoopStartTime);
      lastLoopStartTime = System.currentTimeMillis();

      for (int i = 0; i < remainingSources.length; ++i) {

        if (remainingSources[i] == null) continue;
        out = trySource(remainingSources, i);
        if (out != null) {
          loadedSource = remainingSources[i];
          trustedSource = loadedSource.isTrusted();
          descrip = description + " from " + loadedSource.toString();
          break;
        }
      }

      if (out != null) break;
      ++iteration;
      boolean haveSources = false;
      for (ConfigurationSource remainingSource : remainingSources)
        haveSources = haveSources || remainingSource != null;
      if (!haveSources) {
        // All sources have failed; bail out.
        break;
      }
    }
    return new ConfigDataSourceStream(out, trustedSource, descrip);
  }

  private void configurationFetchFailed(ConfigurationSource[] sources, long startTime) throws ConfigurationSetupException {
    StringBuilder text = new StringBuilder("Could not fetch configuration data from ");
    if (sources.length > 1) {
      text.append(sources.length).append(" different configuration sources.  The sources we tried were: ");
      for (int i = 0; i < sources.length; ++i) {
        if (i > 0) text.append(", ");
        if (i == sources.length - 1) text.append("and ");
        text.append("the ").append(sources[i]);
      }
      text.append(". ");
    } else {
      text.append("the ").append(sources[0]).append(". ");
    }

    if (System.currentTimeMillis() - startTime >= GET_CONFIGURATION_TOTAL_TIMEOUT) {
      text.append(" Fetch attempt duration: ");
      text.append(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)).append(" seconds.");
    }

    text.append("\n\nTo correct this problem specify a valid configuration location using the -f/--config command-line options.");

    consoleLogger.error(text.toString());
    throw new ConfigurationSetupException(text.toString());
  }

  private InputStream trySource(ConfigurationSource[] remainingSources, int i) {
    InputStream out = null;
    InputStream copyIn = null;
    try {
      ByteArrayOutputStream dataCopy = new ByteArrayOutputStream();
      logger.info("Attempting to load configuration from the " + remainingSources[i] + "...");
      out = remainingSources[i].getInputStream(GET_CONFIGURATION_ONE_SOURCE_TIMEOUT);
      try {
        IOUtils.copy(out, dataCopy);
        copyIn = new ByteArrayInputStream(dataCopy.toByteArray());
      } finally {
        out.close();
      }
      directoryLoadedFrom = remainingSources[i].directoryLoadedFrom();
    } catch (ConfigurationSetupException cse) {
      String text = "We couldn't load configuration data from the " + remainingSources[i];
      text += "; this error is permanent, so this source will not be retried.";

      if (remainingSources.length > 1) text += " Skipping this source and going to the next one.";

      text += " (Error: " + cse.getLocalizedMessage() + ".)";

      consoleLogger.warn(text);

      remainingSources[i] = null;
    } catch (IOException ioe) {
      String text = "We couldn't load configuration data from the " + remainingSources[i];

      if (remainingSources.length > 1) {
        text += "; this error is temporary, so this source will be retried later if configuration can't be loaded elsewhere. ";
        text += "Skipping this source and going to the next one.";
      } else {
        text += "; retrying.";
      }

      text += " (Error: " + ioe.getLocalizedMessage() + ".)";
      consoleLogger.warn(text);
    }
    return copyIn;
  }

  private void sleepIfNecessaryToAvoidPoundingSources(long lastLoopStartTime) {
    long delay = MIN_RETRY_TIMEOUT - (System.currentTimeMillis() - lastLoopStartTime);
    if (delay > 0) {
      logger.info("Waiting " + delay + " ms until we try to get configuration data again...");
      ThreadUtil.reallySleep(delay);
    }
  }

  private void updateTcConfigFull(TcConfiguration configDocument, String description) {
    updateTcConfig(configDocument, description, false);
  }

  private void updateTcConfig(TcConfiguration configDocument, String description, boolean serverElementsOnly) {
    if (!serverElementsOnly) {
      this.tcConfigDocument = configDocument;
    } else {
      Assert.assertNotNull(this.tcConfigDocument);
      TcConfig toConfig = this.tcConfigDocument.getPlatformConfiguration();
      TcConfig fromConfig = configDocument.getPlatformConfiguration();
      if (toConfig.getServers() != null) toConfig.setServers(fromConfig.getServers());
    }
  }

  private void logCopyOfConfig() {
    logger.info(describeSources() + ":\n\n" + this.providedTcConfigDocument.toString());
  }

  private void loadConfigurationData(InputStream in, boolean trustedSource, String descrip, ClassLoader loader) throws ConfigurationSetupException {
    TcConfiguration configDocument = getConfigFromSourceStream(in, trustedSource, descrip, directoryConfigurationLoadedFrom()!= null ?
        directoryConfigurationLoadedFrom().getAbsolutePath() : "", loader);
    Assert.assertNotNull(configDocument);
    updateTcConfigFull(configDocument, descrip);
  }

  private void loadServerConfigurationData(InputStream in, boolean trustedSource, String descrip,
                                           boolean updateTcConfig, ClassLoader loader) throws ConfigurationSetupException {
    TcConfiguration configDocument = getConfigFromSourceStream(in, trustedSource, descrip, directoryConfigurationLoadedFrom().getAbsolutePath(), loader);
    Assert.assertNotNull(configDocument);
    if (updateTcConfig) {
      updateTcConfigFull(configDocument, descrip);
    }
  }


  private TcConfiguration getConfigFromSourceStream(InputStream in, boolean trustedSource, String descrip, String source, ClassLoader loader)
      throws ConfigurationSetupException {
    TcConfiguration tcConfigDoc;
    try {
      ByteArrayOutputStream dataCopy = new ByteArrayOutputStream();
      IOUtils.copy(in, dataCopy);
      in.close();

      InputStream copyIn = new ByteArrayInputStream(dataCopy.toByteArray());
      BeanWithErrors beanWithErrors = beanFactory.createBean(copyIn, descrip, source, loader);

      if (beanWithErrors.errors() != null && beanWithErrors.errors().length > 0) {
        logger.debug("Configuration didn't parse; it had " + beanWithErrors.errors().length + " error(s).");

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < beanWithErrors.errors().length; ++i) {
          SAXParseException error = beanWithErrors.errors()[i];
          buf.append("  [" + i + "]: Line " + error.getLineNumber() + ", column " + error.getColumnNumber() + ": " + error.getMessage()
                     + "\n");
        }

        throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the Terracotta schema:\n"
                                              + buf);
      } else {
        logger.debug("Configuration is valid.");
      }

      tcConfigDoc = ((TcConfiguration) beanWithErrors.bean());
      this.providedTcConfigDocument = tcConfigDoc;
    } catch (IOException ioe) {
      throw new ConfigurationSetupException("We were unable to read configuration data from the " + descrip + ": "
                                            + ioe.getLocalizedMessage(), ioe);
    } catch (SAXException saxe) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " is not well-formed XML: "
                                            + saxe.getLocalizedMessage(), saxe);
    } catch (ParserConfigurationException pce) {
      throw Assert.failure("The XML parser can't be configured correctly; this should not happen.", pce);
    }

    return tcConfigDoc;
  }

  @Override
  public File directoryConfigurationLoadedFrom() {
    return directoryLoadedFrom;
  }

  @Override
  public boolean loadedFromTrustedSource() {
    return (baseConfigLoadedFromTrustedSource && (this.configurationSpec.shouldOverrideServerTopology() ? serverOverrideConfigLoadedFromTrustedSource
        : true));
  }

  @Override
  public String rawConfigText() {
    return this.providedTcConfigDocument.toString();
  }
  
  @Override
  public String source() {
    return this.configurationSpec.getBaseConfigSpec();
  }

  @Override
  public String describeSources() {
    return "The configuration specified by '" + baseConfigDescription + "'"
           + (this.serverOverrideConfigDescription == null ? "" : " and '" + this.serverOverrideConfigDescription + "'");
  }
}
