/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.user.UserInfo;

/**
 * @author Ludovic Orban
 */
public class DfltContextService implements ContextService {

  private final ThreadLocal<UserInfo> userInfoThreadLocal = new ThreadLocal<UserInfo>();

  @Override
  public UserInfo getUserInfo() {
    UserInfo userInfo = userInfoThreadLocal.get();
    userInfoThreadLocal.remove();
    return userInfo;
  }

  @Override
  public void putUserInfo(UserInfo userInfo) {
    userInfoThreadLocal.set(userInfo);
  }
}
