/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ClientIDProvider;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.ServiceID;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 *
 */
public class ManagementServicesManagerImpl implements ManagementServicesManager {

  private static final Collection<Method> OBJECT_METHODS = Collections.unmodifiableCollection(new HashSet<Method>(Arrays
      .asList(Object.class.getMethods())));

  private final ConcurrentMap<ServiceID, ServiceHolder> services = new ConcurrentHashMap<ServiceID, ServiceHolder>();

  private final Collection<MessageChannel> messageChannels;
  private final ClientIDProvider clientIDProvider;

  public ManagementServicesManagerImpl(Collection<MessageChannel> messageChannels, ClientIDProvider clientIDProvider) {
    this.messageChannels = messageChannels;
    this.clientIDProvider = clientIDProvider;
  }

  @Override
  public void registerService(ServiceID serviceID, Object service, ExecutorService executorService) {
    services.put(serviceID, new ServiceHolder(service, executorService));
  }

  @Override
  public void unregisterService(ServiceID serviceID) {
    services.remove(serviceID);
  }

  @Override
  public void asyncCall(final RemoteCallHolder remoteCallHolder, final ResponseListener responseListener) {
    ServiceID serviceID = remoteCallHolder.getServiceID();
    final ServiceHolder serviceHolder = services.get(serviceID);
    if (serviceHolder == null) {
      responseListener.onResponse(null, new IllegalArgumentException("No such service: " + serviceID));
      return;
    }

    Method targetMethod = null;

    Method[] methods = serviceHolder.service.getClass().getMethods();
    for_loop:
    for (Method method : methods) {
      if (!method.getName().equals(remoteCallHolder.getMethodName())) { continue; }

      Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes.length != remoteCallHolder.getArgTypeNames().length) { continue; }
      for (int i = 0; i < parameterTypes.length; i++) {
        Class<?> parameterType = parameterTypes[i];
        if (!parameterType.getName().equals(remoteCallHolder.getArgTypeNames()[i])) { continue for_loop; }
      }

      targetMethod = method;
      break;
    }

    if (targetMethod == null) {
      responseListener.onResponse(null, new UnsupportedOperationException("No such method: " + remoteCallHolder.getMethodName() + " with arg types " + Arrays
          .toString(remoteCallHolder.getArgTypeNames())));
      return;
    }

    final Method finalTargetMethod = targetMethod;
    serviceHolder.executorService.submit(new Runnable() {
      @Override
      public void run() {
        Object result = null;
        Exception ex = null;
        try {
          result = finalTargetMethod.invoke(serviceHolder.service, remoteCallHolder.getArgs(serviceHolder.service.getClass().getClassLoader()));
        } catch (Exception e) {
          ex = e;
        }
        responseListener.onResponse(result, ex);
      }
    });
  }

  @Override
  public void sendEvent(TCManagementEvent event) {
    for (MessageChannel messageChannel : messageChannels) {
      ResponseHolder responseHolder = new ResponseHolder();
      responseHolder.setResponse(event);

      InvokeRegisteredServiceResponseMessage response = (InvokeRegisteredServiceResponseMessage)messageChannel.createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE);
      // force the ManagementRequestID as null -> means "event"
      response.setManagementRequestID(null);
      response.setResponseHolder(responseHolder);
      response.send();
    }
  }

  @Override
  public Set<RemoteCallDescriptor> listServices(Set<ServiceID> serviceIDs, boolean includeCallDescriptors) {
    Set<ServiceID> requestedServiceIDs = serviceIDs;
    if (requestedServiceIDs == null || requestedServiceIDs.isEmpty()) {
      requestedServiceIDs = services.keySet();
    }

    Set<RemoteCallDescriptor> response = new HashSet<RemoteCallDescriptor>();

    for (ServiceID requestedServiceID : requestedServiceIDs) {
      ServiceHolder serviceHolder = services.get(requestedServiceID);
      response.addAll(buildRemoteCallDescriptors(requestedServiceID, serviceHolder.service));
    }

    return response;
  }

  private Collection<RemoteCallDescriptor> buildRemoteCallDescriptors(ServiceID serviceID, Object service) {
    Collection<RemoteCallDescriptor> result = new ArrayList<RemoteCallDescriptor>();

    Method[] methods = service.getClass().getMethods();
    for (Method method : methods) {
      if (OBJECT_METHODS.contains(method)) { continue; }

      String name = method.getName();
      List<String> argTypeNames = new ArrayList<String>();
      Class<?>[] parameters = method.getParameterTypes();
      for (Class<?> parameter : parameters) {
        argTypeNames.add(parameter.getName());
      }

      RemoteCallDescriptor descriptor = new RemoteCallDescriptor(clientIDProvider.getClientID(),
          serviceID,
          name,
          argTypeNames.toArray(new String[argTypeNames.size()]));
      result.add(descriptor);
    }

    return result;
  }

  private static final class ServiceHolder {
    private Object service;
    private ExecutorService executorService;

    private ServiceHolder(Object service, ExecutorService executorService) {
      this.service = service;
      this.executorService = executorService;
    }
  }

}
