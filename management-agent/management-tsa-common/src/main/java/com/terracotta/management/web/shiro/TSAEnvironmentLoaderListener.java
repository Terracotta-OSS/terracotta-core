/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.web.shiro;

import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.terracotta.management.ServiceLocator;

import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.terracotta.management.ApplicationTsaService;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.IdentityAssertionServiceClient;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.RequestIdentityAsserter;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.SecurityServiceDirectory;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.ConstantSecurityServiceDirectory;
import com.terracotta.management.security.impl.DfltContextService;
import com.terracotta.management.security.impl.DfltRequestTicketMonitor;
import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.security.impl.DfltUserService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullIdentityAsserter;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.security.impl.RelayingJerseyIdentityAssertionServiceClient;
import com.terracotta.management.security.impl.TSAIdentityAsserter;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.RemoteAgentBridgeServiceImpl;
import com.terracotta.management.service.impl.TimeoutServiceImpl;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;
import com.terracotta.management.web.utils.TSAConfig;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

/**
 * @author Ludovic Orban
 * @param <T>
 */
public class TSAEnvironmentLoaderListener<T> extends EnvironmentLoaderListener {

  private static final int REJECTION_TIMEOUT = Integer.getInteger("com.tc.management.threadPools.rejectionTimeout", 25);

  static {
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();
  }

  private static final Logger LOG = LoggerFactory.getLogger(TSAEnvironmentLoaderListener.class);

  private volatile ThreadPoolExecutor l1BridgeExecutorService;
  private volatile ThreadPoolExecutor tsaExecutorService;
  private volatile RemoteManagementSource remoteManagementSource;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      boolean sslEnabled = TSAConfig.isSslEnabled();
      ServiceLocator serviceLocator = new ServiceLocator();

      /// TSA services ///

