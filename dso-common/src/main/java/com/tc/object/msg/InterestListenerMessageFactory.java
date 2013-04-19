package com.tc.object.msg;

import com.tc.net.NodeID;
import com.tc.object.InterestType;

import java.util.Set;

/**
 * @author Eugene Shelestovich
 */
public interface InterestListenerMessageFactory {

  RegisterInterestListenerMessage newRegisterInterestListenerMessage(NodeID nodeID);

  UnregisterInterestListenerMessage newUnregisterInterestListenerMessage(NodeID nodeID);
}
