package com.terracotta.management.security.impl;

import com.terracotta.management.security.UserService;
import com.terracotta.management.user.UserInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
