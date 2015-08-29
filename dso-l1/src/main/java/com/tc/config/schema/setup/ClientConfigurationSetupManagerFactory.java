package com.tc.config.schema.setup;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author vmad
 */
public class ClientConfigurationSetupManagerFactory {

  private static final TCLogger consoleLogger = CustomerLogging.getConsoleLogger();
  private static final Pattern SERVER_PATTERN = Pattern.compile("(.*):(.*)", Pattern.CASE_INSENSITIVE);
  private final String[] args;
  private final String configurationSpec;
  private final PwProvider securityManager;

  public ClientConfigurationSetupManagerFactory(String[] args, String configurationSpec, PwProvider securityManager) {
    this.args = args;
    this.configurationSpec = configurationSpec;
    this.securityManager = securityManager;
  }

  public L1ConfigurationSetupManager getL1TVSConfigurationSetupManager(SecurityInfo securityInfo) throws ConfigurationSetupException {
    String[] configUrls = configurationSpec.split(",");
    String[] hosts = new String[configUrls.length];
    int[] ports = new int[configUrls.length];
    int index = 0;
    for(String configUrl : configUrls) {
      Matcher matcher = SERVER_PATTERN.matcher(configUrl);
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
        String errMsg = "Invalid configuration URL: " + configUrl;
        consoleLogger.error(errMsg);
        throw new ConfigurationSetupException(errMsg);
      }
      index++;
    }
    return new ClientConfigurationSetupManager(configurationSpec, args, hosts, ports, securityInfo);
  }
}
