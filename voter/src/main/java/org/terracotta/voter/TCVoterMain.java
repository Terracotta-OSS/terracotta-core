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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.config.schema.setup.ConfigurationSetupException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.terracotta.voter.TCVoterMain.Opt.HELP;
import static org.terracotta.voter.TCVoterMain.Opt.OVERRIDE;
import static org.terracotta.voter.TCVoterMain.Opt.SERVER;

public class TCVoterMain {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterMain.class);
  
  enum Opt {
    HELP("h", "help"),
    OVERRIDE("o", "override"),
    SERVER("s", "server");

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
  }

  private static final String ID = UUID.randomUUID().toString();

  public void processArgs(String[] args) throws ConfigurationSetupException, ParseException {
    DefaultParser parser = new DefaultParser();
    Options voterOptions = voterOptions();
    CommandLine commandLine = parser.parse(voterOptions, args);

    if (commandLine.getArgList().size() > 0) {
      throw new ConfigurationSetupException("Invalid arguments provided: " + commandLine.getArgList());
    }

    if (commandLine.hasOption(HELP.getShortName())) {
      Options options = createHelpOptions();
      HelpFormatter helpFormatter = new HelpFormatter();
      helpFormatter.printHelp("start-voter.sh[bat]", options);
      return;
    }

    if (commandLine.getOptions().length == 0) {
      throw new ConfigurationSetupException("Neither the override option -o nor the regular options -s provided");
    }
    

    Optional<Properties> connectionProps = getConnectionProperties(commandLine);
    if (commandLine.hasOption(SERVER.getShortName())) {
      processServerArg(connectionProps, commandLine.getOptionValues(SERVER.getShortName()));
    } else if (commandLine.hasOption(OVERRIDE.getShortName())) {
      String hostPort = commandLine.getOptionValue(OVERRIDE.getShortName());
      validateHostPort(hostPort);
      getVoter(connectionProps).overrideVote(hostPort);
    } else {
      throw new AssertionError("This should not happen");
    }
  }

  protected Options createHelpOptions() {
    // creating new options with long name just for display purposes.
    Options helpOptions = new Options()
        .addOption(Option.builder(HELP.getLongName()).desc("Help").hasArg(false).build())
        .addOption(Option.builder(OVERRIDE.getLongName()).desc("Override vote").hasArg().argName("host:port").build())
        .addOption(Option.builder(SERVER.getLongName()).desc("Server host:port").hasArgs().argName("host:port[,host:port...]").valueSeparator().build());
    return helpOptions;
  }
  
  protected Optional<Properties> getConnectionProperties(CommandLine commandLine) {
    return Optional.empty();
  }

  protected void processServerArg(Optional<Properties> connectionProps, String[] stripes) throws ConfigurationSetupException {
    validateStripesLimit(SERVER.getShortName(), stripes);
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

  protected void validateStripesLimit(String option, String[] args) throws ConfigurationSetupException {
    if (args.length > 1) {
      throw new ConfigurationSetupException("Usage of multiple -" + option + " options not supported");
    }
  }

  protected Options voterOptions() {
    return new Options()
        .addOption(Option.builder(HELP.getShortName()).longOpt(HELP.getLongName()).desc("Help").hasArg(false).build())
        .addOption(Option.builder(OVERRIDE.getShortName()).longOpt(OVERRIDE.getLongName()).desc("Override vote").hasArg().argName("host:port").build())
        .addOption(Option.builder(SERVER.getShortName()).longOpt(SERVER.getLongName()).desc("Server host:port").hasArgs().argName("host:port[,host:port...]").valueSeparator().build());
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

  public static void main(String[] args) throws ConfigurationSetupException, ParseException {
    TCVoterMain main = new TCVoterMain();
    writePID();
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
