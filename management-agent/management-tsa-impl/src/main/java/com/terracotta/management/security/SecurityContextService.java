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
