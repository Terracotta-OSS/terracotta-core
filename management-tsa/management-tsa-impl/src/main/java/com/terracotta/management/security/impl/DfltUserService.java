/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.UserService;
import com.terracotta.management.user.UserInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ludovic Orban
 */
public class DfltUserService implements UserService {

  private final Map<String, UserInfo> userInfoMap = new ConcurrentHashMap<String, UserInfo>();

  @Override
  public UserInfo getUserInfo(String token) {
    return userInfoMap.remove(token);
  }

  @Override
  public String putUserInfo(UserInfo userInfo) {
    String token = UUID.randomUUID().toString();
    userInfoMap.put(token, userInfo);
    return token;
  }

}
