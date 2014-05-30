/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.management.ManagementServicesManager;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.management.ResponseHolder;
import com.tc.object.management.ServiceID;
import com.tc.object.management.TCManagementSerializationException;
import com.tc.object.msg.InvokeRegisteredServiceMessage;
import com.tc.object.msg.InvokeRegisteredServiceResponseMessage;
import com.tc.object.msg.ListRegisteredServicesMessage;
import com.tc.object.msg.ListRegisteredServicesResponseMessage;
import com.tc.util.Assert;

import java.util.Set;

/**
 *
 */
public class ClientManagementHandler extends AbstractEventHandler {
  private final ManagementServicesManager managementServicesManager;

  public ClientManagementHandler(ManagementServicesManager managementServicesManager) {
    this.managementServicesManager = managementServicesManager;
  }

  @Override
  public void handleEvent(EventContext context) throws EventHandlerException {
    if (context instanceof InvokeRegisteredServiceMessage) {
      final InvokeRegisteredServiceMessage request = (InvokeRegisteredServiceMessage)context;
      RemoteCallHolder remoteCallHolder = request.getRemoteCallHolder();

      managementServicesManager.asyncCall(remoteCallHolder, new ManagementServicesManager.ResponseListener() {
        @Override
        public void onResponse(Object responseObject, Exception exception) {
          ResponseHolder responseHolder = new ResponseHolder();
          try {
            responseHolder.setResponse(responseObject);
            responseHolder.setException(exception);
          } catch (TCManagementSerializationException se) {
            responseHolder.setException(se);
          }

          InvokeRegisteredServiceResponseMessage response = (InvokeRegisteredServiceResponseMessage)request.getChannel()
              .createMessage(TCMessageType.INVOKE_REGISTERED_SERVICE_RESPONSE_MESSAGE);
          response.setResponseHolder(responseHolder);
          response.setManagementRequestID(request.getManagementRequestID());
          response.send();
        }
      });
    } else if (context instanceof ListRegisteredServicesMessage) {
      ListRegisteredServicesMessage request = (ListRegisteredServicesMessage)context;

      Set<ServiceID> serviceIDs = request.getServiceIDs();
      boolean includeCallDescriptors = request.isIncludeCallDescriptors();

      Set<RemoteCallDescriptor> descriptors = managementServicesManager.listServices(serviceIDs, includeCallDescriptors);

      ListRegisteredServicesResponseMessage response = (ListRegisteredServicesResponseMessage)request.getChannel().createMessage(TCMessageType.LIST_REGISTERED_SERVICES_RESPONSE_MESSAGE);
      response.setRemoteCallDescriptors(descriptors);
      response.setManagementRequestID(request.getManagementRequestID());
      response.send();
    } else {
      Assert.fail("Unknown event type " + context.getClass().getName());
    }
  }

}
