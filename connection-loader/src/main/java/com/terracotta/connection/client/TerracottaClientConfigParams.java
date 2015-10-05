/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection.client;

import java.util.Set;

public class TerracottaClientConfigParams {
  private String      tcConfigUrl;
  private Set<String> tunnelledMBeanDomains;
  private boolean     rejoin;
  private boolean     nonStop;
  private String      productId;
  private ClassLoader clasLoader;

  public String getTcConfigUrl() {
    return this.tcConfigUrl;
  }

  public TerracottaClientConfigParams tcConfigUrl(String tcConfigUrlParam) {
    this.tcConfigUrl = tcConfigUrlParam;
    return this;
  }

  public Set<String> getTunnelledMBeanDomains() {
    return tunnelledMBeanDomains;
  }

  public void setTunnelledMBeanDomains(Set<String> tunnelledMBeanDomains) {
    this.tunnelledMBeanDomains = tunnelledMBeanDomains;
  }

  public TerracottaClientConfigParams tunnelledMBeanDomains(Set<String> tunnelledMBeanDomainsParams) {
    this.tunnelledMBeanDomains = tunnelledMBeanDomainsParams;
    return this;
  }

  public boolean isRejoin() {
    return rejoin;
  }

  public void setRejoin(boolean rejoin) {
    this.rejoin = rejoin;
  }

  public TerracottaClientConfigParams rejoin(boolean rejoinParam) {
    this.rejoin = rejoinParam;
    return this;
  }

  public TerracottaClientConfigParams nonStopEnabled(boolean nonStopParam) {
    this.nonStop = nonStopParam;
    return this;
  }

  public boolean isNonStop() {
    return nonStop;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public TerracottaClientConfigParams productId(String appName) {
    setProductId(appName);
    return this;
  }

  public ClassLoader getClassLoader() {
    return clasLoader;
  }

  public TerracottaClientConfigParams classLoader(ClassLoader loader) {
    setClassLoader(loader);
    return this;
  }

  public void setClassLoader(ClassLoader loader) {
    this.clasLoader = loader;
  }

}
