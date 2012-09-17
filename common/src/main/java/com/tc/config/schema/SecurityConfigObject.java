/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import com.tc.config.schema.context.ConfigContext;
import com.terracottatech.config.Management;
import com.terracottatech.config.Security;
import com.terracottatech.config.Ssl;

public class SecurityConfigObject extends BaseConfigObject implements SecurityConfig {

  public SecurityConfigObject(ConfigContext context) {
    super(context);
    context.ensureRepositoryProvides(Security.class);
  }

  public String getSslCertificateUri() {
    Security bean = (Security)this.context.bean();
    if (bean == null) { return null; }
    Ssl ssl = bean.getSsl();
    if (ssl == null) { return null; }
    return ssl.getCertificate();
  }

  public String getKeyChainImplClass() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getClass1();
  }

  public String getSecretProviderImplClass() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getSecretProvider();
  }

  public String getKeyChainUrl() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getKeychain().getUrl();
  }

  public String getRealmImplClass() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getRealm();
  }

  public String getRealmUrl() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getUrl();
  }

  public String getUser() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    return bean.getAuth().getUser();
  }

  public String getSecurityServiceLocation() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getIa();
  }

  public Integer getSecurityServiceTimeout() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getTimeout();
  }

  public String getSecurityHostname() {
    final Security bean = (Security)this.context.bean();
    if(bean == null) { return null; }
    Management management = bean.getManagement();
    if(management == null) { return null; }
    return management.getHostname();
  }
}
