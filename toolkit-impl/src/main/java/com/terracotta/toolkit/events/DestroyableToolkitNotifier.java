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
package com.terracotta.toolkit.events;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.events.ToolkitNotificationEvent;
import org.terracotta.toolkit.events.ToolkitNotificationListener;
import org.terracotta.toolkit.events.ToolkitNotifier;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.type.IsolatedClusteredObjectLookup;
import com.terracotta.toolkit.util.ToolkitInstanceProxy;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DestroyableToolkitNotifier<T> extends AbstractDestroyableToolkitObject<ToolkitNotifier> implements
    ToolkitNotifier<T>, ToolkitNotificationListener<T>, RejoinAwareToolkitObject {

  private volatile ToolkitNotifier<T>                                notifier;
  private final String                                               name;
  private final CopyOnWriteArrayList<ToolkitNotificationListener<T>> listeners  = new CopyOnWriteArrayList<ToolkitNotificationListener<T>>();
  private boolean                                                    registered = false;
  private final IsolatedClusteredObjectLookup<ToolkitNotifierImpl>   lookup;

  public DestroyableToolkitNotifier(ToolkitObjectFactory factory,
                                    IsolatedClusteredObjectLookup<ToolkitNotifierImpl> lookup,
                                    ToolkitNotifierImpl<T> notifierImpl, String name, PlatformService platformService) {
    super(factory, platformService);
    this.lookup = lookup;
    this.name = name;
    this.notifier = notifierImpl;
    notifierImpl.setApplyDestroyCallback(getDestroyApplicator());
  }

  @Override
  public void doRejoinStarted() {
    this.notifier = ToolkitInstanceProxy.newRejoinInProgressProxy(name, ToolkitNotifier.class);
  }

  @Override
  public void doRejoinCompleted() {
    if (!isDestroyed()) {
      ToolkitNotifierImpl afterRejoin = lookup.lookupClusteredObject(name, ToolkitObjectType.NOTIFIER, null);
      if (afterRejoin == null) {
        destroyApplicator.applyDestroy();
      } else {
        this.notifier = afterRejoin;
        if (listeners.size() > 0) {
          notifier.addNotificationListener(this);
        }
      }
    }
  }

  @Override
  public void applyDestroy() {
    // status.setDestroyed() is called from Parent class
    this.notifier = ToolkitInstanceProxy.newDestroyedInstanceProxy(name, ToolkitNotifier.class);
  }

  @Override
  public void doDestroy() {
    notifier.destroy();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public synchronized void addNotificationListener(ToolkitNotificationListener<T> listener) {
    if (listeners.addIfAbsent(listener)) {
      if (!registered && listeners.size() > 0) {
        notifier.addNotificationListener(this);
        registered = true;
      }
    }
  }

  @Override
  public synchronized void removeNotificationListener(ToolkitNotificationListener<T> listener) {
    if (listeners.remove(listener)) {
      if (registered && listeners.size() == 0) {
        notifier.removeNotificationListener(this);
        registered = false;
      }
    }
  }

  @Override
  public void notifyListeners(T msg) {
    notifier.notifyListeners(msg);
  }

  @Override
  public List<ToolkitNotificationListener<T>> getNotificationListeners() {
    return Collections.unmodifiableList(listeners);
  }

  @Override
  public void onNotification(ToolkitNotificationEvent<T> event) {
    for (ToolkitNotificationListener<T> listener : listeners) {
      listener.onNotification(event);
    }
  }
}
