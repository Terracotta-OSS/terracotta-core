/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.utils.TSAConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.Representable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public class RemoteCaller {

  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final ContextService contextService;
  private final ExecutorService executorService;
  private final RequestTicketMonitor requestTicketMonitor;
  private final UserService userService;
  private static final Logger LOG = LoggerFactory.getLogger(RemoteCaller.class);


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

  public Map<String, Map<String, String>> getRemoteAgentNodeDetails() throws ServiceExecutionException {
    Map<String, Map<String, String>> nodes = new HashMap<String, Map<String, String>>();
    Set<String> remoteAgentNodeNames = remoteAgentBridgeService.getRemoteAgentNodeNames();
    Map<String, Future<Map<String, String>>> futureMap = new HashMap<String, Future<Map<String, String>>>();

    for (final String remoteAgentNodeName : remoteAgentNodeNames) {
      Future<Map<String, String>> future = executorService.submit(new Callable<Map<String, String>>() {
        @Override
        public Map<String, String> call() throws Exception {
          return remoteAgentBridgeService.getRemoteAgentNodeDetails(remoteAgentNodeName);
        }
      });
      futureMap.put(remoteAgentNodeName, future);
    }

    long timeLeftNanos = TimeUnit.MILLISECONDS.toNanos(remoteAgentBridgeService.getCallTimeout());
    long before = System.nanoTime();

    int numberOfExceptionsThrown = 0;

    for (Map.Entry<String, Future<Map<String, String>>> futureEntry : futureMap.entrySet()) {
      Future<Map<String, String>> future = futureEntry.getValue();
      try {
        Map<String, String> attributes = future.get(Math.max(1L,timeLeftNanos), TimeUnit.NANOSECONDS);
        nodes.put(futureEntry.getKey(), attributes);
      } catch (Exception e) {
        future.cancel(true);
        numberOfExceptionsThrown++;
        LOG.debug("A L1 could not respond in time : ", e);
      } finally {
        timeLeftNanos = timeLeftNanos - (System.nanoTime() - before);
      }
    }
    if (numberOfExceptionsThrown > 0) {
      LOG.warn("There were " + numberOfExceptionsThrown + " exceptions thrown while invoking the L1s");
    }

    return nodes;
  }

  public Object call(final String node, String serviceName, Method method, Object[] args) throws ServiceExecutionException {
    String ticket = requestTicketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());

    final RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
        serviceName, method.getName(), method.getParameterTypes(), args);

    Future<byte[]> future = executorService.submit(new Callable<byte[]>() {
      @Override
      public byte[] call() throws Exception {
        return remoteAgentBridgeService.invokeRemoteMethod(node, remoteCallDescriptor);
      }
    });

    byte[] bytes;
    try {
      bytes = future.get();
    } catch (Exception e) {
      future.cancel(true);
      throw new ServiceExecutionException("Error invoking remote method ", e);
    }
    try {
      Object result = deserialize(bytes);
      if (result instanceof Representable) {
        rewriteAgentId(Collections.singleton((Representable)result), node);
      }
      return result;
    } catch (IOException ioe) {
      throw new ServiceExecutionException("Error deserializing remote response", ioe);
    } catch (ClassNotFoundException cnfe) {
      throw new ServiceExecutionException("Error mapping remote response to local class", cnfe);
    }
  }

  public <T extends Representable> Collection<T> fanOutCollectionCall(final String serviceAgency, Set<String> nodes, final String serviceName, final Method method, final Object[] args) throws ServiceExecutionException {
    final UserInfo userInfo = contextService.getUserInfo();
    Collection<Future<Collection<T>>> futures = new ArrayList<Future<Collection<T>>>();

    for (final String node : nodes) {
      Future<Collection<T>> future = executorService
          .submit(new Callable<Collection<T>>() {
            @Override
            public Collection<T> call() throws Exception {
              String ticket = requestTicketMonitor.issueRequestTicket();
              String token = userService.putUserInfo(userInfo);
              if (serviceAgency != null) {
                Map<String, String> nodeDetails = remoteAgentBridgeService.getRemoteAgentNodeDetails(node);

                if (nodeDetails == null) {
                  return Collections.emptySet();
                }

                String nodeAgency = nodeDetails.get("Agency");
                if (!nodeAgency.equals(serviceAgency)) {
                  return Collections.emptySet();
                }
              }

              RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
                      serviceName, method.getName(), method.getParameterTypes(), args);
              byte[] bytes = remoteAgentBridgeService.invokeRemoteMethod(node, remoteCallDescriptor);
              return rewriteAgentId((Collection<T>) deserialize(bytes), node);
            }
          });
      futures.add(future);
    }
    long timeLeftNanos = TimeUnit.MILLISECONDS.toNanos(remoteAgentBridgeService.getCallTimeout());

    Collection<T> globalResult = new ArrayList<T>();
    int numberOfExceptionsThrown = 0;

    for (Future<Collection<T>> future : futures) {
      long before = System.nanoTime();
      try {
        // future.get(0) is unpredictable
        Collection<T> entities = future.get(Math.max(1L,timeLeftNanos), TimeUnit.NANOSECONDS);
        globalResult.addAll(entities);
      } catch (Exception ee) {
        future.cancel(true);
        numberOfExceptionsThrown++;
        LOG.debug("A L1 could not respond in time : ", ee);
      } finally {
        timeLeftNanos = timeLeftNanos - (System.nanoTime() - before);
      }
    }
    if (numberOfExceptionsThrown > 0) {
      LOG.warn("There were " + numberOfExceptionsThrown + " exceptions thrown while invoking the L1s");
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
