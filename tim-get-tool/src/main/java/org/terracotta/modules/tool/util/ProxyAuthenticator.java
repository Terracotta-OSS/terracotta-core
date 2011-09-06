/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.tool.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;

class ProxyAuthenticator extends Authenticator {
  private String username, password;

  public ProxyAuthenticator(String authString) {
    String[] parts = authString.split(":");
    if (parts.length != 2) { throw new RuntimeException(
                                                        "Failed to parse username:password from authentication string: "
                                                            + authString); }
    this.username = parts[0];
    this.password = parts[1];
  }

  public ProxyAuthenticator(String username, String password) {
    this.username = username;
    this.password = password;
  }

  protected PasswordAuthentication getPasswordAuthentication() {
    return new PasswordAuthentication(username, password.toCharArray());
  }
}