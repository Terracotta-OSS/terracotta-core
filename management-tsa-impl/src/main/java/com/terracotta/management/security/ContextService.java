/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.security;

import com.terracotta.management.user.UserInfo;

/**
 * @author Ludovic Orban
 */
public interface ContextService {

  UserInfo getUserInfo();

  void putUserInfo(UserInfo userInfo);

}
