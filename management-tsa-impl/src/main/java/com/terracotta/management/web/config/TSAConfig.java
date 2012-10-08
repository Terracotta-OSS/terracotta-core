/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.web.config;

import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.KeychainInitializationException;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.impl.DfltSSLContextFactory;
import com.terracotta.management.security.impl.ObfuscatedSecretFileStoreKeyChainAccessor;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class TSAConfig {

  private static final String DEFAULT_IA_URL = "https://localhost:9443/tmc/api/assertIdentity";
  private static final int DEFAULT_TIMEOUT = 10000;

  private static volatile KeyChainAccessor KEY_CHAIN_ACCESSOR;
  private static final Object KEY_CHAIN_ACCESSOR_LOCK = new Object();
  private static volatile SSLContextFactory SSL_CONTEXT_FACTORY;
  private static final Object SSL_CONTEXT_FACTORY_LOCK = new Object();

  public static boolean isSslEnabled() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object secure = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "Secure");
      return Boolean.TRUE.equals(secure);
    } catch (Exception e) {
      return false;
    }
  }

  public static KeyChainAccessor getKeyChain() throws KeychainInitializationException {
    if (KEY_CHAIN_ACCESSOR == null) {
      synchronized (KEY_CHAIN_ACCESSOR_LOCK) {
        if (KEY_CHAIN_ACCESSOR == null) {
          KEY_CHAIN_ACCESSOR = new ObfuscatedSecretFileStoreKeyChainAccessor();
        }
      }
    }
    return KEY_CHAIN_ACCESSOR;
  }

  public static SSLContextFactory getSSLContextFactory() throws KeychainInitializationException {
    if (SSL_CONTEXT_FACTORY == null) {
      synchronized (SSL_CONTEXT_FACTORY_LOCK) {
        if (SSL_CONTEXT_FACTORY == null) {
          SSL_CONTEXT_FACTORY = new DfltSSLContextFactory(getKeyChain(), null, null, false);
        }
      }
    }
    return SSL_CONTEXT_FACTORY;
  }

  public static String getSecurityServiceLocation() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object response = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityServiceLocation");

      if (response == null) {
        return DEFAULT_IA_URL;
      }

      return (String)response;
    } catch (Exception e) {
      throw new RuntimeException("Error getting SecurityServiceLocation", e);
    }
  }

  public static Integer getSecurityTimeout() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object response = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityServiceTimeout");

      if (response == null) {
        return DEFAULT_TIMEOUT;
      }

      return (Integer)response;
    } catch (Exception e) {
      throw new RuntimeException("Error getting SecurityServiceTimeout", e);
    }
  }

  public static String getSecurityCallbackUrl() {
    if (!isSslEnabled()) {
      return null;
    }

    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object securityHostnameAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "SecurityHostname");
      Object dsoListenPortAttribute = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "DSOListenPort");

      return "https://" + securityHostnameAttribute + ":" + dsoListenPortAttribute + "/tc-management-api/assertIdentity";
    } catch (Exception e) {
      throw new RuntimeException("Error building SecurityCallbackUrl", e);
    }
  }

}
