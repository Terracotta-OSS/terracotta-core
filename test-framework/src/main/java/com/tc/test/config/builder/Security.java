package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class Security {

  private Ssl ssl;
  private Keychain keychain;
  private Auth auth;
  private Management management;

  public Ssl getSsl() {
    return ssl;
  }

  public void setSsl(Ssl ssl) {
    this.ssl = ssl;
  }

  public Security ssl(Ssl ssl) {
    setSsl(ssl);
    return this;
  }

  public Keychain getKeychain() {
    return keychain;
  }

  public void setKeychain(Keychain keychain) {
    this.keychain = keychain;
  }

  public Security keychain(Keychain keychain) {
    setKeychain(keychain);
    return this;
  }

  public Auth getAuth() {
    return auth;
  }

  public void setAuth(Auth auth) {
    this.auth = auth;
  }

  public Security auth(Auth auth) {
    setAuth(auth);
    return this;
  }

  public Management getManagement() {
    return management;
  }

  public void setManagement(Management management) {
    this.management = management;
  }

  public Security management(Management management) {
    setManagement(management);
    return this;
  }

}
