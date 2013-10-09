/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteAgentEndpoint;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.Representable;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author Ludovic Orban
 */
public class RemoteCaller {

  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final ContextService contextService;
  private final ExecutorService executorService;
  private final RequestTicketMonitor requestTicketMonitor;
  private final UserService userService;

  public RemoteCaller(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService, ExecutorService executorService, RequestTicketMonitor ticketMonitor, UserService userService) {
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.contextService = contextService;
    this.executorService = executorService;
    this.requestTicketMonitor = ticketMonitor;
    this.userService = userService;
  }

  public Set<String> getRemoteAgentNodeNames() throws ServiceExecutionException {
    return remoteAgentBridgeService.getRemoteAgentNodeNames();
  }

  public Map<String,Map<String,String>> getRemoteAgentNodeDetails() throws ServiceExecutionException {
    return remoteAgentBridgeService.getRemoteAgentNodeDetails();
  }

  public Object call(String node, String serviceName, Method method, Object[] args) throws ServiceExecutionException, IOException, ClassNotFoundException {
    String ticket = requestTicketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
        serviceName, method.getName(), method.getParameterTypes(), args);
    byte[] bytes = remoteAgentBridgeService.invokeRemoteMethod(node, RemoteAgentEndpoint.class, remoteCallDescriptor);
    Object result = deserialize(bytes);
    if (result instanceof Representable) {
      rewriteAgentId(Collections.singleton((Representable)result), node);
    }
    return result;
  }

  public <T extends Representable> Collection<T> fanOutCollectionCall(Set<String> nodes, final String serviceName, final Method method, final Object[] args) throws ServiceExecutionException {
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<T>>> futures = new ArrayList<Future<Collection<T>>>();

    final long callTimeout = remoteAgentBridgeService.getCallTimeout();
    for (final String node : nodes) {
      Future<Collection<T>> future = executorService
          .submit(new Callable<Collection<T>>() {
            @Override
            public Collection<T> call() throws Exception {
              String ticket = requestTicketMonitor.issueRequestTicket();
              String token = userService.putUserInfo(userInfo);
              remoteAgentBridgeService.setCallTimeout(callTimeout);

              try {
                RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
                    serviceName, method.getName(), method.getParameterTypes(), args);
                byte[] bytes = remoteAgentBridgeService.invokeRemoteMethod(node, RemoteAgentEndpoint.class, remoteCallDescriptor);
                return rewriteAgentId((Collection<T>)deserialize(bytes), node);
              } finally {
                remoteAgentBridgeService.clearCallTimeout();
              }
            }
          });
      futures.add(future);
    }

    Collection<T> globalResult = new ArrayList<T>();
    for (Future<Collection<T>> future : futures) {
      try {
        Collection<T> entities = future.get();
        globalResult.addAll(entities);
      } catch (ExecutionException ee) {
        if (ee.getCause() instanceof ServiceExecutionException) { throw (ServiceExecutionException) ee.getCause(); }
        throw new ServiceExecutionException(ee);
      } catch (InterruptedException ie) {
        throw new ServiceExecutionException(ie);
      }
    }
    return globalResult;
  }

  private static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes == null) { return null; }

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    try {
      return (T) ois.readObject();
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

}
