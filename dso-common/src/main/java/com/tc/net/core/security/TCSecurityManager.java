package com.tc.net.core.security;

import com.tc.net.core.BufferManagerFactory;
import com.tc.security.PwProvider;

import java.security.Principal;

/**
 * @author Alex Snaps
 */
public interface TCSecurityManager extends PwProvider {
  Principal authenticate(String username, char[] chars);

  boolean isUserInRole(Principal principal, String roleName);

  BufferManagerFactory getBufferManagerFactory();

  String getIntraL2Username();
}
