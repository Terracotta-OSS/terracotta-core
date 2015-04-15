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

import org.terracotta.toolkit.config.Configuration;

import com.tc.object.LogicalOperation;
import com.tc.object.TCObject;
import com.terracotta.toolkit.config.ConfigChangeListener;
import com.terracotta.toolkit.config.UnclusteredConfiguration;
import com.terracotta.toolkit.config.cache.InternalCacheConfigurationType;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolkitObjectStripeImpl<C extends TCToolkitObject> implements ToolkitObjectStripe<C> {

  private final TCToolkitObject[]                          components;

  // unclustered fields
  private transient volatile TCObject                      tcObject;

  private transient volatile UnclusteredConfiguration      unclusteredConfig;

  private volatile boolean                                 destroyed = false;
  private final CopyOnWriteArrayList<ConfigChangeListener> listeners = new CopyOnWriteArrayList<ConfigChangeListener>();

  public ToolkitObjectStripeImpl(Configuration config, C[] components) {
    unclusteredConfig = new UnclusteredConfiguration();
    this.components = new TCToolkitObject[components.length];
    System.arraycopy(components, 0, this.components, 0, components.length);

    for (String key : config.getKeys()) {
      this.unclusteredConfig.setObject(key, config.getObjectOrNull(key));
    }
  }

  @Override
  public void __tc_managed(TCObject t) {
    this.tcObject = t;
  }

  @Override
  public TCObject __tc_managed() {
    return tcObject;
  }

  @Override
  public boolean __tc_isManaged() {
    return tcObject != null;
  }

  protected TCToolkitObject[] internalGetComponents() {
    return components;
  }

  @Override
  public void setConfigField(String key, Serializable value) {
    synchronized (tcObject.getResolveLock()) {
      InternalCacheConfigurationType type = InternalCacheConfigurationType.getTypeFromConfigString(key);

      if (type == null || type.isDynamicLocalChangeAllowed()) {
        unclusteredConfig.setObject(key, value);
      }

      if (type == null || type.isDynamicChangeAllowed()) {
        logicalInvokePut(key, value);
      }
    }
  }

  @Override
  public void addConfigChangeListener(ConfigChangeListener listener) {
    listeners.add(listener);
  }

  protected void internalSetMapping(String key, Serializable value) {
    InternalCacheConfigurationType type = InternalCacheConfigurationType.getTypeFromConfigString(key);
    if (type != null && !type.isDynamicClusterWideChangeAllowed()) { return; }

    unclusteredConfig.setObject(key, value);
    for (ConfigChangeListener listener : listeners) {
      listener.configChanged(key, value);
    }
  }

  private void logicalInvokePut(String key, Object value) {
    tcObject.logicalInvoke(LogicalOperation.PUT, new Object[] { key, value });
  }

  @Override
  public void cleanupOnDestroy() {
    //
  }

  @Override
  public void destroy() {
    destroyed = true;
  }

  @Override
  public boolean isDestroyed() {
    return destroyed;
  }

  @Override
  public void setApplyDestroyCallback(DestroyApplicator callback) {
    // TODO: fix this
  }

  @Override
  public void applyDestroy() {
    // TODO: fix this
  }

  @Override
  public String toString() {
    return "ClusteredObjectStripe@" + this.unclusteredConfig;
  }

  @Override
  public Configuration getConfiguration() {
    return unclusteredConfig;
  }

  @Override
  public Iterator<C> iterator() {
    return new ToolkitObjectStripeIterator(this.components);
  }

  private static class ToolkitObjectStripeIterator<C extends TCToolkitObject> implements Iterator<C> {
    private final TCToolkitObject[] components;
    private int                     index = 0;

    public ToolkitObjectStripeIterator(TCToolkitObject[] components) {
      this.components = components;
    }

    @Override
    public boolean hasNext() {
      return index < components.length;
    }

    @Override
    public C next() {
      if (!hasNext()) { throw new NoSuchElementException(); }
      index++;
      return (C) components[index - 1];
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }
}
