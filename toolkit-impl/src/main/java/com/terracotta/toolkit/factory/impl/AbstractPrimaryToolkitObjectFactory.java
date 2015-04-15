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
package com.terracotta.toolkit.factory.impl;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.factory.ToolkitObjectFactory;
import com.terracotta.toolkit.object.AbstractDestroyableToolkitObject;
import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;
import com.terracotta.toolkit.roots.AggregateToolkitTypeRoot;

public abstract class AbstractPrimaryToolkitObjectFactory<T extends RejoinAwareToolkitObject, S extends TCToolkitObject>
    implements ToolkitObjectFactory<T> {

  protected final AggregateToolkitTypeRoot<T, S> aggregateRootMap;
  protected final ToolkitInternal                toolkit;

  public AbstractPrimaryToolkitObjectFactory(ToolkitInternal toolkit, AggregateToolkitTypeRoot<T, S> aggregateRootMap) {
    this.aggregateRootMap = aggregateRootMap;
    this.toolkit = toolkit;
  }

  @Override
  public final T getOrCreate(String name, Configuration config) {
    return aggregateRootMap.getOrCreateToolkitType(toolkit, this, name, config);
  }

  /**
   * {@inheritDoc}
   */
  public final void destroy(AbstractDestroyableToolkitObject obj) {
    final ToolkitObjectType type = getManufacturedToolkitObjectType();
    aggregateRootMap.destroy(obj, type);
  }

  public final void applyDestroy(ToolkitObject obj) {
    aggregateRootMap.applyDestroy(obj.getName());
  }

  public final void dispose(ToolkitObject obj) {
    aggregateRootMap.dispose(getManufacturedToolkitObjectType(), obj.getName());
  }
}