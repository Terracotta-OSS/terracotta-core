/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import static com.terracotta.management.service.impl.util.RemoteManagementSource.toCsv;

import org.terracotta.management.ServiceExecutionException;

import com.tc.license.ProductID;
import com.terracotta.management.resource.AbstractTsaEntityV2;
import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.resource.ConfigEntityV2;
import com.terracotta.management.resource.ServerEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.StatisticsEntityV2;
import com.terracotta.management.resource.ThreadDumpEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;
import com.terracotta.management.security.SecurityContextService;
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

/**
 * @author Ludovic Orban
 */
public class ClientManagementServiceV2 {

  private static final String[]  CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES = new String[] { "ReadRate", "WriteRate" };

  private final LocalManagementSource localManagementSource;
  private final TimeoutService timeoutService;
  private final ServerManagementServiceV2 serverManagementService;
  private final ExecutorService executorService;
  private final RemoteManagementSource remoteManagementSource;
  private final SecurityContextService securityContextService;

  public ClientManagementServiceV2(ServerManagementServiceV2 serverManagementService, ExecutorService executorService, TimeoutService timeoutService, LocalManagementSource localManagementSource, RemoteManagementSource remoteManagementSource, SecurityContextService securityContextService) {
    this.timeoutService = timeoutService;
    this.serverManagementService = serverManagementService;
    this.executorService = executorService;
    this.localManagementSource = localManagementSource;
    this.remoteManagementSource = remoteManagementSource;
    this.securityContextService = securityContextService;
  }


  public Collection<ThreadDumpEntityV2> clientsThreadDump(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "clientsThreadDump", new ForEachClient<ThreadDumpEntityV2>() {
      @Override
      public ThreadDumpEntityV2 queryClient(ObjectName clientObjectName, String clientId) {
        ThreadDumpEntityV2 threadDumpEntityV2 = new ThreadDumpEntityV2();
        threadDumpEntityV2.setNodeType(ThreadDumpEntityV2.NodeType.CLIENT);
        threadDumpEntityV2.setSourceId(clientId);
        try {
          threadDumpEntityV2.setDump(localManagementSource.clientThreadDump(clientObjectName));
        } catch (ManagementSourceException mse) {
          threadDumpEntityV2.setDump("Unavailable");
        }
        return threadDumpEntityV2;
      }

      @Override
      public Collection<ThreadDumpEntityV2> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("diagnostics")
            .path("threadDump")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", clientIds); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", clientProductIds); }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), ThreadDumpEntityV2.class);
      }
    });
  }

  public Collection<ClientEntityV2> getClients(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "getClients", new ForEachClient<ClientEntityV2>() {
      @Override
      public ClientEntityV2 queryClient(ObjectName clientObjectName, String clientId) {
        ClientEntityV2 clientEntityV2 = new ClientEntityV2();
        clientEntityV2.getAttributes().putAll(localManagementSource.getClientAttributes(clientObjectName));
        return clientEntityV2;
      }

      @Override
      public Collection<ClientEntityV2> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("topologies")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", clientIds); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", clientProductIds); }

        Collection<TopologyEntityV2> topologyEntities = remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), TopologyEntityV2.class);

        Collection<ClientEntityV2> result = new ArrayList<ClientEntityV2>();
        for (TopologyEntityV2 topologyEntityV2 : topologyEntities) {
          result.addAll(topologyEntityV2.getClientEntities());
        }
        return result;
      }
    });
  }

  public Collection<StatisticsEntityV2> getClientsStatistics(Set<String> clientIds, Set<ProductID> clientProductIds, final Set<String> attributesToShow) throws ServiceExecutionException {
    final String[] attributeNames = (attributesToShow == null) ?
        CLIENT_STATS_MBEAN_ATTRIBUTE_NAMES :
        new ArrayList<String>(attributesToShow).toArray(new String[attributesToShow.size()]);

    return forEachClient(clientProductIds, clientIds, "getClientsStatistics", new ForEachClient<StatisticsEntityV2>() {
      @Override
      public StatisticsEntityV2 queryClient(ObjectName clientObjectName, String clientId) {
        StatisticsEntityV2 statisticsEntityV2 = new StatisticsEntityV2();
        statisticsEntityV2.setSourceId(clientId);
        try {
          statisticsEntityV2.getStatistics().putAll(localManagementSource.getClientStatistics(clientId, attributeNames));
        } catch (ManagementSourceException e) {
          statisticsEntityV2.getStatistics().put("Error", e.getMessage());
        }
        return statisticsEntityV2;
      }

      @Override
      public Collection<StatisticsEntityV2> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("statistics")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", clientIds); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", clientProductIds); }
        if (attributesToShow != null) { uriBuilder.queryParam("show", toCsv(attributesToShow)); }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), StatisticsEntityV2.class);
      }
    });
  }

  public Collection<ConfigEntityV2> getClientConfigs(Set<String> clientIds, Set<ProductID> clientProductIds) throws ServiceExecutionException {
    return forEachClient(clientProductIds, clientIds, "getClientConfigs", new ForEachClient<ConfigEntityV2>() {
      @Override
      public ConfigEntityV2 queryClient(ObjectName clientObjectName, String clientId) {
        ConfigEntityV2 configEntityV2 = new ConfigEntityV2();
        configEntityV2.setSourceId(clientId);
        try {
          configEntityV2.getAttributes().putAll(localManagementSource.getClientConfig(clientObjectName));
        } catch (ManagementSourceException e) {
          configEntityV2.getAttributes().put("Error", e.getMessage());
        }
        return configEntityV2;
      }

      @Override
      public Collection<ConfigEntityV2> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds) {
        UriBuilder uriBuilder = UriBuilder.fromPath("tc-management-api")
            .path("agents")
            .path("configurations")
            .path("clients");
        if (clientIds != null) { uriBuilder.matrixParam("ids", clientIds); }
        if (clientProductIds != null) { uriBuilder.queryParam("productIds", clientProductIds); }

        return remoteManagementSource.getFromRemoteL2(activeServerName, uriBuilder.build(), ConfigEntityV2.class);
      }
    });
  }

  private String findActiveServerName() throws ServiceExecutionException {
    Collection<ServerGroupEntityV2> serverGroups = serverManagementService.getServerGroups(null);
    for (ServerGroupEntityV2 serverGroup : serverGroups) {
      for (ServerEntityV2 server : serverGroup.getServers()) {
        Object state = server.getAttributes().get("State");
        if ("ACTIVE-COORDINATOR".equals(state)) {
          return (String)server.getAttributes().get("Name");
        }
      }
    }
    return null;
  }

  interface ForEachClient<T extends AbstractTsaEntityV2> {
    T queryClient(ObjectName clientObjectName, String clientId);
    Collection<T> queryActiveServerClients(String activeServerName, Set<String> clientIds, Set<ProductID> clientProductIds);
  }

  private <T extends AbstractTsaEntityV2> Collection<T> forEachClient(Set<ProductID> clientProductIds, Set<String> clientIds, String methodName, final ForEachClient<T> fec) throws ServiceExecutionException {
    if (!localManagementSource.isActiveCoordinator()) {
      String activeServerName = findActiveServerName();
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
