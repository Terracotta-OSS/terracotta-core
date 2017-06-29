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
package com.tc.objectserver.entity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.entity.VoltronEntityMessage;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Map<ClientDescriptorImpl, EntityID> clientStates = new ConcurrentHashMap<>();
  private static final Logger logger    = LoggerFactory.getLogger(ClientEntityStateManagerImpl.class);

  public ClientEntityStateManagerImpl() {
  }

  @Override
  public boolean addReference(ClientDescriptorImpl instance, EntityID eid) {
    EntityID check = clientStates.put(instance, eid);
    Assert.assertNull(check);
    logger.debug("Adding reference:" + instance + " " + eid);
    // We currently assume that we are being used precisely:  all add/remove calls are expected to have a specific meaning.
    return true;
  }

  @Override
  public boolean removeReference(ClientDescriptorImpl descriptor) {
    EntityID eid = clientStates.remove(descriptor);
    Assert.assertNotNull(eid);
    logger.debug("Removing reference:" + descriptor + " " + eid);
    return true;
  }

  @Override
  public boolean verifyNoReferences(EntityID eid) {
    return !clientStates.values().stream().anyMatch((led)->led.equals(eid));
  }

  @Override
  public List<VoltronEntityMessage> clientDisconnected(ClientID client) {
    ArrayList<VoltronEntityMessage> msgs = new ArrayList<>();

    clientStates.entrySet().stream().filter(e->e.getKey().getNodeID().equals(client)).forEach(e->
            //  don't care about version for reslease.  Is this OK?
      msgs.add(new ReferenceMessage(client, false, EntityDescriptor.createDescriptorForFetch(e.getValue(), EntityDescriptor.INVALID_VERSION, e.getKey().getClientInstanceID()), null))
    );
    return msgs;
  }
}
