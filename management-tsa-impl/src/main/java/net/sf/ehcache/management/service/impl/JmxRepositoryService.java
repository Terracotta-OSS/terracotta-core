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
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.config.TSAConfig;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class JmxRepositoryService implements EntityResourceFactory, CacheManagerService, CacheService, AgentService {

  private final static Set<String> DFLT_ATTRS = new HashSet<String>(Arrays.asList(new String[] { "Name" }));

  private final MBeanServerConnection mBeanServerConnection;
  private final JmxEhcacheRequestValidator requestValidator;
  private final RequestTicketMonitor ticketMonitor;
  private final ContextService contextService;
  private final UserService userService;

  public JmxRepositoryService(MBeanServerConnection mBeanServerConnection, JmxEhcacheRequestValidator requestValidator,
                              RequestTicketMonitor ticketMonitor, ContextService contextService, UserService userService) {
    this.mBeanServerConnection = mBeanServerConnection;
    this.requestValidator = requestValidator;
    this.ticketMonitor = ticketMonitor;
    this.contextService = contextService;
    this.userService = userService;
  }

  private ObjectName getRepositoryServiceName(String node) {
    try {
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*,node=" + node), null);
      if (objectNames.size() != 1) {
        throw new AssertionError("Found " + objectNames.size() + " ObjectNames for node : " + node + " - should be == 1");
      }
      return objectNames.iterator().next();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> T deserialize(byte[] bytes) {
    try {
      ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
      try {
        return (T)ois.readObject();
      } finally {
        ois.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void updateCacheManager(String cacheManagerName, CacheManagerEntity resource) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "updateCacheManager", new Class<?>[] { String.class, CacheManagerEntity.class }, new Object[] { cacheManagerName, resource });
  }

  @Override
  public void clearCacheStats(String cacheManagerName, String cacheName) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "clearCacheStats", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName, String cacheName, CacheEntity resource) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createOrUpdateCache", new Class<?>[] { String.class, String.class, CacheEntity.class }, new Object[] { cacheManagerName, cacheName, resource });
  }

  @Override
  public void clearCache(String cacheManagerName, String cacheName) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "clearCache", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames, Set<String> attributes) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createCacheManagerEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, attributes });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createCacheManagerConfigEntities", new Class<?>[] { Set.class }, new Object[] { cacheManagerNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames, Set<String> cacheNames, Set<String> attributes) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createCacheEntities", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, attributes });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames, Set<String> cacheNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createCacheConfigEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames, Set<String> cacheNames, Set<String> statNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "createCacheStatisticSampleEntity", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, statNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> idSet) throws ServiceExecutionException {
    Collection<AgentMetadataEntity> result = new ArrayList<AgentMetadataEntity>();

    Set<String> nodes = getNodes();
    if (idSet.isEmpty()) {
      idSet = nodes;
    }

    UserInfo userInfo = contextService.getUserInfo();
    for (String id : idSet) {
      if (!nodes.contains(id)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }

      requestValidator.setValidatedNode(id);
      ObjectName repositoryService = getRepositoryServiceName(id);

      String ticket = ticketMonitor.issueRequestTicket();
      String token = userService.putUserInfo(userInfo);

      DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
      byte[] bytes = mbean.invoke(ticket, token, TSAConfig.getSecurityCallbackUrl(), "getAgentsMetadata", new Class<?>[] { Set.class }, new Object[] { Collections.emptySet() });
      Collection<AgentMetadataEntity> resp = deserialize(bytes);
      for(AgentMetadataEntity ame :  resp) {
        ame.setAgentId(id);
      }
      result.addAll(resp);
    }

    return result;
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> idSet) throws ServiceExecutionException {
    Collection<AgentEntity> result = new ArrayList<AgentEntity>();

    Set<String> nodes = getNodes();
    if (idSet.isEmpty()) {
      idSet = nodes;
    }

    for (String id : idSet) {
      if (!nodes.contains(id)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }

      AgentEntity e = new AgentEntity();
      e.setAgentId(id);

      Collection<Representable> reps = new HashSet<Representable>();
      requestValidator.setValidatedNode(id);
      reps.addAll(createCacheManagerEntities(null, DFLT_ATTRS));
      e.setRootRepresentables(reps);

      result.add(e);
    }

    return result;
  }

  private Set<String> getNodes() {
    try {
      Set<String> nodes = new HashSet<String>();
      Set<ObjectName> objectNames = mBeanServerConnection.queryNames(new ObjectName("net.sf.ehcache:type=RepositoryService,*"), null);
      for (ObjectName objectName : objectNames) {
        String node = objectName.getKeyProperty("node");
        nodes.add(node);
      }
      return nodes;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
