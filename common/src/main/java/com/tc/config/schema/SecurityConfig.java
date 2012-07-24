/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

public interface SecurityConfig extends Config {
  String getSslCertificateUri();
  String getKeyChainImplClass();
  String getSecretProviderImplClass();
  String getKeyChainUrl();
  String getRealmImplClass();
  String getRealmUrl();
  String getUser();
}
