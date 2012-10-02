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
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;
import org.terracotta.management.resource.services.LicenseService;
import org.terracotta.management.resource.services.LicenseServiceImpl;
import org.terracotta.management.resource.services.validator.RequestValidator;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

/**
 * @author Ludovic Orban
 */
public class JmxRepositoryService implements EntityResourceFactory, CacheManagerService, CacheService, AgentService {

  private final static Set<String> DFLT_ATTRS = new HashSet<String>(Arrays.asList(new String[] { "Name" }));

  private final MBeanServerConnection mBeanServerConnection;
  private static JmxEhcacheRequestValidator requestValidator;

  public static void create() {
    try {
      // JMXServiceURL target = new JMXServiceURL(serviceURL);
      // JMXConnector connector = JMXConnectorFactory.connect(target);
      // MBeanServerConnection mBeanServerConnection = connector.getMBeanServerConnection();

      MBeanServer findMBeanServer = ManagementFactory.getPlatformMBeanServer();
      JmxRepositoryService repoSvc = new JmxRepositoryService(findMBeanServer);
      requestValidator = new JmxEhcacheRequestValidator(findMBeanServer);
//      JmxRepositoryService repoSvc = new JmxRepositoryService(mBeanServerConnection);
//      requestValidator = new JmxEhcacheRequestValidator(mBeanServerConnection);
      ServiceLocator locator = new ServiceLocator().loadService(LicenseService.class, new LicenseServiceImpl(true))
                                                   .loadService(RequestValidator.class, requestValidator)
                                                   .loadService(CacheManagerService.class, repoSvc)
                                                   .loadService(CacheService.class, repoSvc)
                                                   .loadService(EntityResourceFactory.class, repoSvc)
                                                   .loadService(AgentService.class, repoSvc);
      ServiceLocator.load(locator);
    } catch (Exception ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public JmxRepositoryService(MBeanServerConnection mBeanServerConnection) {
    this.mBeanServerConnection = mBeanServerConnection;
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
  public void updateCacheManager(String cacheManagerName, CacheManagerEntity resource) throws ServiceExecutionException {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke("updateCacheManager", new Class<?>[] { String.class, CacheManagerEntity.class }, new Object[] { cacheManagerName, resource });
  }

  @Override
  public void clearCacheStats(String cacheManagerName, String cacheName) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke("clearCacheStats", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName, String cacheName, CacheEntity resource) throws ServiceExecutionException {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke("createOrUpdateCache", new Class<?>[] { String.class, String.class, CacheEntity.class }, new Object[] { cacheManagerName, cacheName, resource });
  }

  @Override
  public void clearCache(String cacheManagerName, String cacheName) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    mbean.invoke("clearCache", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(Set<String> cacheManagerNames, Set<String> attributes) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("createCacheManagerEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, attributes });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(Set<String> cacheManagerNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("createCacheManagerConfigEntities", new Class<?>[] { Set.class }, new Object[] { cacheManagerNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(Set<String> cacheManagerNames, Set<String> cacheNames, Set<String> attributes) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("createCacheEntities", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, attributes });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(Set<String> cacheManagerNames, Set<String> cacheNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("createCacheConfigEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(Set<String> cacheManagerNames, Set<String> cacheNames, Set<String> statNames) {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("createCacheStatisticSampleEntity", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, statNames });
    return deserialize(bytes);
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    ObjectName repositoryService = getRepositoryServiceName(requestValidator.getValidatedNode());

    DfltSamplerRepositoryServiceMBean mbean = JMX.newMBeanProxy(mBeanServerConnection, repositoryService, DfltSamplerRepositoryServiceMBean.class);
    byte[] bytes = mbean.invoke("getAgentsMetadata", new Class<?>[] { Set.class }, new Object[] { ids });
    return deserialize(bytes);
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
