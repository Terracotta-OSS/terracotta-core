/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.setup;

import org.apache.commons.io.CopyUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlInteger;
import org.xml.sax.SAXException;

import com.tc.config.schema.beanfactory.BeanWithErrors;
import com.tc.config.schema.beanfactory.ConfigBeanFactory;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.defaults.FromSchemaDefaultValueProvider;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.config.schema.repository.ApplicationsRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.sources.ConfigurationSource;
import com.tc.config.schema.setup.sources.FileConfigurationSource;
import com.tc.config.schema.setup.sources.ResourceConfigurationSource;
import com.tc.config.schema.setup.sources.ServerConfigurationSource;
import com.tc.config.schema.setup.sources.URLConfigurationSource;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.Assert;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link ConfigurationCreator} that works off XML files, using the standard config-spec model.
 */
public class StandardXMLFileConfigurationCreator implements ConfigurationCreator {

  private static final TCLogger     consoleLogger                        = CustomerLogging.getConsoleLogger();

  private static final long         GET_CONFIGURATION_TOTAL_TIMEOUT      = 5 * 60 * 1000;                     // five
  // minutes
  private static final long         GET_CONFIGURATION_ONE_SOURCE_TIMEOUT = 30 * 1000;                         // thirty
  // seconds
  private static final long         MIN_RETRY_TIMEOUT                    = 5 * 1000;                          // five
  // seconds

  protected final String            configurationSpec;
  protected final File              defaultDirectory;
  protected final ConfigBeanFactory beanFactory;

  private TCLogger                  logger;
  private boolean                   loadedFromTrustedSource;
  private String                    configDescription;
  private File                      directoryLoadedFrom;

  public StandardXMLFileConfigurationCreator(String configurationSpec, File defaultDirectory,
                                             ConfigBeanFactory beanFactory) {
    this(TCLogging.getLogger(StandardXMLFileConfigurationCreator.class), configurationSpec, defaultDirectory,
        beanFactory);
  }

  public StandardXMLFileConfigurationCreator(TCLogger logger, String configurationSpec, File defaultDirectory,
                                             ConfigBeanFactory beanFactory) {
    Assert.assertNotBlank(configurationSpec);
    Assert.assertNotNull(defaultDirectory);
    Assert.assertNotNull(beanFactory);

    this.logger = logger;
    this.configurationSpec = configurationSpec;
    this.defaultDirectory = defaultDirectory;
    this.beanFactory = beanFactory;
    this.configDescription = null;
  }

  private static final Pattern SERVER_PATTERN   = Pattern.compile("(.*):(.*)", Pattern.CASE_INSENSITIVE);

  private static final Pattern RESOURCE_PATTERN = Pattern.compile("resource://(.*)", Pattern.CASE_INSENSITIVE);

  // We require more than one character before the colon so that we don't mistake Windows-style directory paths as URLs.
  private static final Pattern URL_PATTERN      = Pattern.compile("[A-Za-z][A-Za-z]+://.*");

  private ConfigurationSource[] createConfigurationSources() throws ConfigurationSetupException {
    String[] components = configurationSpec.split(",");
    ConfigurationSource[] out = new ConfigurationSource[components.length];

    for (int i = 0; i < components.length; ++i) {
      String thisComponent = components[i];
      ConfigurationSource source = null;

      if (source == null) source = attemptToCreateServerSource(thisComponent);
      if (source == null) source = attemptToCreateResourceSource(thisComponent);
      if (source == null) source = attemptToCreateURLSource(thisComponent);
      if (source == null) source = attemptToCreateFileSource(thisComponent);

      if (source == null) {
        // formatting
        throw new ConfigurationSetupException("The location '" + thisComponent
            + "' is not in any recognized format -- it doesn't " + "seem to be a server, resource, URL, or file.");
      }

      out[i] = source;
    }

    return out;
  }

  private ConfigurationSource attemptToCreateServerSource(String text) {
    Matcher matcher = SERVER_PATTERN.matcher(text);
    if (matcher.matches()) {
      String host = matcher.group(1);
      String portText = matcher.group(2);

      try {
        return new ServerConfigurationSource(host, Integer.parseInt(portText));
      } catch (Exception e) {/**/
      }
    }
    return null;
  }

