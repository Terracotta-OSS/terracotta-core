/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.client;

import java.util.Set;

public class TerracottaClientConfigParams {
  private String      tcConfigSnippetOrUrl;
  private boolean     isUrl;
  private Set<String> tunnelledMBeanDomains;
  private boolean     rejoin;
  private boolean     nonStop;
  private String      productId;
  private ClassLoader clasLoader;
  private boolean     asyncInit;

  public String getTcConfigSnippetOrUrl() {
    return tcConfigSnippetOrUrl;
  }

  public void setTcConfigSnippetOrUrl(String tcConfigSnippetOrUrl) {
    this.tcConfigSnippetOrUrl = tcConfigSnippetOrUrl;
  }

  public TerracottaClientConfigParams tcConfigSnippetOrUrl(String tcConfigSnippetOrUrlParam) {
    this.tcConfigSnippetOrUrl = tcConfigSnippetOrUrlParam;
    return this;
  }

  public boolean isUrl() {
    return isUrl;
  }

  public void setUrl(boolean isUrl) {
    this.isUrl = isUrl;
  }

  public TerracottaClientConfigParams isUrl(boolean isUrlParam) {
    this.isUrl = isUrlParam;
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

  public TerracottaClientConfig newTerracottaClientConfig() {
    return new TerracottaClientConfig(this);
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(final String productId) {
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
  
  public void setAsyncInit(boolean asyncInit) {
    this.asyncInit = asyncInit;
  }

  public boolean isAsyncInit() {
    return this.asyncInit;
  }

}
