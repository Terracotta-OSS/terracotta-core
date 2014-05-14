/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.InvalidIAInteractionException;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.SecurityServiceDirectory;
import com.terracotta.management.user.UserInfo;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author Ludovic Orban
 */
public class RelayingJerseyIdentityAssertionServiceClient extends JerseyIdentityAssertionServiceClient {
  private final ContextService contextService;

  public RelayingJerseyIdentityAssertionServiceClient(KeyChainAccessor keyChainAccessor, SSLContextFactory sslCtxtFactory,
                                                      SecurityServiceDirectory securityServiceDirectory,
                                                      ContextService contextService) throws URISyntaxException, MalformedURLException {
    super(keyChainAccessor, sslCtxtFactory, securityServiceDirectory);
    this.contextService = contextService;
  }

  @Override
  public UserInfo retreiveUserDetail(IACredentials credentials) throws InvalidIAInteractionException {
    UserInfo userInfo = super.retreiveUserDetail(credentials);

    contextService.putUserInfo(userInfo);
    return userInfo;
  }
}
