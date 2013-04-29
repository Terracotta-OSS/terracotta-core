package com.tc.object.msg;

import com.tc.net.NodeID;

/**
 * @author Eugene Shelestovich
 */
public interface ServerEventListenerMessageFactory {

  RegisterServerEventListenerMessage newRegisterServerEventListenerMessage(NodeID nodeID);

  UnregisterServerEventListenerMessage newUnregisterServerEventListenerMessage(NodeID nodeID);
}
