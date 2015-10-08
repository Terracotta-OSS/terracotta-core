/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.connection.client;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;


public class TerracottaClientConfigParams {
  private final List<String> stripeMembers;
  private Set<String> tunnelledMBeanDomains;
  private boolean     rejoin;
  private boolean     nonStop;
  private String      productId;
  private ClassLoader clasLoader;

  public TerracottaClientConfigParams() {
    this.stripeMembers = new Vector<String>();
  }

  public List<String> getStripeMemberUris() {
    return Collections.unmodifiableList(this.stripeMembers);
  }

  public TerracottaClientConfigParams addStripeMemberUri(String stripeMember) {
    this.stripeMembers.add(stripeMember);
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
