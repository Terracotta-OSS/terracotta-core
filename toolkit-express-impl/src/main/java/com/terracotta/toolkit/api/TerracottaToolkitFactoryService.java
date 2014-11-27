/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.api;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitInstantiationException;
import org.terracotta.toolkit.api.ToolkitFactoryService;

import com.terracotta.toolkit.client.TerracottaClientConfig;
import com.terracotta.toolkit.client.TerracottaClientConfigParams;
import com.terracotta.toolkit.client.TerracottaToolkitCreator;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class TerracottaToolkitFactoryService implements ToolkitFactoryService {

  private final static String     TERRACOTTA_TOOLKIT_TYPE          = "terracotta";
  private final static String     NON_STOP_TERRACOTTA_TOOLKIT_TYPE = "nonstop-terracotta";
  private static final String     TUNNELLED_MBEAN_DOMAINS_KEY      = "tunnelledMBeanDomains";
  private static final String     TC_CONFIG_SNIPPET_KEY            = "tcConfigSnippet";
  private static final String     REJOIN_KEY                       = "rejoin";
  private static final String     PRODUCT_ID_KEY                   = "productId";
  private static final String     CLASSLOADER                      = "classloader";
  private static final Properties EMPTY_PROPERTIES                 = new Properties();
  private static final boolean    NONSTOP_INIT_ENABLED             = Boolean.getBoolean("toolkit.nonstop.init.enabled");

  @Override
  public boolean canHandleToolkitType(String type, String subName) {
    return TERRACOTTA_TOOLKIT_TYPE.equals(type) || NON_STOP_TERRACOTTA_TOOLKIT_TYPE.equals(type);
  }

  @Override
  public Toolkit createToolkit(String type, String subName, Properties properties) throws ToolkitInstantiationException {
    properties = properties == null ? EMPTY_PROPERTIES : properties;
    if (!canHandleToolkitType(type, subName)) {
      //
      throw new ToolkitInstantiationException("Cannot handle toolkit of type: " + type + ", subName: " + subName);
    }
    try {
      return createToolkit(createTerracottaClientConfig(type, subName, properties), properties);
    } catch (Throwable t) {
      if (t instanceof ToolkitInstantiationException) {
        throw (ToolkitInstantiationException) t;
      } else {
        throw new ToolkitInstantiationException(t);
      }
    }
  }

  /**
   * Overridden by enterprise to create enterprise toolkit
   */
  protected Toolkit createToolkit(TerracottaClientConfig config, Properties properties) {
    return new TerracottaToolkitCreator(config, properties, false).createToolkit();
  }

  private TerracottaClientConfig createTerracottaClientConfig(String type, String subName, Properties properties)
      throws ToolkitInstantiationException {
    TerracottaClientConfigParams terracottaClientConfigParams = new TerracottaClientConfigParams()
        .rejoin(isRejoinEnabled(properties)).nonStopEnabled(isNonStopEnabled(type))
        .classLoader(getClassLoader(properties));
    terracottaClientConfigParams.setAsyncInit(NONSTOP_INIT_ENABLED);
    String tcConfigSnippet = properties.getProperty(TC_CONFIG_SNIPPET_KEY, "");
    if (tcConfigSnippet == null || tcConfigSnippet.trim().equals("")) {
      // if no tcConfigSnippet, assume url
      terracottaClientConfigParams.tcConfigSnippetOrUrl(getTerracottaUrlFromSubName(subName)).isUrl(true);
    } else {
      terracottaClientConfigParams.tcConfigSnippetOrUrl(tcConfigSnippet).isUrl(false);
    }
    terracottaClientConfigParams.tunnelledMBeanDomains(getTunnelledMBeanDomains(properties));

    if (properties.containsKey(PRODUCT_ID_KEY)) {
      terracottaClientConfigParams.productId(properties.getProperty(PRODUCT_ID_KEY));
    }

    return terracottaClientConfigParams.newTerracottaClientConfig();
  }

  private ClassLoader getClassLoader(Properties properties) {
    if (properties == null) { return null; }
    return (ClassLoader) properties.get(CLASSLOADER);
  }

  private boolean isNonStopEnabled(String type) {
    return type.equals(NON_STOP_TERRACOTTA_TOOLKIT_TYPE);
  }

  private boolean isRejoinEnabled(Properties properties) {
    if (properties == null || properties.size() == 0) { return false; }
    String rejoin = properties.getProperty(REJOIN_KEY);
    return "true".equals(rejoin);
  }

  private String getTerracottaUrlFromSubName(String subName) throws ToolkitInstantiationException {
    StringBuilder tcUrl = new StringBuilder();

    // toolkitUrl is of form: 'toolkit:terracotta://server:tsa-port'
    if (subName == null || !subName.startsWith("//")) {
      //
      throw new ToolkitInstantiationException(
                                              "'subName' in toolkitUrl for toolkit type 'terracotta' should start with '//', "
                                                  + "and should be of form: 'toolkit:terracotta://server:tsa-port' - "
                                                  + subName);
    }
    String terracottaUrl = subName.substring(2);
    // terracottaUrl can only be form of server:port, or a csv of same
    if (terracottaUrl == null || terracottaUrl.equals("")) {
      //
      throw new ToolkitInstantiationException(
                                              "toolkitUrl should be of form: 'toolkit:terracotta://server:tsa-port', server:port not specified after 'toolkit:terracotta://' in toolkitUrl - "
                                                  + subName);
    }
    // ignore last comma, if any
    terracottaUrl = terracottaUrl.trim();
    String[] serverPortTokens = terracottaUrl.split(",");
    for (String serverPortToken : serverPortTokens) {
      if (serverPortToken.equals("")) {
        continue;
      }
      String[] tokens = serverPortToken.split(":");
      if (tokens.length != 2) {
        //
        throw new ToolkitInstantiationException(
                                                "toolkitUrl should be of form: 'toolkit:terracotta://server:tsa-port', invalid server:port specified - '"
                                                    + serverPortToken + "'");
      }
      if (!isValidInteger(tokens[1])) {
        //
        throw new ToolkitInstantiationException(
                                                "toolkitUrl should be of form: 'toolkit:terracotta://server:tsa-port', invalid server:port specified in token - '"
                                                    + serverPortToken + "', 'port' is not a valid integer - "
                                                    + tokens[1]);
      }
      tcUrl.append(tokens[0] + ":");
      tcUrl.append(tokens[1] + ",");
    }
    tcUrl.deleteCharAt(tcUrl.lastIndexOf(","));
    return tcUrl.toString();
  }

  private boolean isValidInteger(String value) {
    try {
      Integer.parseInt(value);
    } catch (Exception e) {
      return false;
    }
    return true;
  }

  private static Set<String> getTunnelledMBeanDomains(Properties properties) {
    if (properties == null || properties.size() == 0) { return Collections.EMPTY_SET; }
    String domainsCSV = properties.getProperty(TUNNELLED_MBEAN_DOMAINS_KEY);
    if (domainsCSV == null || domainsCSV.equals("")) { return Collections.EMPTY_SET; }
    return Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(domainsCSV.split(","))));
  }

}