  private ConfigurationSource attemptToCreateResourceSource(String text) {
    Matcher matcher = RESOURCE_PATTERN.matcher(text);
    if (matcher.matches()) return new ResourceConfigurationSource(matcher.group(1), getClass());
    else return null;
  }

  private ConfigurationSource attemptToCreateFileSource(String text) {
    return new FileConfigurationSource(text, defaultDirectory);
  }

  private ConfigurationSource attemptToCreateURLSource(String text) {
    Matcher matcher = URL_PATTERN.matcher(text);
    if (matcher.matches()) return new URLConfigurationSource(text);
    else return null;
  }

  public void createConfigurationIntoRepositories(MutableBeanRepository l1BeanRepository,
                                                  MutableBeanRepository l2sBeanRepository,
                                                  MutableBeanRepository systemBeanRepository,
                                                  ApplicationsRepository applicationsRepository)
      throws ConfigurationSetupException {
    Assert.assertNotNull(l1BeanRepository);
    Assert.assertNotNull(l2sBeanRepository);
    Assert.assertNotNull(systemBeanRepository);
    Assert.assertNotNull(applicationsRepository);

    ConfigurationSource[] sources = createConfigurationSources();
    long startTime = System.currentTimeMillis();
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
          descrip = loadedSource.toString();
          break;
        }
      }

      if (out != null) break;

      ++iteration;

      boolean haveSources = false;
      for (int i = 0; i < remainingSources.length; ++i)
        haveSources = haveSources || remainingSources[i] != null;
      if (!haveSources) {
        // All sources have failed; bail out.
        break;
      }
    }

    if (out == null) configurationFetchFailed(sources, startTime);

    loadConfigurationData(out, trustedSource, descrip, l1BeanRepository, l2sBeanRepository, systemBeanRepository,
        applicationsRepository);
    consoleLogger.info("Configuration loaded from the " + descrip + ".");
  }

  private void configurationFetchFailed(ConfigurationSource[] sources, long startTime)
      throws ConfigurationSetupException {
    String text = "Could not fetch configuration data from ";
    if (sources.length > 1) text += "" + sources.length + " different configuration sources";
    else text += "the " + sources[0];
    text += ". ";

    if (sources.length > 1) {
      text += " The sources we tried are: ";
      for (int i = 0; i < sources.length; ++i) {
        if (i > 0) text += ", ";
        if (i == sources.length - 1) text += "and ";
        text += "the " + sources[i].toString();
      }
      text += ". ";
    }

    if (System.currentTimeMillis() - startTime >= GET_CONFIGURATION_TOTAL_TIMEOUT) {
      text += " Fetch attempt duration: " + ((System.currentTimeMillis() - startTime) / 1000) + " seconds.";
    }

    text += "\n\nTo correct this problem specify a valid configuration location using the ";
    text += "-f/--config command-line options.";

    consoleLogger.error(text);
    throw new ConfigurationSetupException(text);
  }

  private InputStream trySource(ConfigurationSource[] remainingSources, int i) {
    InputStream out = null;

    try {
      logger.info("Attempting to load configuration from the " + remainingSources[i] + "...");
      out = remainingSources[i].getInputStream(GET_CONFIGURATION_ONE_SOURCE_TIMEOUT);
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

    return out;
  }

  private void sleepIfNecessaryToAvoidPoundingSources(long lastLoopStartTime) {
    long delay = MIN_RETRY_TIMEOUT - (System.currentTimeMillis() - lastLoopStartTime);
    if (delay > 0) {
      try {
        logger.info("Waiting " + delay + " ms until we try to get configuration data again...");
        Thread.sleep(delay);
      } catch (InterruptedException ie) {
        // whatever
      }
    }
  }

  private void logCopyOfConfig(InputStream in, String descrip) throws IOException {
    StringWriter sw = new StringWriter();
    CopyUtils.copy(in, sw);

    logger.info("Successfully loaded configuration from the " + descrip + ". Config is:\n\n" + sw.toString());
  }

  private void loadConfigurationData(InputStream in, boolean trustedSource, String descrip,
                                     MutableBeanRepository clientBeanRepository,
                                     MutableBeanRepository serversBeanRepository,
                                     MutableBeanRepository systemBeanRepository,
                                     ApplicationsRepository applicationsRepository) throws ConfigurationSetupException {
    try {
      loadedFromTrustedSource = trustedSource;
      configDescription = descrip;

      ByteArrayOutputStream dataCopy = new ByteArrayOutputStream();
      CopyUtils.copy(in, dataCopy);

      logCopyOfConfig(new ByteArrayInputStream(dataCopy.toByteArray()), descrip);

      InputStream copyIn = new ByteArrayInputStream(dataCopy.toByteArray());
      BeanWithErrors beanWithErrors = beanFactory.createBean(copyIn, descrip);

      if (beanWithErrors.errors() != null && beanWithErrors.errors().length > 0) {
        logger.debug("Configuration didn't parse; it had " + beanWithErrors.errors().length + " error(s).");

        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < beanWithErrors.errors().length; ++i) {
          XmlError error = beanWithErrors.errors()[i];
          buf.append("  [" + i + "]: Line " + error.getLine() + ", column " + error.getColumn() + ": "
              + error.getMessage() + "\n");
        }

        throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
            + "Terracotta schema:\n" + buf);
      } else {
        logger.debug("Configuration is valid.");
      }

      TcConfig config = ((TcConfigDocument) beanWithErrors.bean()).getTcConfig();
      Servers servers = config.getServers();
      if (servers != null) {
        Server server;
        for (int i = 0; i < servers.sizeOfServerArray(); i++) {
          server = servers.getServerArray(i);
          // CDV-166: per our documentation in the schema itself, host is supposed to default to '%i' and name is
          // supposed to default to 'host:dso-port'
          if (!server.isSetHost() || server.getHost().trim().length() == 0) {
            server.setHost("%i");
          }
          if (!server.isSetName() || server.getName().trim().length() == 0) {
            int dsoPort = server.getDsoPort();
            if (dsoPort == 0) {
              // Find the default value, if we can
              final DefaultValueProvider defaultValueProvider = new FromSchemaDefaultValueProvider();
              if (defaultValueProvider.hasDefault(server.schemaType(), "dso-port")) {
                final XmlInteger defaultValue = (XmlInteger) defaultValueProvider.defaultFor(server.schemaType(),
                    "dso-port");
                dsoPort = defaultValue.getBigIntegerValue().intValue();
              }
            }
            server.setName(server.getHost() + (dsoPort > 0 ? ":" + dsoPort : ""));
          }
          // CDV-77: add parameter expansion to the <server> attributes ('host' and 'name')
          server.setHost(ParameterSubstituter.substitute(server.getHost()));
          server.setName(ParameterSubstituter.substitute(server.getName()));
        }
      }

      clientBeanRepository.setBean(config.getClients(), descrip);
      serversBeanRepository.setBean(config.getServers(), descrip);
      systemBeanRepository.setBean(config.getSystem(), descrip);

      if (config.isSetApplication()) {
        applicationsRepository.repositoryFor(TVSConfigurationSetupManagerFactory.DEFAULT_APPLICATION_NAME).setBean(
            config.getApplication(), descrip);
      }
    } catch (IOException ioe) {
      throw new ConfigurationSetupException("We were unable to read configuration data from the " + descrip + ": "
          + ioe.getLocalizedMessage(), ioe);
    } catch (SAXException saxe) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " is not well-formed XML: "
          + saxe.getLocalizedMessage(), saxe);
    } catch (ParserConfigurationException pce) {
      throw Assert.failure("The XML parser can't be configured correctly; this should not happen.", pce);
    } catch (XmlException xmle) {
      throw new ConfigurationSetupException("The configuration data in the " + descrip + " does not obey the "
          + "Terracotta schema: " + xmle.getLocalizedMessage(), xmle);
    }
  }

  public File directoryConfigurationLoadedFrom() {
    return directoryLoadedFrom;
  }

  public boolean loadedFromTrustedSource() {
    return loadedFromTrustedSource;
  }

  public String describeSources() {
    if (configDescription == null) {
      return "The configuration specified by '" + configurationSpec + "'";
    } else {
      return configDescription;
    }
  }

}
