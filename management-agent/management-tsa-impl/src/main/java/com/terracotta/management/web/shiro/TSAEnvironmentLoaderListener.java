/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import com.terracotta.management.l1bridge.RemoteRequestValidator;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.services.AgentService;
import org.terracotta.management.resource.services.validator.RequestValidator;
import org.terracotta.session.management.SessionsService;

import com.tc.net.util.TSASSLSocketFactory;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.l1bridge.RemoteAgentService;
import com.terracotta.management.l1bridge.RemoteServiceStubGenerator;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.IdentityAssertionServiceClient;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.RequestIdentityAsserter;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.SecretUtils;
import com.terracotta.management.security.SecurityServiceDirectory;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.ConstantSecurityServiceDirectory;
import com.terracotta.management.security.impl.DfltContextService;
import com.terracotta.management.security.impl.DfltRequestTicketMonitor;
import com.terracotta.management.security.impl.DfltUserService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullIdentityAsserter;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.security.impl.RelayingJerseyIdentityAssertionServiceClient;
import com.terracotta.management.security.impl.TSAIdentityAsserter;
import com.terracotta.management.service.BackupService;
import com.terracotta.management.service.ConfigurationService;
import com.terracotta.management.service.DiagnosticsService;
import com.terracotta.management.service.JmxService;
import com.terracotta.management.service.LogsService;
import com.terracotta.management.service.MonitoringService;
import com.terracotta.management.service.OperatorEventsService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.ShutdownService;
import com.terracotta.management.service.TopologyService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.service.impl.BackupServiceImpl;
import com.terracotta.management.service.impl.ConfigurationServiceImpl;
import com.terracotta.management.service.impl.DiagnosticsServiceImpl;
import com.terracotta.management.service.impl.JmxServiceImpl;
import com.terracotta.management.service.impl.LogsServiceImpl;
import com.terracotta.management.service.impl.MonitoringServiceImpl;
import com.terracotta.management.service.impl.OperatorEventsServiceImpl;
import com.terracotta.management.service.impl.ShutdownServiceImpl;
import com.terracotta.management.service.impl.TopologyServiceImpl;
import com.terracotta.management.service.impl.TsaAgentServiceImpl;
import com.terracotta.management.service.impl.TsaManagementClientServiceImpl;
import com.terracotta.management.service.impl.pool.JmxConnectorPool;
import com.terracotta.management.web.utils.TSAConfig;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.remote.rmi.RMIConnectorServer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * @author Ludovic Orban
 */
public class TSAEnvironmentLoaderListener extends EnvironmentLoaderListener {

  private static final Logger LOG = LoggerFactory.getLogger(TSAEnvironmentLoaderListener.class);

  private volatile JmxConnectorPool jmxConnectorPool;
  private volatile ExecutorService executorService;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      boolean sslEnabled = TSAConfig.isSslEnabled();
      ServiceLocator serviceLocator = new ServiceLocator();

      /// TSA services ///

