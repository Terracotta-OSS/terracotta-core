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
