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


import com.tc.entity.VoltronEntityMessage;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.EntityDescriptor;
import com.tc.object.EntityID;
import com.tc.object.tx.TransactionID;
import com.tc.util.Assert;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.terracotta.entity.EntityMessage;


public class ClientEntityStateManagerImpl implements ClientEntityStateManager {
  private final Map<ClientID, Set<EntityDescriptor>> clientStates = new ConcurrentHashMap<>();
  private final Set<ClientID> clientGC = new CopyOnWriteArraySet<>();
  private static final TCLogger logger    = TCLogging.getLogger(ClientEntityStateManagerImpl.class);

  public ClientEntityStateManagerImpl() {
  }

  @Override
  public boolean addReference(ClientID clientID, EntityDescriptor entityDescriptor) {
    Set<EntityDescriptor> led = clientStates.get(clientID);
    if (led == null) {
      led = new CopyOnWriteArraySet<>();
      Set<EntityDescriptor> check = clientStates.putIfAbsent(clientID, led);
      if (check != null) {
        led = check;
      }
    }
    Assert.assertNotNull(led);
    boolean didAdd = led.add(entityDescriptor);
    logger.debug("Adding reference:" + clientID + " " + entityDescriptor.getEntityID());
    // We currently assume that we are being used precisely:  all add/remove calls are expected to have a specific meaning.
    Assert.assertTrue(didAdd);
    return didAdd;
  }

  @Override
  public boolean removeReference(ClientID clientID, EntityDescriptor entityDescriptor) {
    Set<EntityDescriptor> refs = clientStates.get(clientID);
    logger.debug("Removing reference:" + clientID + " " + entityDescriptor.getEntityID());
    boolean didRemove = false;
    if (refs != null) {
      didRemove = refs.remove(entityDescriptor);
      // We currently assume that we are being used precisely:  all add/remove calls are expected to have a specific meaning.
      Assert.assertTrue(didRemove);
      if (refs.isEmpty() && this.clientGC.contains(clientID)) {
        this.clientGC.remove(clientID);
        this.clientStates.remove(clientID);
      }
    }
    return didRemove;
  }

  @Override
  public boolean verifyNoReferences(EntityID eid) {
    return !clientStates.values().stream().anyMatch((led)->led.stream().anyMatch(ed->ed.getEntityID().equals(eid)));
  }

  @Override
  public List<VoltronEntityMessage> clientDisconnected(ClientID client) {
    Set<EntityDescriptor> list = this.clientStates.get(client);
    if (list != null) {
      ArrayList<VoltronEntityMessage> msgs = new ArrayList<>(list.size());
      if (!list.isEmpty()) {
        this.clientGC.add(client);
        logger.debug("list has: " + client + " " + list);
      } else {
        this.clientStates.remove(client);
        logger.debug("list empty: removing " + client);
      }

      for (EntityDescriptor oneInstance : list) {
        msgs.add(new ReferenceMessage(client, false, oneInstance, null));
      }
      return msgs;
    }
    return Collections.emptyList();
  }
}
