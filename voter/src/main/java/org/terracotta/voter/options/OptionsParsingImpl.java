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
package org.terracotta.voter.options;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.tc.config.schema.setup.ConfigurationSetupException;

import java.util.List;

@Parameters
@Usage("(-connect-to <hostname:port>,<hostname:port>... -connect-to <hostname:port>,<hostname:port> ... | -vote-for <hostname:port>))")
public class OptionsParsingImpl implements OptionsParsing {

  @Parameter(names = {"-help", "-h"}, description = "Help", help = true)
  private boolean help;

  @Parameter(names = {"-vote-for", "-o"}, description = "Override vote to host:port")
  private String overrideHostPort;

  @Parameter(names = {"-connect-to", "-s"}, description = "Comma separated host:port to connect to (one per stripe)", splitter = NoCommaSplitter.class)
  private List<String> serversHostPort;

  @Override
  public Options process() throws ConfigurationSetupException {
    validateOptions();
    Options options = new Options();
    options.setHelp(help);
    options.setOverrideHostPort(overrideHostPort);
    options.setServerHostPort(serversHostPort);
    return options;
  }

  private void validateOptions() throws ConfigurationSetupException {
    if (!help) {
      if (overrideHostPort == null && serversHostPort == null) {
        throw new ConfigurationSetupException("Neither the -vote-for option nor the regular -connect-to option provided");
      } else if (overrideHostPort != null && serversHostPort != null) {
        throw new ConfigurationSetupException("Either the -vote-for or the regular -connect-to option can be provided");
      }
    }
  }
}
