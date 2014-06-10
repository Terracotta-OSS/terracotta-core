package com.tc.security;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Alex Snaps
 */
public class PwProviderUtil {

  private static AtomicReference<PwProvider> backend = new AtomicReference<PwProvider>();

  public static char[] getPasswordTo(URI uri) {
    final PwProvider tcSecurityManager = backend.get();
    if(tcSecurityManager == null) {
      throw new IllegalStateException("We haven't had a BackEnd set yet!");
    }
    return tcSecurityManager.getPasswordFor(uri);
  }

  public static void setBackEnd(final PwProvider securityManager) {
    if(!PwProviderUtil.backend.compareAndSet(null, securityManager)) {
      throw new IllegalStateException("BackEnd was already set!");
    }
  }

  public static PwProvider getProvider() {
    return backend.get();
  }
}
