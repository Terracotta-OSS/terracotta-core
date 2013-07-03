package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class Keychain {

  private String url;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Keychain url(String url) {
    setUrl(url);
    return this;
  }

}
