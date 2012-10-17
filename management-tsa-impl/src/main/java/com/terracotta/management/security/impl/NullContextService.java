/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.user.UserInfo;

/**
 * @author Ludovic Orban
 */
public class NullContextService implements ContextService {

  @Override
  public UserInfo getUserInfo() {
    return null;
  }

  @Override
  public void putUserInfo(UserInfo userInfo) {
    //
  }
}
