/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security;

import com.terracotta.management.user.UserInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author brandony
 */
public interface RequestIdentityAsserter {
  UserInfo assertIdentity(HttpServletRequest request,
                          HttpServletResponse response)
      throws InvalidIAInteractionException;
}
