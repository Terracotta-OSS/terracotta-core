/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import com.terracotta.management.l1bridge.util.RemoteCallerUtility;
import com.terracotta.management.l1bridge.util.RemoteCallerV2Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.l1bridge.RemoteCallDescriptor;
import org.terracotta.management.resource.AbstractEntityV2;
import org.terracotta.management.resource.ExceptionEntityV2;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.utils.TSAConfig;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public class RemoteCallerV2 extends RemoteCaller {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteCallerV2.class);

  public RemoteCallerV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService, ExecutorService executorService, RequestTicketMonitor ticketMonitor, UserService userService, TimeoutService timeoutService) {
    super(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService);
    this.remoteCallerUtility = new RemoteCallerV2Util();
  }

  //For testing
  public RemoteCallerV2(RemoteAgentBridgeService remoteAgentBridgeService, ContextService contextService, ExecutorService executorService, RequestTicketMonitor ticketMonitor, UserService userService, TimeoutService timeoutService,RemoteCallerUtility remoteCallerUtility) {
    super(remoteAgentBridgeService, contextService, executorService, ticketMonitor, userService, timeoutService,remoteCallerUtility);
  }

  public <T extends AbstractEntityV2> ResponseEntityV2<T> fanOutResponseCall(final String serviceAgency, Set<String> nodes, final String serviceName, final Method method, final Object[] args) throws ServiceExecutionException {
    final UserInfo userInfo = contextService.getUserInfo();
    Map<String, Future<ResponseEntityV2<T>>> futures = new HashMap<String, Future<ResponseEntityV2<T>>>();
    final Set<String> clientUUIDs = fetchClientUUIDs();
    for (final String node : nodes) {
      if (node.equals(AbstractEntityV2.EMBEDDED_AGENT_ID)) { continue; }
      try {
        Future<ResponseEntityV2<T>> future = executorService.submit(new Callable<ResponseEntityV2<T>>() {
          @Override
          public ResponseEntityV2<T> call() throws Exception {
            String ticket = requestTicketMonitor.issueRequestTicket();
            String token = userService.putUserInfo(userInfo);

            if (serviceAgency != null) {
              Map<String, String> nodeDetails = remoteAgentBridgeService.getRemoteAgentNodeDetails(node);
              String nodeAgency = nodeDetails.get("Agency");
              if (!serviceAgency.equals(nodeAgency)) {
                return new ResponseEntityV2<T>();
              }
            }

            RemoteCallDescriptor remoteCallDescriptor = new RemoteCallDescriptor(ticket, token, TSAConfig.getSecurityCallbackUrl(),
                serviceName, method.getName(), method.getParameterTypes(), args,clientUUIDs);
            byte[] bytes = remoteAgentBridgeService.invokeRemoteMethod(node, remoteCallDescriptor);
            return deserializeAndRewriteAgentId(bytes, node);
          }
        });
        futures.put(node, future);
      } catch (RejectedExecutionException ree) {
        LOG.debug("L1 thread pool rejected task, throttling a bit before resuming fan-out call...", ree);
        try {
          Thread.sleep(100L);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }


    long timeLeft = timeoutService.getCallTimeout();
    ResponseEntityV2<T> globalResult = new ResponseEntityV2<T>();

    for (Map.Entry<String, Future<ResponseEntityV2<T>>> entry : futures.entrySet()) {
      String node = entry.getKey();
      Future<ResponseEntityV2<T>> future = entry.getValue();

      long before = System.nanoTime();
      try {
        ResponseEntityV2<T> resp = future.get(Math.max(1L, timeLeft), TimeUnit.MILLISECONDS);
        globalResult.getEntities().addAll(resp.getEntities());
        globalResult.getExceptionEntities().addAll(resp.getExceptionEntities());
      } catch (Exception e) {
        ExceptionEntityV2 e1 = new ExceptionEntityV2(e);
        e1.setAgentId(node);
        e1.setMessage("Agent failed to respond to " + serviceName + "." + method.getName());
        globalResult.getExceptionEntities().add(e1);

        future.cancel(true);
        LOG.debug("Future execution error in {}.{}", serviceName, method.getName(), e);
      } finally {
        timeLeft -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
      }
    }
    return globalResult;
  }

  @Override
  protected void rewriteAgentId(Object obj, String agentId) {
    if (obj instanceof ResponseEntityV2) {
      ResponseEntityV2<AbstractEntityV2> entityV2 = (ResponseEntityV2)obj;
      for (AbstractEntityV2 abstractEntityV2 : entityV2.getEntities()) {
        abstractEntityV2.setAgentId(agentId);
      }
      for (ExceptionEntityV2 exceptionEntityV2 : entityV2.getExceptionEntities()) {
        exceptionEntityV2.setAgentId(agentId);
      }
    } else {
      super.rewriteAgentId(obj, agentId);
    }
  }

  protected Set<String> fetchClientUUIDs(){
    Set<String> clientUUIDs = null;
    try {
      clientUUIDs = this.remoteCallerUtility.fetchClientUUIDs();
    } catch (ServiceExecutionException e) {
      LOG.error("Failed to fetch client UUIDS ", e);
    }
    return clientUUIDs;
  }
}
