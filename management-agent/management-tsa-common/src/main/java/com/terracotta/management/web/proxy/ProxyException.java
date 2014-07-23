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
