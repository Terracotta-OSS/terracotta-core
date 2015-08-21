/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.core.security;

import java.security.Principal;

/**
 * @author Alex Snaps
 */
public interface Realm {

  /**
   * Initialize the realm. This method is called after the whole security
   * mechanism is initialized, so the realm has a chance to read its password(s)
   * from the key chain using {@link com.tc.security.PwProviderUtil} for instance.
   */
  void initialize();

  Principal authenticate(String username, char[] passwd);
}
