package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.resource.services.validator.impl.JmxEhcacheRequestValidator;
import net.sf.ehcache.management.service.AgentService;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Ludovic Orban
 */
public class JmxRepositoryService implements EntityResourceFactory, CacheManagerService, CacheService, AgentService {

  private final TsaManagementClientService tsaManagementClientService;
  private final JmxEhcacheRequestValidator requestValidator;
  private final RequestTicketMonitor ticketMonitor;
  private final ContextService contextService;
  private final UserService userService;
  private final ExecutorService executorService;

  public JmxRepositoryService(TsaManagementClientService tsaManagementClientService, JmxEhcacheRequestValidator requestValidator,
                              RequestTicketMonitor ticketMonitor, ContextService contextService, UserService userService,
                              ExecutorService executorService) {
    this.tsaManagementClientService = tsaManagementClientService;
    this.requestValidator = requestValidator;
    this.ticketMonitor = ticketMonitor;
    this.contextService = contextService;
    this.userService = userService;
    this.executorService = executorService;
  }

  private static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes == null) {
      return null;
    }

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    try {
      return (T)ois.readObject();
    } finally {
      ois.close();
    }
  }

  private static <T extends Representable> Collection<T> rewriteAgentId(Collection<T> representables, String agentId) {
    if (representables != null) {
      for (Representable r : representables) {
        r.setAgentId(agentId);
      }
    } else {
      representables = Collections.emptySet();
    }
    return representables;
  }

  @Override
  public void updateCacheManager(String cacheManagerName, CacheManagerEntity resource) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token,
        TSAConfig.getSecurityCallbackUrl(), "updateCacheManager", new Class<?>[] { String.class, CacheManagerEntity.class }, new Object[] { cacheManagerName, resource });
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName, String cacheName, CacheEntity resource) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
        .getSecurityCallbackUrl(), "createOrUpdateCache", new Class<?>[] { String.class, String.class, CacheEntity.class }, new Object[] { cacheManagerName, cacheName, resource });
  }

  @Override
  public void clearCache(String cacheManagerName, String cacheName) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
        .getSecurityCallbackUrl(), "clearCache", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(final Set<String> cacheManagerNames, final Set<String> attributes) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheManagerEntity>>> futures = new ArrayList<Future<Collection<CacheManagerEntity>>>();

    for (final String node : nodes) {
      Future<Collection<CacheManagerEntity>> future = executorService.submit(new Callable<Collection<CacheManagerEntity>>() {
        @Override
        public Collection<CacheManagerEntity> call() throws Exception {
          String ticket = ticketMonitor.issueRequestTicket();
          String token = userService.putUserInfo(userInfo);

          byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "createCacheManagerEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, attributes });
          return rewriteAgentId((Collection<CacheManagerEntity>)deserialize(bytes), node);
        }
      });
      futures.add(future);
    }

    Collection<CacheManagerEntity> globalResult = new ArrayList<CacheManagerEntity>();
    for (Future<Collection<CacheManagerEntity>> future : futures) {
      try {
        Collection<CacheManagerEntity> cacheManagerEntities = future.get();
        globalResult.addAll(cacheManagerEntities);
      } catch (Exception e) {
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(final Set<String> cacheManagerNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheManagerConfigEntity>>> futures = new ArrayList<Future<Collection<CacheManagerConfigEntity>>>();

    for (final String node : nodes) {
      Future<Collection<CacheManagerConfigEntity>> future = executorService.submit(new Callable<Collection<CacheManagerConfigEntity>>() {
        @Override
        public Collection<CacheManagerConfigEntity> call() throws Exception {
          String ticket = ticketMonitor.issueRequestTicket();
          String token = userService.putUserInfo(userInfo);

          byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "createCacheManagerConfigEntities", new Class<?>[] { Set.class }, new Object[] { cacheManagerNames });
          return rewriteAgentId((Collection<CacheManagerConfigEntity>)deserialize(bytes), node);
        }
      });
      futures.add(future);
    }

    Collection<CacheManagerConfigEntity> globalResult = new ArrayList<CacheManagerConfigEntity>();
    for (Future<Collection<CacheManagerConfigEntity>> future : futures) {
      try {
        Collection<CacheManagerConfigEntity> cacheManagerConfigEntities = future.get();
        globalResult.addAll(cacheManagerConfigEntities);
      } catch (Exception e) {
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(final Set<String> cacheManagerNames, final Set<String> cacheNames, final Set<String> attributes) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheEntity>>> futures = new ArrayList<Future<Collection<CacheEntity>>>();

    for (final String node : nodes) {
      Future<Collection<CacheEntity>> future = executorService.submit(new Callable<Collection<CacheEntity>>() {
        @Override
        public Collection<CacheEntity> call() throws Exception {
          String token = userService.putUserInfo(userInfo);
          String ticket = ticketMonitor.issueRequestTicket();

          byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "createCacheEntities", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, attributes });
          return rewriteAgentId((Collection<CacheEntity>)deserialize(bytes), node);
        }
      });

      futures.add(future);
    }

    Collection<CacheEntity> globalResult = new ArrayList<CacheEntity>();
    for (Future<Collection<CacheEntity>> future : futures) {
      try {
        Collection<CacheEntity> cacheEntities = future.get();
        globalResult.addAll(cacheEntities);
      } catch (Exception e) {
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(final Set<String> cacheManagerNames, final Set<String> cacheNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheConfigEntity>>> futures = new ArrayList<Future<Collection<CacheConfigEntity>>>();

    for (final String node : nodes) {
      Future<Collection<CacheConfigEntity>> future = executorService.submit(new Callable<Collection<CacheConfigEntity>>() {
        @Override
        public Collection<CacheConfigEntity> call() throws Exception {
          String ticket = ticketMonitor.issueRequestTicket();
          String token = userService.putUserInfo(userInfo);

          byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "createCacheConfigEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames });
          return rewriteAgentId((Collection<CacheConfigEntity>)deserialize(bytes), node);
        }
      });

      futures.add(future);
    }

    Collection<CacheConfigEntity> globalResult = new ArrayList<CacheConfigEntity>();
    for (Future<Collection<CacheConfigEntity>> future : futures) {
      try {
        Collection<CacheConfigEntity> cacheStatisticSampleEntities = future.get();
        globalResult.addAll(cacheStatisticSampleEntities);
      } catch (Exception e) {
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(final Set<String> cacheManagerNames, final Set<String> cacheNames, final Set<String> statNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheStatisticSampleEntity>>> futures = new ArrayList<Future<Collection<CacheStatisticSampleEntity>>>();

    for (final String node : nodes) {
      Future<Collection<CacheStatisticSampleEntity>> f = executorService.submit(new Callable<Collection<CacheStatisticSampleEntity>>() {
        @Override
        public Collection<CacheStatisticSampleEntity> call() throws Exception {
          String token = userService.putUserInfo(userInfo);
          String ticket = ticketMonitor.issueRequestTicket();

          byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "createCacheStatisticSampleEntity", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, statNames });
          return rewriteAgentId((Collection<CacheStatisticSampleEntity>)deserialize(bytes), node);
        }
      });

      futures.add(f);
    }

    Collection<CacheStatisticSampleEntity> globalResult = new ArrayList<CacheStatisticSampleEntity>();
    for (Future<Collection<CacheStatisticSampleEntity>> future : futures) {
      try {
        Collection<CacheStatisticSampleEntity> cacheStatisticSampleEntities = future.get();
        globalResult.addAll(cacheStatisticSampleEntities);
      } catch (Exception e) {
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    Set<String> nodes = tsaManagementClientService.getL1Nodes().keySet();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> idsClone = new HashSet<String>(ids);
    idsClone.removeAll(nodes);
    if (!idsClone.isEmpty()) {
      throw new ServiceExecutionException("Unknown agent IDs : " + idsClone);
    }

    final UserInfo userInfo = contextService.getUserInfo();
    Collection<Future<Collection<AgentMetadataEntity>>> futures = new ArrayList<Future<Collection<AgentMetadataEntity>>>();

    for (final String id : ids) {
      Future<Collection<AgentMetadataEntity>> future = executorService.submit(new Callable<Collection<AgentMetadataEntity>>() {
        @Override
        public Collection<AgentMetadataEntity> call() throws Exception {
          String ticket = ticketMonitor.issueRequestTicket();
          String token = userService.putUserInfo(userInfo);

          byte[] bytes = tsaManagementClientService.invokeMethod(id, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
              .getSecurityCallbackUrl(), "getAgentsMetadata", new Class<?>[] { Set.class }, new Object[] { Collections.emptySet() });
          return rewriteAgentId((Collection<AgentMetadataEntity>)deserialize(bytes), id);
        }
      });

      futures.add(future);
    }

    Collection<AgentMetadataEntity> globalResult = new ArrayList<AgentMetadataEntity>();
    for (Future<Collection<AgentMetadataEntity>> future : futures) {
      try {
        Collection<AgentMetadataEntity> agentMetadataEntities = future.get();
        globalResult.addAll(agentMetadataEntities);
      } catch (Exception e) {
        e.printStackTrace();
        throw new ServiceExecutionException(e);
      }
    }
    return globalResult;
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> idSet) throws ServiceExecutionException {
    Collection<AgentEntity> result = new ArrayList<AgentEntity>();

    Map<String, Map<String, String>> nodes = tsaManagementClientService.getL1Nodes();
    if (idSet.isEmpty()) {
      idSet = nodes.keySet();
    }

    for (String id : idSet) {
      if (!nodes.keySet().contains(id)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      Map<String, String> props = nodes.get(id);

      AgentEntity e = new AgentEntity();
      e.setAgentId(id);
      e.setAgencyOf(props.get("Agency"));
      e.setVersion(props.get("Version"));
      result.add(e);
    }

    return result;
  }

}
