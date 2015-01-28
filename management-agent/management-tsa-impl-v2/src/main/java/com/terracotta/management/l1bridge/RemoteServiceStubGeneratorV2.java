/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import com.terracotta.management.l1bridge.util.RemoteCallerUtility;
import org.terracotta.management.resource.ErrorEntity;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.L1MBeansSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class RemoteServiceStubGeneratorV2 {

  private final RemoteCallerV2 remoteCaller;
  private final RemoteRequestValidator requestValidator;
  private final L1MBeansSource l1MBeansSource;

  public RemoteServiceStubGeneratorV2(RequestTicketMonitor requestTicketMonitor, UserService userService,
                                      ContextService contextService, RemoteRequestValidator requestValidator,
                                      RemoteAgentBridgeService remoteAgentBridgeService, ExecutorService executorService,
                                      TimeoutService timeoutService, L1MBeansSource l1MBeansSource) {
    this.requestValidator = requestValidator;
    this.l1MBeansSource = l1MBeansSource;
    this.remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);
  }

  //for testing
  public RemoteServiceStubGeneratorV2(RequestTicketMonitor requestTicketMonitor, UserService userService,
                                      ContextService contextService, RemoteRequestValidator requestValidator,
                                      RemoteAgentBridgeService remoteAgentBridgeService, ExecutorService executorService,
                                      TimeoutService timeoutService, L1MBeansSource l1MBeansSource,RemoteCallerUtility remoteCallerUtility) {
    this.requestValidator = requestValidator;
    this.l1MBeansSource = l1MBeansSource;
    this.remoteCaller = new RemoteCallerV2(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService,remoteCallerUtility);
  }



  public <T> T newRemoteService(Class<T> clazz, String agency) {
    return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { clazz }, new RemoteInvocationHandler(clazz.getName(), agency));
  }

  private final class RemoteInvocationHandler implements InvocationHandler {

    private final String serviceName;
    private final String agency;

    private RemoteInvocationHandler(String serviceName, String agency) {
      this.serviceName = serviceName;
      this.agency = agency;
    }

    @Override
    public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
      if (l1MBeansSource.containsJmxMBeans()) {
        return invokeOnLocalServer(method, args);
      } else {
        // cannot handle the request on this server, find an active to do the job
        l1MBeansSource.proxyClientRequest();
        return null;
      }
    }

    private Object invokeOnLocalServer(Method method, Object[] args) throws org.terracotta.management.ServiceExecutionException {
      Set<String> nodes = requestValidator.getValidatedNodes();
      if (nodes == null) {
        throw new RuntimeException("Request has not been validated which prevents it from being bridged to the L1s. Bug?");
      }
      if (nodes.isEmpty()) {
        ErrorEntity errorEntity = new ErrorEntity();
        errorEntity.setError("No connected client");
        errorEntity.setDetails("No L1 to send the request to, try again later.");
        throw new WebApplicationException(Response.status(404).entity(errorEntity).build());
      }

      if (ResponseEntityV2.class.isAssignableFrom(method.getReturnType())) {
        return remoteCaller.fanOutResponseCall(agency, nodes, serviceName, method, args);
      } else {
        String node = requestValidator.getSingleValidatedNode();
        return remoteCaller.call(node, serviceName, method, args);
      }
    }

  }

}