      TsaManagementClientServiceImpl tsaManagementClientService;
      if (sslEnabled) {
        final TSASSLSocketFactory socketFactory = new TSASSLSocketFactory();
        jmxConnectorPool = new JmxConnectorPool("service:jmx:rmi://{0}:{1}/jndi/rmi://{0}:{1}/jmxrmi") {
          @Override
          protected Map<String, Object> createJmxConnectorEnv(String host, int port) {
            try {
              Map<String, Object> env = new HashMap<String, Object>();
              env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, socketFactory);
              env.put("com.sun.jndi.rmi.factory.socket", socketFactory);
              String intraL2Username = TSAConfig.getIntraL2Username();
              URIKeyName alias = new URIKeyName("jmx://" + intraL2Username + "@" + host + ":" + port);
              byte[] secret = TSAConfig.getKeyChain().retrieveSecret(alias);
              if (secret == null) {
                throw new RuntimeException("Missing keychain entry for URL [" + alias + "]");
              }
              env.put("jmx.remote.credentials", new Object[] { intraL2Username, SecretUtils.toCharsAndWipe(secret)});
              return env;
            } catch (Exception e) {
              throw new RuntimeException("Error retrieving secret for JMX host [" + host + ":" + port + "]", e);
            }
          }
        };
      } else {
        jmxConnectorPool = new JmxConnectorPool("service:jmx:jmxmp://{0}:{1}");
      }
      executorService = Executors.newCachedThreadPool(new ThreadFactory() {
        private final ThreadGroup group = (System.getSecurityManager() != null) ?
            System.getSecurityManager().getThreadGroup() : Thread.currentThread().getThreadGroup();
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
          Thread thread = new Thread(group, r, "Management-Agent-" + threadNumber.getAndIncrement(), 0);
          if (thread.isDaemon()) {
            thread.setDaemon(false);
          }
          if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
          }
          return thread;
        }
      });
      tsaManagementClientService = new TsaManagementClientServiceImpl(jmxConnectorPool, sslEnabled, executorService, TSAConfig.getDefaultL1BridgeTimeout());

      serviceLocator.loadService(TSARequestValidator.class, new TSARequestValidator());
      serviceLocator.loadService(TsaManagementClientService.class, tsaManagementClientService);
      serviceLocator.loadService(TopologyService.class, new TopologyServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(MonitoringService.class, new MonitoringServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(DiagnosticsService.class, new DiagnosticsServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(ConfigurationService.class, new ConfigurationServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(BackupService.class, new BackupServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(LogsService.class, new LogsServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(OperatorEventsService.class, new OperatorEventsServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(ShutdownService.class, new ShutdownServiceImpl(tsaManagementClientService));
      serviceLocator.loadService(JmxService.class, new JmxServiceImpl(tsaManagementClientService));

      /// L1 bridge and Security Services ///

      KeyChainAccessor kcAccessor;
      IdentityAssertionServiceClient identityAssertionServiceClient;
      ContextService contextService;
      UserService userService;
      RequestTicketMonitor requestTicketMonitor;
      RequestIdentityAsserter identityAsserter;

      if (sslEnabled) {
        kcAccessor = TSAConfig.getKeyChain();
        String securityServiceLocation = TSAConfig.getSecurityServiceLocation();
        Integer securityTimeout = TSAConfig.getSecurityTimeout();
        SSLContextFactory sslCtxtFactory = TSAConfig.getSSLContextFactory();

        contextService = new DfltContextService();
        userService = new DfltUserService();
        URI securityServiceLocationUri = securityServiceLocation == null ? null : new URI(securityServiceLocation);
        SecurityServiceDirectory securityServiceDirectory = new ConstantSecurityServiceDirectory(securityServiceLocationUri, securityTimeout);
        identityAssertionServiceClient = new RelayingJerseyIdentityAssertionServiceClient(kcAccessor, sslCtxtFactory, securityServiceDirectory, contextService);
        requestTicketMonitor = new DfltRequestTicketMonitor();
        identityAsserter = new TSAIdentityAsserter(requestTicketMonitor, userService, kcAccessor);
      } else {
        identityAssertionServiceClient = null;
        kcAccessor = null;
        contextService = new NullContextService();
        userService = new NullUserService();
        requestTicketMonitor = new NullRequestTicketMonitor();
        identityAsserter = new NullIdentityAsserter();
      }

      RemoteRequestValidator requestValidator = new RemoteRequestValidator(tsaManagementClientService);
      RemoteServiceStubGenerator remoteServiceStubGenerator = new RemoteServiceStubGenerator(requestTicketMonitor, userService, contextService, requestValidator, tsaManagementClientService, executorService);

      serviceLocator.loadService(RequestTicketMonitor.class, requestTicketMonitor);
      serviceLocator.loadService(RequestIdentityAsserter.class, identityAsserter);
      serviceLocator.loadService(ContextService.class, contextService);
      serviceLocator.loadService(UserService.class, userService);
      serviceLocator.loadService(IdentityAssertionServiceClient.class, identityAssertionServiceClient);
      serviceLocator.loadService(RequestValidator.class, requestValidator);
      serviceLocator.loadService(KeyChainAccessor.class, kcAccessor);
      serviceLocator.loadService(RemoteAgentBridgeService.class, tsaManagementClientService);

      /// Compound Agent Service ///

      RemoteAgentService remoteAgentService = new RemoteAgentService(tsaManagementClientService, contextService, executorService, requestTicketMonitor, userService);
      serviceLocator.loadService(AgentService.class, new TsaAgentServiceImpl(tsaManagementClientService, tsaManagementClientService, remoteAgentService));

      /// Ehcache Services ///

      serviceLocator.loadService(CacheManagerService.class, remoteServiceStubGenerator.newRemoteService(CacheManagerService.class, "Ehcache"));
      serviceLocator.loadService(CacheService.class, remoteServiceStubGenerator.newRemoteService(CacheService.class, "Ehcache"));
      serviceLocator.loadService(EntityResourceFactory.class, remoteServiceStubGenerator.newRemoteService(EntityResourceFactory.class, "Ehcache"));

      /// Sessions Services ///

      serviceLocator.loadService(SessionsService.class, remoteServiceStubGenerator.newRemoteService(SessionsService.class, "Sessions"));

      /// <end of services> ///

      ServiceLocator.load(serviceLocator);

      List<String> strings = tsaManagementClientService.performSecurityChecks();
      for (String string : strings) {
        LOG.warn(string);
      }

      super.contextInitialized(sce);
    } catch (Exception e) {
      throw new RuntimeException("Error initializing TSAEnvironmentLoaderListener", e);
    }
  }

  @Override
  protected Class<?> determineWebEnvironmentClass(ServletContext servletContext) {
    return TSAIniWebEnvironment.class;
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    if (jmxConnectorPool != null) {
      jmxConnectorPool.shutdown();
    }
    if (executorService != null) {
      executorService.shutdown();
    }

    super.contextDestroyed(sce);
  }

}
