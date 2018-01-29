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
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.terracotta.config.Server;
import org.terracotta.config.TCConfigurationParser;
import org.terracotta.config.TcConfiguration;
import org.xml.sax.SAXException;

import com.tc.config.schema.setup.ConfigurationSetupException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class TCVoterMain {

  private static final String HELP = "h";
  private static final String CLUSTER_NAME = "n";
  private static final String SERVER = "s";
  private static final String CONFIG_FILE = "f";
  private static final String VETO = "v";

  public static TCVoter voter = new TCVoterImpl();

  public static void main(String[] args) throws ConfigurationSetupException {
    CommandLine commandLine = null;
    Options options = voterOptions();
    try {
      commandLine = new PosixParser().parse(options, args);
    } catch (ParseException pe) {
      throw new ConfigurationSetupException(pe.getLocalizedMessage(), pe);
    }

    if (commandLine.getArgList().size() > 0) {
      throw new ConfigurationSetupException("Invalid arguments provided: " + commandLine.getArgList());
    }

    if (commandLine.hasOption(HELP)) {
      new HelpFormatter().printHelp("start-voter.sh[bat]", options);
    } else {
      if (!commandLine.hasOption(VETO) && !commandLine.hasOption(CLUSTER_NAME)) {
        throw new ConfigurationSetupException("Neither the veto option -v nor the regular options with -n and -s or -f provided");
      }

      if (commandLine.hasOption(VETO)) {
        String vetoTarget = commandLine.getOptionValue(VETO);
        validatedHostPort(vetoTarget);
        voter.vetoVote(vetoTarget);
      }

      if (commandLine.hasOption(CLUSTER_NAME)) {
        if (commandLine.hasOption(SERVER) && commandLine.hasOption(CONFIG_FILE)) {
          throw new ConfigurationSetupException("Both -s and -f options provided. Use either one and not both together.");
        }
        String clusterName = commandLine.getOptionValue(CLUSTER_NAME);
        if (commandLine.hasOption(SERVER)) {
          String[] hostPorts = commandLine.getOptionValues(SERVER);
          for (String hostPort : hostPorts) {
            validatedHostPort(hostPort);
          }
          voter.register(clusterName, hostPorts);
        } else if (commandLine.hasOption(CONFIG_FILE)) {
          String[] hostPorts = getServerHostPortsFromConfig(commandLine.getOptionValue(CONFIG_FILE));
          voter.register(clusterName, hostPorts);
        } else {
          throw new ConfigurationSetupException("Neither -s nor -f option provided with -n option");
        }
      }
    }
  }

  private static String[] getServerHostPortsFromConfig(String tcConfigPath) throws ConfigurationSetupException {
    TcConfiguration config;
    try {
      config = TCConfigurationParser.parse(new File(tcConfigPath));
    } catch (IOException | SAXException e) {
      throw new ConfigurationSetupException("Failed to parse the configuration file at : " + tcConfigPath);
    }

    List<Server> servers = config.getPlatformConfiguration().getServers().getServer();
    return servers.stream()
        .map(s -> s.getHost() + ":" + s.getTsaPort().getValue())
        .collect(toList())
        .toArray(new String[0]);
  }

  private static Options voterOptions() {
    return new Options()
        .addOption(HELP, "help", false, "Help")
        .addOption(CLUSTER_NAME, "name", true, "Cluster name")
        .addOption(SERVER, "server", true, "Server host:port")
        .addOption(CONFIG_FILE, "file", true, "Terracotta server configuration file")
        .addOption(VETO, "veto", true, "Veto vote");
  }

  private static void validatedHostPort(String hostPort) throws ConfigurationSetupException {
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
}
