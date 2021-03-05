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
package org.terracotta.voter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.schema.setup.ConfigurationSetupException;
import org.terracotta.voter.options.Options;
import org.terracotta.voter.options.OptionsParsing;
import org.terracotta.voter.options.OptionsParsingImpl;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TCVoterMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterMain.class);

  private static final String ID = UUID.randomUUID().toString();

  public void processArgs(String[] args) throws ConfigurationSetupException {
    OptionsParsing optionsParsing = getParsingObject();
    CustomJCommander jCommander = new CustomJCommander(optionsParsing);
    jCommander.parse(args);
    Options options = optionsParsing.process();

    if (options.isHelp()) {
      jCommander.usage();
      return;
    }

    writePID();
    Optional<Properties> connectionProps = getConnectionProperties(options);
    if (options.getServersHostPort() != null) {
      processServerArg(connectionProps, options.getServersHostPort().toArray(new String[0]));
    } else if (options.getOverrideHostPort() != null) {
      String hostPort = options.getOverrideHostPort();
      validateHostPort(hostPort);
      getVoter(connectionProps).overrideVote(hostPort);
    } else {
      throw new AssertionError("This should not happen");
    }
  }

  protected OptionsParsing getParsingObject() {
    return new OptionsParsingImpl();
  }
  
  protected Optional<Properties> getConnectionProperties(Options option) {
    return Optional.empty();
  }

  protected void processServerArg(Optional<Properties> connectionProps, String[] stripes) throws ConfigurationSetupException {
    validateStripesLimit(stripes);
    String[] hostPorts = stripes[0].split(",");
    for (String hostPort : hostPorts) {
      validateHostPort(hostPort);
    }
    startVoter(connectionProps, hostPorts);
  }
  
  protected TCVoter getVoter(Optional<Properties> connectionProps) {
    return new TCVoterImpl();
  }

  protected void startVoter(Optional<Properties> connectionProps, String... hostPorts) {
    new ActiveVoter(ID, new CompletableFuture<>(), connectionProps, hostPorts).start();
  }

  protected void validateStripesLimit(String[] args) throws ConfigurationSetupException {
    if (args.length > 1) {
      throw new ConfigurationSetupException("Usage of multiple -connect-to options not supported");
    }
  }

  protected void validateHostPort(String hostPort) throws ConfigurationSetupException {
    URI uri;
    try {
      uri = new URI("tc://" + hostPort);
    } catch (URISyntaxException e) {
      throw new ConfigurationSetupException(e);
    }

    if (uri.getHost() == null || uri.getPort() == -1) {
      throw new ConfigurationSetupException("Invalid host:port combination provided: " + hostPort);
    }
  }

  public static void main(String[] args) throws ConfigurationSetupException {
    TCVoterMain main = new TCVoterMain();
    main.processArgs(args);
  }

  private static void writePID() {
    try {
      String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
      long pid = Long.parseLong(processName.split("@")[0]);
      LOGGER.info("PID is {}", pid);
    } catch (Throwable t) {
      LOGGER.warn("Unable to fetch the PID of this process.");
    }
  }
}
