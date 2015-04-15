/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.client;

import java.util.Set;

public class TerracottaClientConfig {

  private final String      tcConfigSnippetOrUrl;
  private final boolean     isUrl;
  private final Set<String> tunnelledMBeanDomains;
  private final boolean     rejoin;
  private final boolean     nonStop;
  private final String      productId;
  private final ClassLoader classLoader;
  private final boolean     asyncInit;

  TerracottaClientConfig(TerracottaClientConfigParams params) {
    this.tcConfigSnippetOrUrl = params.getTcConfigSnippetOrUrl();
    this.isUrl = params.isUrl();
    this.tunnelledMBeanDomains = params.getTunnelledMBeanDomains();
    this.rejoin = params.isRejoin();
    this.nonStop = params.isNonStop();
    this.productId = params.getProductId();
    this.classLoader = params.getClassLoader();
    this.asyncInit = params.isAsyncInit();
  }

  public String getTcConfigSnippetOrUrl() {
    return tcConfigSnippetOrUrl;
  }

  public boolean isUrl() {
    return isUrl;
  }

  public Set<String> getTunnelledMBeanDomains() {
    return tunnelledMBeanDomains;
  }

  public boolean isRejoin() {
    return rejoin;
  }

  public boolean isNonStopEnabled() {
    return nonStop;
  }

  public String getProductId() {
    return productId;
  }

  public ClassLoader getClassLoader() {
    return classLoader;
  }

  public boolean isAsyncInit() {
    return this.asyncInit;
  }
}
