/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.core.api.ServerConfigurationContext;

import java.util.Iterator;
import java.util.Map;

public class L2ObjectSyncDehydrateHandler extends AbstractEventHandler {

  private Sink                    sendSink;
  private ObjectManager           objectManager;

  public void handleEvent(EventContext context) {
    ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
    Map moObjects = mosc.getObjects();
    ObjectStringSerializer serializer = new ObjectStringSerializer();
    TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    for (Iterator i = moObjects.values().iterator(); i.hasNext();) {
      ManagedObject m = (ManagedObject) i.next();
      m.toDNA(out, serializer);
      objectManager.releaseReadOnly(m);
    }
    mosc.setDehydratedBytes(out.toArray(), moObjects.size(), serializer);
    sendSink.add(mosc);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.objectManager = oscc.getObjectManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
  }
}
