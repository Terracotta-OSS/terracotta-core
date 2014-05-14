package com.terracotta.management;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

public interface ApplicationTsaService {
  public Set<Class<?>> getResourceClasses();

  <T> Map<Class<T>, T> getServiceClasses(ThreadPoolExecutor tsaExecutorService, TimeoutService timeoutService,
      LocalManagementSource localManagementSource, RemoteManagementSource remoteManagementSource,
      SecurityContextService securityContextService, RequestTicketMonitor requestTicketMonitor,
      UserService userService, ContextService contextService, RemoteAgentBridgeService remoteAgentBridgeService,
      ThreadPoolExecutor l1BridgeExecutorService);
}
