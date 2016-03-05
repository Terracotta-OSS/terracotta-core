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
package com.terracotta.connection.client;

import java.util.Collections;
import java.util.List;
import java.util.Vector;


public class TerracottaClientConfigParams {
  private final List<String> stripeMembers;
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
