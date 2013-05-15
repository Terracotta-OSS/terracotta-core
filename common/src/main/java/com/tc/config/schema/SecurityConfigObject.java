/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.Management;
import com.terracottatech.config.Security;
import com.terracottatech.config.Ssl;
import static com.terracotta.management.security.SecretUtils.*;

public class SecurityConfigObject extends BaseConfigObject implements SecurityConfig {

  public static final String VM_ARG_KEYCHAIN_SECRET_PROVIDER = System.getProperty(TERRACOTTA_CUSTOM_SECRET_PROVIDER_PROP);
  public static final String VM_ARG_KEYCHAIN_IMPL = System.getProperty(TERRACOTTA_KEYCHAIN_IMPL_CLASS_PROP);
  public static final String VM_ARG_KEYCHAIN_URL = System.getProperty(TERRACOTTA_KEYCHAIN_LOCATION_PROP);


  public SecurityConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Security.class);
  }

  @Override
  public String getSslCertificateUri() {
    Security bean = (Security)this.context.bean();
    if (bean == null) { return null; }
    Ssl ssl = bean.getSsl();
    if (ssl == null) { return null; }
    return ssl.getCertificate();
  }

  @Override
  public String getKeyChainImplClass() {
    if(VM_ARG_KEYCHAIN_IMPL != null) {
      return VM_ARG_KEYCHAIN_IMPL;
    }
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getClass1();
  }

  @Override
  public String getSecretProviderImplClass() {
    if(VM_ARG_KEYCHAIN_SECRET_PROVIDER != null) {
      return VM_ARG_KEYCHAIN_SECRET_PROVIDER;
    }
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getSecretProvider();
  }

  @Override
  public String getKeyChainUrl() {
    if(VM_ARG_KEYCHAIN_URL != null) {
      return VM_ARG_KEYCHAIN_URL;
    }
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getUrl();
  }

  @Override
  public String getRealmImplClass() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getRealm();
  }

  @Override
  public String getRealmUrl() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getUrl();
  }

  @Override
  public String getUser() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getUser();
  }

  @Override
  public String getSecurityServiceLocation() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getIa();
  }

  @Override
  public Integer getSecurityServiceTimeout() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getTimeout();
  }

  @Override
  public String getSecurityHostname() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getHostname();
  }
}
