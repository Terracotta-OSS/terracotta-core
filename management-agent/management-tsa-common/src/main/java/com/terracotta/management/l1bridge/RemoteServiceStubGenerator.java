/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.terracotta.management.l1bridge;

import org.terracotta.management.resource.ErrorEntity;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.ActiveServerSource;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.web.proxy.ProxyException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * @author Ludovic Orban
 */
public class RemoteServiceStubGenerator {

  private final RemoteCaller remoteCaller;
  private final RemoteRequestValidator requestValidator;
  private final ActiveServerSource activeServerSource;

  public RemoteServiceStubGenerator(RequestTicketMonitor requestTicketMonitor, UserService userService,
                                    ContextService contextService, RemoteRequestValidator requestValidator,
                                    RemoteAgentBridgeService remoteAgentBridgeService, ExecutorService executorService,
                                    TimeoutService timeoutService, ActiveServerSource activeServerSource) {
    this.requestValidator = requestValidator;
    this.activeServerSource = activeServerSource;
    this.remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService, timeoutService);
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
      if (activeServerSource.isCurrentServerActive()) {
        return invokeActiveCase(method, args);
      } else {
        // cannot handle the request on this server, find an active to do the job
        List<String> activeL2Urls = activeServerSource.getActiveL2Urls();
        if (activeL2Urls.isEmpty()) {
          ErrorEntity errorEntity = new ErrorEntity();
          errorEntity.setError("No active coordinator");
          errorEntity.setDetails("No server is in the ACTIVE-COORDINATOR state, try again later.");
          throw new WebApplicationException(Response.status(404).entity(errorEntity).build());
        }
        Collections.shuffle(activeL2Urls);
        String activeL2Url = activeL2Urls.get(0);
        throw new ProxyException(activeL2Url);
      }
    }

    private Object invokeActiveCase(Method method, Object[] args) throws org.terracotta.management.ServiceExecutionException {Set<String> nodes = requestValidator.getValidatedNodes();
      if (nodes == null) {
        throw new RuntimeException("Request has not been validated which prevents it from being bridged to the L1s. Bug?");
      }
      if (nodes.isEmpty()) {
        ErrorEntity errorEntity = new ErrorEntity();
        errorEntity.setError("No connected client");
        errorEntity.setDetails("No L1 to send the request to, try again later.");
        throw new WebApplicationException(Response.status(404).entity(errorEntity).build());
      }

      if (nodes.size() > 1 && Collection.class.isAssignableFrom(method.getReturnType())) {
        return remoteCaller.fanOutCollectionCall(agency, nodes, serviceName, method, args);
      } else {
        String node = requestValidator.getSingleValidatedNode();
        return remoteCaller.call(node, serviceName, method, args);
      }
    }

  }

}
