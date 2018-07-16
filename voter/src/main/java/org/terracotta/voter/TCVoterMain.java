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
import org.xml.sax.SAXException;

import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TCVoterMain {

  private static final String HELP = "h";
  private static final String OVERRIDE = "o";
  protected static final String SERVER = "s";
  protected static final String CONFIG_FILE = "f";

  private static final String ID = UUID.randomUUID().toString();

  public TCVoter voter = new TCVoterImpl();

  public void processArgs(String[] args) throws ConfigurationSetupException, ParseException {
    DefaultParser parser = new DefaultParser();
    Options voterOptions = voterOptions();
    CommandLine commandLine = parser.parse(voterOptions, args);

    if (commandLine.getArgList().size() > 0) {
      throw new ConfigurationSetupException("Invalid arguments provided: " + commandLine.getArgList());
    }

    if (commandLine.hasOption(HELP)) {
      new HelpFormatter().printHelp("start-voter.sh[bat]", voterOptions);
      return;
    }

    if (commandLine.getOptions().length == 0) {
      throw new ConfigurationSetupException("Neither the override option -o nor the regular options -s or -f provided");
    }

    if (commandLine.hasOption(SERVER) && commandLine.hasOption(CONFIG_FILE)) {
      throw new ConfigurationSetupException("Both -s and -f options provided. Use either one and not both together.");
    }

    if (commandLine.hasOption(SERVER)) {
      processServerArg(commandLine.getOptionValues(SERVER));
    } else if (commandLine.hasOption(CONFIG_FILE)) {
      processConfigFileArg(commandLine.getOptionValues(CONFIG_FILE));
    } else if (commandLine.hasOption(OVERRIDE)) {
      String hostPort = commandLine.getOptionValue(OVERRIDE);
      validateHostPort(hostPort);
      voter.overrideVote(hostPort);
    } else {
      throw new AssertionError("This should not happen");
    }
  }

  protected void processServerArg(String[] stripes) throws ConfigurationSetupException {
    validateStripesLimit(SERVER, stripes);
    String[] hostPorts = stripes[0].split(",");
    for (String hostPort : hostPorts) {
      validateHostPort(hostPort);
    }
    startVoter(hostPorts);
  }

  protected void processConfigFileArg(String[] stripes) throws ConfigurationSetupException {
    validateStripesLimit(CONFIG_FILE, stripes);

    TCConfigParserUtil parser = new TCConfigParserUtil();
    String[] hostPorts;
    try {
      hostPorts = parser.parseHostPorts(new FileInputStream(stripes[0]));
    } catch (SAXException | IOException e) {
      throw new ConfigurationSetupException(e);
    }
    startVoter(hostPorts);
  }

  protected void startVoter(String... hostPorts) {
    new ActiveVoter(ID, new CompletableFuture<>(), hostPorts).start();
  }

  protected void validateStripesLimit(String option, String[] args) throws ConfigurationSetupException {
    if (args.length > 1) {
      throw new ConfigurationSetupException("Usage of multiple -" + option + " options not supported");
    }
  }

  private Options voterOptions() {
    return new Options()
        .addOption(Option.builder(HELP).desc("Help").hasArg(false).build())
        .addOption(Option.builder(OVERRIDE).desc("Override vote").hasArg().argName("host:port").build())
        .addOption(Option.builder(CONFIG_FILE).desc("Server configuration file").hasArgs().argName("tc-config path").build())
        .addOption(Option.builder(SERVER).desc("Server host:port").hasArgs().argName("host:port[,host:port...]").valueSeparator().build());
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
    main.processArgs(args);
  }

}
