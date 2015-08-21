package com.tc.objectserver.entity;

import com.tc.net.NodeID;
import com.tc.object.EntityDescriptor;
import com.tc.object.net.DSOChannelManagerEventListener;

/**
 * @author twu
 */
public interface ClientEntityStateManager extends DSOChannelManagerEventListener {

  boolean addReference(NodeID nodeID, EntityDescriptor entityDescriptor);

  boolean removeReference(NodeID nodeID, EntityDescriptor entityDescriptor);
}
