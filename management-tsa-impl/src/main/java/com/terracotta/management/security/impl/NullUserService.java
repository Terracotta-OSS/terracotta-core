/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.UserService;
import com.terracotta.management.user.UserInfo;

/**
 * @author Ludovic Orban
 */
public class NullUserService implements UserService {

  @Override
  public UserInfo getUserInfo(String token) {
    return null;
  }

  @Override
  public String putUserInfo(UserInfo userInfo) {
    return null;
  }

}