      int maxThreads = TCPropertiesImpl.getProperties().getInt(TCPropertiesConsts.L2_REMOTEJMX_MAXTHREADS);
      l1BridgeExecutorService = new ThreadPoolExecutor(maxThreads, maxThreads, 60L, TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(maxThreads * 32, true), new ManagementThreadFactory("Management-Agent-L1"));
      l1BridgeExecutorService.allowCoreThreadTimeOut(true);
      l1BridgeExecutorService.setRejectedExecutionHandler(new RejectedExecutionHandler() {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
          try {
            boolean accepted = l1BridgeExecutorService.getQueue().offer(r, REJECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!accepted) {
              throw new RejectedExecutionException("L1 Management thread pool saturated, job rejected");
            }
          } catch (InterruptedException ie) {
            throw new RejectedExecutionException("L1 Management thread pool interrupted, job rejected", ie);
          }
        }
      });

      tsaExecutorService = new ThreadPoolExecutor(maxThreads, maxThreads, 60L, TimeUnit.SECONDS,
          new ArrayBlockingQueue<Runnable>(maxThreads * 32, true), new ManagementThreadFactory("Management-Agent-L2"));
      tsaExecutorService.allowCoreThreadTimeOut(true);
      tsaExecutorService.setRejectedExecutionHandler(new RejectedExecutionHandler() {
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
          try {
            boolean accepted = l1BridgeExecutorService.getQueue().offer(r, REJECTION_TIMEOUT, TimeUnit.MILLISECONDS);
            if (!accepted) {
              throw new RejectedExecutionException("L2 Management thread pool saturated, job rejected");
            }
          } catch (InterruptedException ie) {
            throw new RejectedExecutionException("L2 Management thread pool interrupted, job rejected", ie);
          }
        }
      });

      TimeoutService timeoutService = new TimeoutServiceImpl(TSAConfig.getDefaultL1BridgeTimeout());
      SecurityContextService securityContextService = new DfltSecurityContextService();

      LocalManagementSource localManagementSource = new LocalManagementSource();

      RemoteAgentBridgeService remoteAgentBridgeService = new RemoteAgentBridgeServiceImpl();

      serviceLocator.loadService(TimeoutService.class, timeoutService);
      serviceLocator.loadService(SecurityContextService.class, securityContextService);

      // /Security Services ///

      KeyChainAccessor kcAccessor;
      IdentityAssertionServiceClient identityAssertionServiceClient;
      ContextService contextService;
      UserService userService;
      RequestTicketMonitor requestTicketMonitor;
      RequestIdentityAsserter identityAsserter;

      SSLContextFactory sslCtxtFactory;
      if (sslEnabled) {
        kcAccessor = TSAConfig.getKeyChain();
        String securityServiceLocation = TSAConfig.getSecurityServiceLocation();
        Integer securityTimeout = TSAConfig.getSecurityTimeout();
        sslCtxtFactory = TSAConfig.getSSLContextFactory();

        contextService = new DfltContextService();
        userService = new DfltUserService();
        URI securityServiceLocationUri = securityServiceLocation == null ? null : new URI(securityServiceLocation);
        SecurityServiceDirectory securityServiceDirectory = new ConstantSecurityServiceDirectory(securityServiceLocationUri, securityTimeout);
        identityAssertionServiceClient = new RelayingJerseyIdentityAssertionServiceClient(kcAccessor, sslCtxtFactory, securityServiceDirectory, contextService);
        requestTicketMonitor = new DfltRequestTicketMonitor();
        identityAsserter = new TSAIdentityAsserter(requestTicketMonitor, userService, kcAccessor);
      } else {
        sslCtxtFactory = null;
        identityAssertionServiceClient = null;
        kcAccessor = null;
        contextService = new NullContextService();
        userService = new NullUserService();
        requestTicketMonitor = new NullRequestTicketMonitor();
        identityAsserter = new NullIdentityAsserter();
      }

      remoteManagementSource = new RemoteManagementSource(localManagementSource, timeoutService, securityContextService, sslCtxtFactory);
      serviceLocator.loadService(RemoteManagementSource.class, remoteManagementSource);

      serviceLocator.loadService(RequestIdentityAsserter.class, identityAsserter);
      serviceLocator.loadService(IdentityAssertionServiceClient.class, identityAssertionServiceClient);
      serviceLocator.loadService(KeyChainAccessor.class, kcAccessor);
      serviceLocator.loadService(TSARequestValidator.class, new TSARequestValidator());

      ServiceLoader<ApplicationTsaService> loaders = ServiceLoader.load(ApplicationTsaService.class);
      for (ApplicationTsaService applicationEhCacheService : loaders) {
        Map<Class<T>, T> serviceClasses = applicationEhCacheService.getServiceClasses(tsaExecutorService,
            timeoutService, localManagementSource, remoteManagementSource, securityContextService, requestTicketMonitor,
            userService, contextService, remoteAgentBridgeService, l1BridgeExecutorService);
        for (Entry<Class<T>, T> entry : serviceClasses.entrySet()) {
          serviceLocator.loadService(entry.getKey(), entry.getValue());
        }
      }

      ServiceLocator.load(serviceLocator);

      List<String> strings = localManagementSource.performSecurityChecks();
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
    if (remoteManagementSource != null) {
      remoteManagementSource.shutdown();
    }
    if (l1BridgeExecutorService != null) {
      l1BridgeExecutorService.shutdown();
    }
    if (tsaExecutorService != null) {
      tsaExecutorService.shutdown();
    }

    super.contextDestroyed(sce);
  }

  private static final class ManagementThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumberGenerator = new AtomicInteger(1);

    private final ThreadGroup group = (System.getSecurityManager() != null) ?
        System.getSecurityManager().getThreadGroup() : Thread.currentThread().getThreadGroup();
        private final String threadNamePrefix;

        private ManagementThreadFactory(String threadNamePrefix) {
          this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
          Thread thread = new Thread(group, r, threadNamePrefix + "-" + threadNumberGenerator.getAndIncrement(), 0);
          if (thread.isDaemon()) {
            thread.setDaemon(false);
          }
          if (thread.getPriority() != Thread.NORM_PRIORITY) {
            thread.setPriority(Thread.NORM_PRIORITY);
          }
          return thread;
        }
  }

}
