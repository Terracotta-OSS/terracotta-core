package com.terracotta.management.web.proxy;

/**
 * Thrown when a request must be processed by another server, it will then be proxied by ProxyExceptionMapper.
 *
 * @author Ludovic Orban
 */
public class ProxyException extends RuntimeException {
  private final String activeL2Url;

  public ProxyException(String activeL2Url) {
    this.activeL2Url = activeL2Url;
  }

  public String getActiveL2Url() {
    return activeL2Url;
  }
}
