/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import net.sf.ehcache.management.resource.services.validator.impl.JmxEhcacheRequestValidator;
import net.sf.ehcache.management.service.AgentService;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import net.sf.ehcache.management.service.impl.JmxRepositoryService;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.IdentityAssertionServiceClient;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.RequestIdentityAsserter;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.DfltContextService;
import com.terracotta.management.security.impl.DfltRequestTicketMonitor;
import com.terracotta.management.security.impl.DfltUserService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullIdentityAsserter;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.security.impl.RelayingJerseyIdentityAssertionServiceClient;
import com.terracotta.management.security.impl.TSAIdentityAsserter;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.MonitoringService;
import com.terracotta.management.service.TopologyService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.ClearTextTsaManagementClientServiceImpl;
import com.terracotta.management.service.impl.DiagnosticsServiceImpl;
import com.terracotta.management.service.impl.MonitoringServiceImpl;
import com.terracotta.management.service.impl.TopologyServiceImpl;
import com.terracotta.management.service.impl.TsaAgentServiceImpl;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;
import com.terracotta.management.web.config.TSAConfig;

import javax.servlet.ServletContextEvent;

/**
 * @author Ludovic Orban
 */
public class TSAEnvironmentLoaderListener extends EnvironmentLoaderListener {

  private volatile JmxConnectorPool jmxConnectorPool;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      ServiceLocator serviceLocator = new ServiceLocator();

      // The following services are for monitoring the TSA itself
      serviceLocator.loadService(TSARequestValidator.class, new TSARequestValidator());
      jmxConnectorPool = new JmxConnectorPool();
      TsaManagementClientService tsaManagementClientService = new ClearTextTsaManagementClientServiceImpl(jmxConnectorPool);
      serviceLocator.loadService(TsaManagementClientService.class, tsaManagementClientService);
      serviceLocator.loadService(TopologyService.class, new TopologyServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(MonitoringService.class, new MonitoringServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(DiagnosticsService.class, new DiagnosticsServiceImpl(tsaManagementClientService));

      // The following services are for forwarding REST calls to L1s, using security or not
      boolean sslEnabled = TSAConfig.isSslEnabled();

      JmxEhcacheRequestValidator requestValidator = new JmxEhcacheRequestValidator(tsaManagementClientService);
      AgentService l1Agent;

      if (sslEnabled) {
        KeyChainAccessor kcAccessor = TSAConfig.getKeyChain();
        String securityServiceLocation = TSAConfig.getSecurityServiceLocation();
        Integer securityTimeout = TSAConfig.getSecurityTimeout();
        SSLContextFactory sslCtxtFactory = TSAConfig.getSSLContextFactory();

        ContextService contextService = new DfltContextService();
        UserService userService = new DfltUserService();
        IdentityAssertionServiceClient identityAssertionServiceClient = new RelayingJerseyIdentityAssertionServiceClient(kcAccessor, sslCtxtFactory, securityServiceLocation, securityTimeout, contextService);
        RequestTicketMonitor requestTicketMonitor = new DfltRequestTicketMonitor();
        TSAIdentityAsserter identityAsserter = new TSAIdentityAsserter(requestTicketMonitor, userService, kcAccessor);

        JmxRepositoryService repoSvc = new JmxRepositoryService(tsaManagementClientService, requestValidator, requestTicketMonitor, contextService, userService);
        l1Agent = repoSvc;

        serviceLocator.loadService(RequestTicketMonitor.class, requestTicketMonitor);
        serviceLocator.loadService(RequestIdentityAsserter.class, identityAsserter);
        serviceLocator.loadService(ContextService.class, contextService);
        serviceLocator.loadService(UserService.class, userService);
        serviceLocator.loadService(IdentityAssertionServiceClient.class, identityAssertionServiceClient);
        serviceLocator.loadService(RequestValidator.class, requestValidator);
        serviceLocator.loadService(KeyChainAccessor.class, kcAccessor);
        serviceLocator.loadService(CacheManagerService.class, repoSvc);
        serviceLocator.loadService(CacheService.class, repoSvc);
        serviceLocator.loadService(EntityResourceFactory.class, repoSvc);
      } else {
        ContextService contextService = new NullContextService();
        UserService userService = new NullUserService();
        RequestTicketMonitor requestTicketMonitor = new NullRequestTicketMonitor();
        RequestIdentityAsserter identityAsserter = new NullIdentityAsserter();

        JmxRepositoryService repoSvc = new JmxRepositoryService(tsaManagementClientService, requestValidator, requestTicketMonitor, contextService, userService);
        l1Agent = repoSvc;

        serviceLocator.loadService(RequestTicketMonitor.class, requestTicketMonitor);
        serviceLocator.loadService(RequestIdentityAsserter.class, identityAsserter);
        serviceLocator.loadService(ContextService.class, contextService);
        serviceLocator.loadService(UserService.class, userService);
        serviceLocator.loadService(RequestValidator.class, requestValidator);
        serviceLocator.loadService(CacheManagerService.class, repoSvc);
        serviceLocator.loadService(CacheService.class, repoSvc);
        serviceLocator.loadService(EntityResourceFactory.class, repoSvc);
      }

      serviceLocator.loadService(AgentService.class, new TsaAgentServiceImpl(tsaManagementClientService, l1Agent));

      ServiceLocator.load(serviceLocator);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Error initializing TSAEnvironmentLoaderListener", e);
    }

    super.contextInitialized(sce);
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    jmxConnectorPool.shutdown();

    super.contextDestroyed(sce);
  }
}
