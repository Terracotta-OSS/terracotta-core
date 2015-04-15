/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.web.shiro;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class SecuritySetup {

  private final NullContextService nullContextService = new NullContextService();
  private final NullRequestTicketMonitor nullRequestTicketMonitor = new NullRequestTicketMonitor();
  private final NullUserService nullUserService = new NullUserService();
  private final DfltSecurityContextService dfltSecurityContextService = new DfltSecurityContextService();

  public ContextService getContextService() {
    return nullContextService;
  }

  public RemoteManagementSource buildRemoteManagementSource(LocalManagementSource localManagementSource, TimeoutService timeoutService) {
    return new RemoteManagementSource(localManagementSource, timeoutService);
  }

  public RequestTicketMonitor getRequestTicketMonitor() {
    return nullRequestTicketMonitor;
  }

  public UserService getUserService() {
    return nullUserService;
  }

  public SecurityContextService getSecurityContextService() {
    return dfltSecurityContextService;
  }

  public List<String> performSecurityChecks() {
    return Collections.emptyList();
  }
}
