package com.terracotta.management.security.web.config;

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

  private static volatile KeyChainAccessor KEY_CHAIN_ACCESSOR;
  private static final Object KEY_CHAIN_ACCESSOR_LOCK = new Object();

  public static boolean isSslEnabled() {
    try {
      MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
      Object secure = mBeanServer.getAttribute(new ObjectName("org.terracotta.internal:type=Terracotta Server,name=Terracotta Server"), "Secure");
      return Boolean.TRUE.equals(secure);
    } catch (Exception e) {
      e.printStackTrace(System.out);
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
    return new DfltSSLContextFactory(getKeyChain(), null, null, false);
  }

  public static String getSecurityServiceLocation() {
    return "https://localhost:9443/tmc/api/assertIdentity";
  }

  public static Integer getSecurityTimeout() {
    return 10000;
  }

}
