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
package com.terracotta.management.web.proxy;

/**
 * Thrown when a request must be processed by another server, it will then be proxied by ProxyExceptionMapper.
 *
 * @author Ludovic Orban
 */
public class ProxyException extends RuntimeException {
  private final String activeL2WithMBeansUrl;

  public ProxyException(String activeL2WithMBeansUrl) {
    this.activeL2WithMBeansUrl = activeL2WithMBeansUrl;
  }

  public String getActiveL2WithMBeansUrl() {
    return activeL2WithMBeansUrl;
  }
}
