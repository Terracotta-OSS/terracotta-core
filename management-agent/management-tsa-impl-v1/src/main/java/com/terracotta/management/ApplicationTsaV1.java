package com.terracotta.management;

import net.sf.ehcache.management.resource.services.ElementsResourceServiceImpl;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;

import org.terracotta.management.application.DefaultApplication;
import org.terracotta.management.resource.services.AgentService;
import org.terracotta.management.resource.services.validator.RequestValidator;
import org.terracotta.session.management.SessionsService;

import com.terracotta.management.l1bridge.RemoteAgentService;
import com.terracotta.management.l1bridge.RemoteRequestValidator;
import com.terracotta.management.l1bridge.RemoteServiceStubGenerator;
import com.terracotta.management.resource.services.BackupResourceServiceImpl;
import com.terracotta.management.resource.services.ConfigurationResourceServiceImpl;
import com.terracotta.management.resource.services.DiagnosticsResourceServiceImpl;
import com.terracotta.management.resource.services.JmxResourceServiceImpl;
import com.terracotta.management.resource.services.LicenseResourceServiceImpl;
import com.terracotta.management.resource.services.LogsResourceServiceImpl;
import com.terracotta.management.resource.services.MonitoringResourceServiceImpl;
import com.terracotta.management.resource.services.OperatorEventsResourceServiceImpl;
import com.terracotta.management.resource.services.ShutdownResourceServiceImpl;
import com.terracotta.management.resource.services.TopologyResourceServiceImpl;
import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.BackupService;
import com.terracotta.management.service.ConfigurationService;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.JmxService;
import com.terracotta.management.service.LicenseService;
import com.terracotta.management.service.LogsService;
import com.terracotta.management.service.MonitoringService;
import com.terracotta.management.service.OperatorEventsService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.ShutdownService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.TopologyService;
import com.terracotta.management.service.impl.BackupServiceImpl;
import com.terracotta.management.service.impl.ClientManagementService;
import com.terracotta.management.service.impl.ConfigurationServiceImpl;
import com.terracotta.management.service.impl.DiagnosticsServiceImpl;
import com.terracotta.management.service.impl.JmxServiceImpl;
import com.terracotta.management.service.impl.LicenseServiceImpl;
import com.terracotta.management.service.impl.LogsServiceImpl;
import com.terracotta.management.service.impl.MonitoringServiceImpl;
import com.terracotta.management.service.impl.OperatorEventsServiceImpl;
import com.terracotta.management.service.impl.ServerManagementService;
import com.terracotta.management.service.impl.ShutdownServiceImpl;
import com.terracotta.management.service.impl.TopologyServiceImpl;
import com.terracotta.management.service.impl.TsaAgentServiceImpl;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;
import com.terracotta.management.web.proxy.ProxyExceptionMapper;
import com.terracotta.management.web.resource.services.IdentityAssertionResourceService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class ApplicationTsaV1 extends DefaultApplication implements ApplicationTsaService {

  @Override
  public Set<Class<?>> getResourceClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(ElementsResourceServiceImpl.class);
    s.add(BackupResourceServiceImpl.class);
    s.add(ConfigurationResourceServiceImpl.class);
    s.add(DiagnosticsResourceServiceImpl.class);
    s.add(LogsResourceServiceImpl.class);
    s.add(MonitoringResourceServiceImpl.class);
    s.add(OperatorEventsResourceServiceImpl.class);
    s.add(ShutdownResourceServiceImpl.class);
    s.add(TopologyResourceServiceImpl.class);
    s.add(IdentityAssertionResourceService.class);
    s.add(JmxResourceServiceImpl.class);
    s.add(LicenseResourceServiceImpl.class);
    
    s.add(ProxyExceptionMapper.class);


    s.add(net.sf.ehcache.management.resource.services.CacheStatisticSamplesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CachesResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagersResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheManagerConfigsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.CacheConfigsResourceServiceImpl.class);
    s.add(org.terracotta.management.resource.services.AgentsResourceServiceImpl.class);
    s.add(net.sf.ehcache.management.resource.services.QueryResourceServiceImpl.class);

    s.add(org.terracotta.session.management.SessionsResourceServiceImpl.class);
    

    return s;
  }

  @Override
  public Map<Class<?>, Object> getServiceClasses(ThreadPoolExecutor tsaExecutorService, TimeoutService timeoutService,
                                                 LocalManagementSource localManagementSource,
                                                 RemoteManagementSource remoteManagementSource,
                                                 SecurityContextService securityContextService,
                                                 RequestTicketMonitor requestTicketMonitor, UserService userService,
                                                 ContextService contextService,
                                                 RemoteAgentBridgeService remoteAgentBridgeService,
                                                 ThreadPoolExecutor l1BridgeExecutorService) {

    Map<Class<?>, Object> serviceClasses = new HashMap<Class<?>, Object>();

    ServerManagementService serverManagementService = new ServerManagementService(tsaExecutorService, timeoutService,
                                                                                  localManagementSource,
                                                                                  remoteManagementSource,
                                                                                  securityContextService);
    OperatorEventsServiceImpl operatorEventsServiceImpl = new OperatorEventsServiceImpl(serverManagementService);
    ClientManagementService clientManagementService = new ClientManagementService(serverManagementService,
                                                                                  tsaExecutorService, timeoutService,
                                                                                  localManagementSource,
                                                                                  remoteManagementSource,
                                                                                  securityContextService);

    /// pure L2 Services ///
    serviceClasses.put(TopologyService.class, new TopologyServiceImpl(serverManagementService, clientManagementService, operatorEventsServiceImpl));
    serviceClasses.put(MonitoringService.class, new MonitoringServiceImpl(serverManagementService, clientManagementService));
    serviceClasses.put(DiagnosticsService.class, new DiagnosticsServiceImpl(serverManagementService, clientManagementService));
    serviceClasses.put(ConfigurationService.class, new ConfigurationServiceImpl(serverManagementService, clientManagementService));
    serviceClasses.put(BackupService.class, new BackupServiceImpl(serverManagementService));
    serviceClasses.put(LogsService.class, new LogsServiceImpl(serverManagementService));
    serviceClasses.put(OperatorEventsService.class, operatorEventsServiceImpl);
    serviceClasses.put(ShutdownService.class, new ShutdownServiceImpl(serverManagementService));
    serviceClasses.put(JmxService.class, new JmxServiceImpl(serverManagementService));
    serviceClasses.put(LicenseService.class, new LicenseServiceImpl(serverManagementService));
    serviceClasses.put(ClientManagementService.class, clientManagementService);

    /// L1 bridge and Security Services ///
    RemoteRequestValidator requestValidator = new RemoteRequestValidator(remoteAgentBridgeService, serverManagementService);
    RemoteServiceStubGenerator remoteServiceStubGenerator = new RemoteServiceStubGenerator(requestTicketMonitor,
                                                                                           userService, contextService,
                                                                                           requestValidator,
                                                                                           remoteAgentBridgeService,
                                                                                           l1BridgeExecutorService,
                                                                                           timeoutService,
                                                                                           serverManagementService);
    serviceClasses.put(RequestTicketMonitor.class, requestTicketMonitor);
    serviceClasses.put(ContextService.class, contextService);
    serviceClasses.put(UserService.class, userService);
    serviceClasses.put(RequestValidator.class, requestValidator);
    serviceClasses.put(RemoteAgentBridgeService.class, remoteAgentBridgeService);

    /// Compound Agent Service ///
    RemoteAgentService remoteAgentService = new RemoteAgentService(remoteAgentBridgeService, contextService, l1BridgeExecutorService, requestTicketMonitor, userService, timeoutService, serverManagementService);
    serviceClasses.put(AgentService.class, new TsaAgentServiceImpl(serverManagementService, remoteAgentBridgeService, remoteAgentService));

    /// Ehcache Services ///
    serviceClasses.put(CacheManagerService.class, remoteServiceStubGenerator.newRemoteService(CacheManagerService.class, "Ehcache"));
    serviceClasses.put(CacheService.class, remoteServiceStubGenerator.newRemoteService(CacheService.class, "Ehcache"));
    serviceClasses.put(EntityResourceFactory.class, remoteServiceStubGenerator.newRemoteService(EntityResourceFactory.class, "Ehcache"));

    /// Sessions Services ///
    serviceClasses.put(SessionsService.class, remoteServiceStubGenerator.newRemoteService(SessionsService.class, "Sessions"));

    return serviceClasses;

  }

}