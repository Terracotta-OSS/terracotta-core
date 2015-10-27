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

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vmad
 */
public class ClientConfigurationSetupManagerFactory {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private static final Pattern SERVER_PATTERN = Pattern.compile("(.*):(.*)", Pattern.CASE_INSENSITIVE);
  private final String[] args;
  private final List<String> stripeMemberUris;
  private final PwProvider securityManager;

  public ClientConfigurationSetupManagerFactory(String[] args, List<String> stripeMemberUris, PwProvider securityManager) {
    this.args = args;
    this.stripeMemberUris = stripeMemberUris;
    this.securityManager = securityManager;
  }

  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager(SecurityInfo securityInfo) throws ConfigurationSetupException {
    int memberCount = stripeMemberUris.size();
    String[] hosts = new String[memberCount];
    int[] ports = new int[memberCount];
    int index = 0;
    for (String stripeMemberUri : this.stripeMemberUris) {
      Matcher matcher = SERVER_PATTERN.matcher(stripeMemberUri);
      if (matcher.matches()) {
        String host = matcher.group(1);
        int userSeparatorIndex = host.indexOf('@');
        if (userSeparatorIndex > -1) {
          host = host.substring(userSeparatorIndex + 1);
        }
        int port = Integer.parseInt(matcher.group(2));
        hosts[index] = host;
        ports[index] = port;
      } else {
        String errMsg = "Invalid configuration URL: " + stripeMemberUri;
        consoleLogger.error(errMsg);
        throw new ConfigurationSetupException(errMsg);
      }
      index++;
    }
    return new ClientConfigurationSetupManager(this.stripeMemberUris, args, hosts, ports, securityInfo);
  }
}
