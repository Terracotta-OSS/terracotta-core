package com.terracotta.management;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.terracotta.management.ServiceLocator;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

/**
 * Implementers of this interface contribute JAX-RS (Rest resources) to the TSA Rest agent, when implementing
 * {@link ApplicationTsaService#getResourceClasses() }
 * 
 * In a similar fashion, they can also contribute backend services for those Rest resources, implementing
 * {@link ApplicationTsaService#getServiceClasses(ThreadPoolExecutor, TimeoutService, LocalManagementSource, RemoteManagementSource, SecurityContextService, RequestTicketMonitor, UserService, ContextService, RemoteAgentBridgeService, ThreadPoolExecutor)
 * () }
 *
 * The {@link ServiceLoader} API is used to dynamically discover those services contributors.
 * 
 * @author Anthony Dahanne
 *
 */
public interface ApplicationTsaService {
  /**
   * Used to contribute JAX RS resources to the TSA Rest Agent
   * 
   * @return JAX-RS resources
   */
  public Set<Class<?>> getResourceClasses();

  /**
   * Used to contribute backend services for the Rest resources
   * 
   * @param tsaExecutorService
   * @param timeoutService
   * @param localManagementSource
   * @param remoteManagementSource
   * @param securityContextService
   * @param requestTicketMonitor
   * @param userService
   * @param contextService
   * @param remoteAgentBridgeService
   * @param l1BridgeExecutorService
   * 
   * @return a list of wired up backend services, ready to be added to the {@link ServiceLocator}
   */
  <T> Map<Class<T>, T> getServiceClasses(ThreadPoolExecutor tsaExecutorService, TimeoutService timeoutService,
      LocalManagementSource localManagementSource, RemoteManagementSource remoteManagementSource,
      SecurityContextService securityContextService, RequestTicketMonitor requestTicketMonitor,
      UserService userService, ContextService contextService, RemoteAgentBridgeService remoteAgentBridgeService,
      ThreadPoolExecutor l1BridgeExecutorService);
}
