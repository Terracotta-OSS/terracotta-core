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
