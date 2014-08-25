/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.VersionedEntity;

import com.tc.license.ProductID;
import com.terracotta.management.resource.ClientEntity;
import com.terracotta.management.resource.ConfigEntity;
import com.terracotta.management.resource.StatisticsEntity;
import com.terracotta.management.resource.ThreadDumpEntity;
import com.terracotta.management.resource.TopologyEntity;
import com.terracotta.management.resource.services.utils.ProductIdConverter;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.ManagementSourceException;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.management.ObjectName;
import javax.ws.rs.core.UriBuilder;

import static com.terracotta.management.service.impl.util.RemoteManagementSource.toCsv;

/**
 * @author Ludovic Orban
 */
public class ClientManagementService {

  private static final String[]  CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "ReadRate", "WriteRate" };

  private final LocalManagementSource localManagementSource;
  private final TimeoutService timeoutService;
  private final L1MBeansSource l1MBeansSource;
  private final ExecutorService executorService;
  private final RemoteManagementSource remoteManagementSource;
  private final SecurityContextService securityContextService;

  public ClientManagementService(L1MBeansSource l1MBeansSource, ExecutorService executorService, TimeoutService timeoutService, LocalManagementSource localManagementSource, RemoteManagementSource remoteManagementSource, SecurityContextService securityContextService) {
    this.timeoutService = timeoutService;
    this.l1MBeansSource = l1MBeansSource;
    this.executorService = executorService;
    this.localManagementSource = localManagementSource;
    this.remoteManagementSource = remoteManagementSource;
    this.securityContextService = securityContextService;
  }


  public Collection<ThreadDumpEntity> clientsThreadDump(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "clientsThreadDump", new ForEachClient<ThreadDumpEntity>() {
      @Override
      public ThreadDumpEntity queryClient(ObjectName clientObjectName, String clientId) {
        ThreadDumpEntity threadDumpEntity = new ThreadDumpEntity();
        threadDumpEntity.setNodeType(ThreadDumpEntity.NodeType.CLIENT);
        threadDumpEntity.setVersion(localManagementSource.getVersion());
        threadDumpEntity.setSourceId(clientId);
        try {
          threadDumpEntity.setDump(localManagementSource.clientThreadDump(clientObjectName));
        } catch (ManagementSourceException mse) {
          threadDumpEntity.setDump("Unavailable");
        }
        return threadDumpEntity;
      }

      @Override
      public Collection<ThreadDumpEntity> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("threadDump")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", toCsv(clientIds)); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", toCsv(ProductIdConverter.productIdsToStrings(clientProductIds))); }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), Collection.class, ThreadDumpEntity.class);
      }
    });
  }

  public Collection<ClientEntity> getClients(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "getClients", new ForEachClient<ClientEntity>() {
      @Override
      public ClientEntity queryClient(ObjectName clientObjectName, String clientId) {
        ClientEntity clientEntity = new ClientEntity();
        Map<String, Object> clientAttributes = localManagementSource.getClientAttributes(clientObjectName);
        clientEntity.setVersion((String)clientAttributes.remove("MavenArtifactsVersion"));
        clientEntity.getAttributes().putAll(clientAttributes);
        return clientEntity;
      }

      @Override
      public Collection<ClientEntity> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("topologies")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", toCsv(clientIds)); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", toCsv(ProductIdConverter.productIdsToStrings(clientProductIds))); }

        Collection<TopologyEntity> topologyEntities = remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), Collection.class, TopologyEntity.class);

        Collection<ClientEntity> result = new ArrayList<ClientEntity>();
        for (TopologyEntity topologyEntity : topologyEntities) {
          result.addAll(topologyEntity.getClientEntities());
        }
        return result;
      }
    });
  }

  public Collection<StatisticsEntity> getClientsStatistics(Set<String> clientIds, Set<ProductID> clientProductIds, final Set<String> attributesToShow) throws ServiceExecutionException {
    final String[] attributeNames = (attributesToShow == null) ?
        CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    return forEachClient(clientProductIds, clientIds, "getClientsStatistics", new ForEachClient<StatisticsEntity>() {
      @Override
      public StatisticsEntity queryClient(ObjectName clientObjectName, String clientId) {
        StatisticsEntity statisticsEntity = new StatisticsEntity();
        statisticsEntity.setSourceId(clientId);
        statisticsEntity.setVersion(localManagementSource.getVersion());
        try {
          statisticsEntity.getStatistics().putAll(localManagementSource.getClientStatistics(clientId, attributeNames));
        } catch (ManagementSourceException e) {
          statisticsEntity.getStatistics().put("Error", e.getMessage());
        }
        return statisticsEntity;
      }

      @Override
      public Collection<StatisticsEntity> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("statistics")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", toCsv(clientIds)); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", toCsv(ProductIdConverter.productIdsToStrings(clientProductIds))); }
        if (attributesToShow != null) {
          for (String attr : attributesToShow) {
            uriBuilder.queryParam("show", attr);
          }
        }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), Collection.class, StatisticsEntity.class);
      }
    });
  }

  public Collection<ConfigEntity> getClientConfigs(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "getClientConfigs", new ForEachClient<ConfigEntity>() {
      @Override
      public ConfigEntity queryClient(ObjectName clientObjectName, String clientId) {
        ConfigEntity configEntity = new ConfigEntity();
        configEntity.setVersion(localManagementSource.getVersion());
        configEntity.setSourceId(clientId);
        try {
          configEntity.getAttributes().putAll(localManagementSource.getClientConfig(clientObjectName));
        } catch (ManagementSourceException e) {
          configEntity.getAttributes().put("Error", e.getMessage());
        }
        return configEntity;
      }

      @Override
      public Collection<ConfigEntity> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("configurations")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", toCsv(clientIds)); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", toCsv(ProductIdConverter.productIdsToStrings(clientProductIds))); }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), Collection.class, ConfigEntity.class);
      }
    });
  }


  interface ForEachClient<T extends VersionedEntity> {
    T queryClient(ObjectName clientObjectName, String clientId);
    Collection<T> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds);
  }

  private <T extends VersionedEntity> Collection<T> forEachClient(Set<ProductID> clientProductIds, Set<String> clientIds, String methodName, final ForEachClient<T> fec) throws ServiceExecutionException {
    if (!l1MBeansSource.containsJmxMBeans()) {
      String activeServerName = l1MBeansSource.getActiveL2ContainingMBeansName();
      if (activeServerName == null) {
        // there's no active at this time
        return Collections.emptySet();
      }
      return fec.queryActiveServerClients(activeServerName, clientIds, clientProductIds);
    }

    Map<String, Future<T>> futures = new HashMap<String, Future<T>>();

    Collection<ObjectName> clientObjectNames = localManagementSource.fetchClientObjectNames(clientProductIds);
    for (final ObjectName clientObjectName : clientObjectNames) {
      final String clientId = localManagementSource.getClientID(clientObjectName);
      if (clientIds != null && !clientIds.contains(clientId)) {
        continue;
      }

      final SecurityContextService.SecurityContext context = securityContextService.getSecurityContext();
      Future<T> future = executorService.submit(new Callable<T>() {
        @Override
        public T call() throws Exception {
          securityContextService.setSecurityContext(context);
          try {
            return fec.queryClient(clientObjectName, clientId);
          } finally {
            securityContextService.clearSecurityContext();
          }
        }
      });
      futures.put(clientId, future);
    }

    try {
      return remoteManagementSource.collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), methodName, Integer.MAX_VALUE);
    } catch (Exception e) {
      remoteManagementSource.cancelFutures(futures.values());
      throw new ServiceExecutionException("error collecting client data via " + methodName, e);
    }
  }

}
