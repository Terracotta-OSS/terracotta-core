package com.tc.services;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.entity.ServerEntityResponseMessage;

/**
 * @author twu
 */
public class CommunicatorResponseHandler extends AbstractEventHandler<ServerEntityResponseMessage> {
  private final CommunicatorService communicatorService;

  public CommunicatorResponseHandler(CommunicatorService communicatorService) {
    this.communicatorService = communicatorService;
  }

  @Override
  public void handleEvent(ServerEntityResponseMessage responseMessage) throws EventHandlerException {
    communicatorService.response(responseMessage.getSourceNodeID(), responseMessage.getResponseId());
  }
}
