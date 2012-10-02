/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package com.terracotta.management.security.impl;

import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.security.HMACBuilder;
import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.InvalidIAInteractionException;
import com.terracotta.management.security.InvalidRequestTicketException;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.RequestIdentityAsserter;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.user.UserInfo;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Ludovic Orban
 */
public final class NullIdentityAsserter implements RequestIdentityAsserter {

  @Override
  public UserInfo assertIdentity(HttpServletRequest request,
                                 HttpServletResponse response) throws InvalidIAInteractionException {
      throw new InvalidIAInteractionException(
          String.format("IA request received from host '%s' while security is disabled.", request.getRemoteAddr()));
  }

}
