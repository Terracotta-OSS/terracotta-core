package com.terracotta.management.l1bridge;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * @author Ludovic Orban
 */
public class RemoteServiceStubGenerator {

  private final RemoteCaller remoteCaller;
  private final RemoteRequestValidator requestValidator;

  public RemoteServiceStubGenerator(RequestTicketMonitor requestTicketMonitor, UserService userService,
                                    ContextService contextService, RemoteRequestValidator requestValidator,
                                    RemoteAgentBridgeService remoteAgentBridgeService, ExecutorService executorService) {
    this.requestValidator = requestValidator;
    this.remoteCaller = new RemoteCaller(remoteAgentBridgeService, contextService, executorService, requestTicketMonitor, userService);
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
      if (Collection.class.isAssignableFrom(method.getReturnType())) {
        Set<String> nodes = requestValidator.getValidatedNodes();
        if (nodes == null) {
          throw new RuntimeException("Request has not been validated which prevents it from being bridged to the L1s. Bug?");
        }
        return remoteCaller.fanOutCollectionCall(agency, nodes, serviceName, method, args);
      } else {
        String node = requestValidator.getSingleValidatedNode();
        if (node == null) {
          throw new RuntimeException("Request has not been validated which prevents it from being bridged to the L1s. Bug?");
        }
        return remoteCaller.call(node, serviceName, method, args);
      }
    }

  }

}
