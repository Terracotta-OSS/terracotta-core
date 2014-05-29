package com.terracotta.management.l1bridge;

import org.terracotta.management.resource.ErrorEntity;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.service.TimeoutService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
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

  public RemoteServiceStubGenerator(RequestTicketMonitor requestTicketMonitor, UserService userService,
                                    ContextService contextService, RemoteRequestValidator requestValidator,
                                    RemoteAgentBridgeService remoteAgentBridgeService, ExecutorService executorService,
                                    TimeoutService timeoutService) {
    this.requestValidator = requestValidator;
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
      Set<String> nodes = requestValidator.getValidatedNodes();
      if (nodes == null) {
        throw new RuntimeException("Request has not been validated which prevents it from being bridged to the L1s. Bug?");
      }
      if (nodes.isEmpty()) {
        ErrorEntity errorEntity = new ErrorEntity();
        errorEntity.setDetails("No L1 to send the request to, try again later.");
        Response response =  Response.status(404).entity(errorEntity).build();
        WebApplicationException webApplicationException = new WebApplicationException(response);
        throw webApplicationException;
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
