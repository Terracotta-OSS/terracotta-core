/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.object;

import com.tc.net.GroupID;
import com.tc.object.LiteralValues;
import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.tc.platform.PlatformService;
import com.terracotta.toolkit.TerracottaToolkit;
import com.terracotta.toolkit.object.serialization.SerializationStrategy;
import com.terracotta.toolkit.object.serialization.SerializedClusterObject;
import com.terracotta.toolkit.object.serialization.SerializedClusterObjectFactory;
import com.terracotta.toolkit.object.serialization.SerializedClusterObjectFactoryImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractTCToolkitObject implements TCToolkitObject {

  protected final SerializedClusterObjectFactory serializedClusterObjectFactory;
  protected final SerializationStrategy          serStrategy;

  protected volatile GroupID                     gid;
  protected volatile TCObject                    tcObject;
  private final Collection<DestroyApplicator>          applyDestroyCallbacks;
  private volatile boolean                       destroyed = false;
  protected final PlatformService                platformService;

  public AbstractTCToolkitObject(PlatformService platformService) {
    this.platformService = platformService;
    SerializationStrategy registeredSerializer = platformService
        .lookupRegisteredObjectByName(TerracottaToolkit.TOOLKIT_SERIALIZER_REGISTRATION_NAME,
                                      SerializationStrategy.class);
    if (registeredSerializer == null) {
      //
      throw new AssertionError("No SerializationStrategy registered in L1");
    }
    this.serStrategy = registeredSerializer;
    this.serializedClusterObjectFactory = new SerializedClusterObjectFactoryImpl(platformService, serStrategy);
    this.applyDestroyCallbacks = new CopyOnWriteArrayList<DestroyApplicator>();
  }

  @Override
  public void __tc_managed(TCObject t) {
    this.tcObject = t;
    this.gid = new GroupID(t.getObjectID().getGroupID());
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  protected void doLogicalDestroy() {
    platformService.logicalInvoke(this, LogicalOperation.DESTROY, new Object[] {});
  }

  @Override
  public final void destroy() {
    doLogicalDestroy();
    applyDestroy();
  }

  @Override
  public final void applyDestroy() {
    destroyed = true;
    cleanupOnDestroy();
    for (DestroyApplicator applyDestroyCallback : applyDestroyCallbacks) {
      applyDestroyCallback.applyDestroy();
    }
    applyDestroyCallbacks.clear();
  }

  @Override
  public void setApplyDestroyCallback(DestroyApplicator callback) {
    if (callback != null) {
      applyDestroyCallbacks.add(callback);
    } else {
      throw new AssertionError("DestroyApplicator callback is null");
    }
  }

  @Override
  public final boolean isDestroyed() {
    return destroyed;
  }

  protected Object createTCCompatibleObject(Object e) {
    boolean isLiteral = LiteralValues.isLiteralInstance(e);
    if (isLiteral) { return e; }

    return createSerializedClusterObject(e);
  }

  protected Collection createTCCompatiableCollection(Collection c) {
    Collection serCollection = new ArrayList(c.size());
    for (Object e : c) {
      serCollection.add(createTCCompatibleObject(e));
    }
    return serCollection;
  }

  private SerializedClusterObject createSerializedClusterObject(Object obj) {
    return serializedClusterObjectFactory.createSerializedClusterObject(obj, gid);
  }

}
