/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.ServiceID;
import com.tc.object.management.TCSerializableCollection;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public class ListRegisteredServicesMessage extends AbstractManagementMessage {

  private static final byte SERVICE_IDS = 1;
  private static final byte INCLUDE_CALL_DESCRIPTORS = 2;

  private ServiceIDCollection serviceIDs = new ServiceIDCollection();
  private boolean includeCallDescriptors;

  public ListRegisteredServicesMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ListRegisteredServicesMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }


  public Set<ServiceID> getServiceIDs() {
    return serviceIDs.getAuthority();
  }

  public void setServiceIDs(Set<ServiceID> serviceIDs) {
    this.serviceIDs.clear();
    this.serviceIDs.addAll(serviceIDs);
  }

  public boolean isIncludeCallDescriptors() {
    return includeCallDescriptors;
  }

  public void setIncludeCallDescriptors(boolean includeCallDescriptors) {
    this.includeCallDescriptors = includeCallDescriptors;
  }

  @Override
  protected void dehydrateValues() {
    super.dehydrateValues();
    putNVPair(SERVICE_IDS, serviceIDs);
    putNVPair(INCLUDE_CALL_DESCRIPTORS, includeCallDescriptors);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (!super.hydrateValue(name)) {
      switch (name) {
        case SERVICE_IDS:
          serviceIDs = (ServiceIDCollection)getObject(new ServiceIDCollection());
          return true;

        case INCLUDE_CALL_DESCRIPTORS:
          includeCallDescriptors = getBooleanValue();
          return true;

        default:
          return false;
      }
    } else {
      return true;
    }
  }

  private static class ServiceIDCollection extends TCSerializableCollection<ServiceID> {
    @Override
    protected ServiceID newObject() {
      return new ServiceID();
    }
  }

}
