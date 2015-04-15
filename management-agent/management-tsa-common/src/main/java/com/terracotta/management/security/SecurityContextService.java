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
package com.terracotta.management.security;

/**
 * Created by lorban on 22/04/14.
 */
public interface SecurityContextService {

  public static class SecurityContext {
    private final String requestTicket;
    private final String signature;
    private final String alias;
    private final String token;

    public SecurityContext(String requestTicket, String signature, String alias, String token) {
      this.requestTicket = requestTicket;
      this.signature = signature;
      this.alias = alias;
      this.token = token;
    }

    public String getRequestTicket() {
      return requestTicket;
    }

    public String getSignature() {
      return signature;
    }

    public String getAlias() {
      return alias;
    }

    public String getToken() {
      return token;
    }
  }

  void setSecurityContext(SecurityContext securityContext);
  SecurityContext getSecurityContext();
  void clearSecurityContext();
}
