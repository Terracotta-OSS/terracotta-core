/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.management.RemoteCallDescriptor;
import com.tc.object.management.RemoteCallHolder;
import com.tc.object.session.SessionID;

import java.io.IOException;

/**
 *
 */
public class InvokeRegisteredServiceMessage extends AbstractManagementMessage {

  private static final byte REMOTE_CALL_HOLDER = 1;

  private RemoteCallHolder remoteCallHolder;


  public InvokeRegisteredServiceMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public InvokeRegisteredServiceMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel, TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public RemoteCallHolder getRemoteCallHolder() {
    return remoteCallHolder;
  }

  public void setRemoteCallHolder(RemoteCallHolder remoteCallHolder) {
    this.remoteCallHolder = remoteCallHolder;
  }

  @Override
  protected void dehydrateValues() {
    super.dehydrateValues();
    putNVPair(REMOTE_CALL_HOLDER, remoteCallHolder);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    if (!super.hydrateValue(name)) {
      switch (name) {
        case REMOTE_CALL_HOLDER:
          remoteCallHolder = (RemoteCallHolder) this.<RemoteCallDescriptor>getObject(new RemoteCallHolder());
          return true;

        default:
          return false;
      }
    } else {
      return true;
    }
  }

}
