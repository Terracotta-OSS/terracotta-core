package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class Auth {

  private String realm;
  private String url;


  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }

  public Auth realm(String realm) {
    setRealm(realm);
    return this;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Auth url(String url) {
    setUrl(url);
    return this;
  }

}
