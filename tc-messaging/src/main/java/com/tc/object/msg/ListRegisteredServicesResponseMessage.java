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
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.TCSerializableCollection;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public class ListRegisteredServicesResponseMessage extends AbstractManagementMessage {

  private static final byte REMOTE_CALL_DESCRIPTORS = 1;

  private RemoteCallDescriptorCollection remoteCallDescriptors = new RemoteCallDescriptorCollection();

  public ListRegisteredServicesResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ListRegisteredServicesResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public Set<RemoteCallDescriptor> getRemoteCallDescriptors() {
    return remoteCallDescriptors.getAuthority();
  }

  public void setRemoteCallDescriptors(Set<RemoteCallDescriptor> remoteCallDescriptors) {
    this.remoteCallDescriptors.clear();
    this.remoteCallDescriptors.addAll(remoteCallDescriptors);
  }

  @Override
  protected void dehydrateValues() {
    super.dehydrateValues();
    putNVPair(REMOTE_CALL_DESCRIPTORS, remoteCallDescriptors);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (!super.hydrateValue(name)) {
      switch (name) {
        case REMOTE_CALL_DESCRIPTORS:
          remoteCallDescriptors = (RemoteCallDescriptorCollection)getObject(new RemoteCallDescriptorCollection());
          return true;

        default:
          return false;
      }
    } else {
      return true;
    }
  }

  private final static class RemoteCallDescriptorCollection extends TCSerializableCollection<RemoteCallDescriptor> {
    @Override
    protected RemoteCallDescriptor newObject() {
      return new RemoteCallDescriptor();
    }
  }

}
