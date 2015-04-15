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
package com.terracotta.toolkit.nonstop;

import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.collections.ToolkitSet;
import org.terracotta.toolkit.collections.ToolkitSortedMap;
import org.terracotta.toolkit.collections.ToolkitSortedSet;
import org.terracotta.toolkit.concurrent.atomic.ToolkitAtomicLong;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.nonstop.NonStopConfiguration;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopReadTimeoutBehavior;
import org.terracotta.toolkit.nonstop.NonStopConfigurationFields.NonStopWriteTimeoutBehavior;
import org.terracotta.toolkit.object.ToolkitObject;

import com.terracotta.toolkit.collections.map.LocalReadsToolkitCacheImpl;
import com.terracotta.toolkit.collections.map.TimeoutBehaviorToolkitCacheImpl;
import com.terracotta.toolkit.collections.map.ToolkitCacheImplInterface;

public class NonstopTimeoutBehaviorResolver {

  private final ExceptionOnTimeoutBehaviorResolver exceptionOnTimeoutBehaviorResolver = new ExceptionOnTimeoutBehaviorResolver();
  private final NoOpBehaviorResolver               noOpBehaviorResolver               = new NoOpBehaviorResolver();

  public <E extends ToolkitObject> E resolveTimeoutBehavior(ToolkitObjectType objectType,
                                                            NonStopConfiguration nonStopConfiguration,
                                                            ToolkitObjectLookup<E> toolkitObjectLookup) {
    if (nonStopConfiguration.getReadOpNonStopTimeoutBehavior() == NonStopReadTimeoutBehavior.EXCEPTION
        && nonStopConfiguration.getWriteOpNonStopTimeoutBehavior() == NonStopWriteTimeoutBehavior.EXCEPTION) {
      return (E) resolveExceptionOnTimeoutBehavior(objectType);
    } else if (nonStopConfiguration.getReadOpNonStopTimeoutBehavior() == NonStopReadTimeoutBehavior.NO_OP
               && nonStopConfiguration.getWriteOpNonStopTimeoutBehavior() == NonStopWriteTimeoutBehavior.NO_OP) {
      return (E) resolveNoOpTimeoutBehavior(objectType);
    } else {
      // create separate behaviors for mutable and immutable ops.
      return resolveReadWriteTimeoutBehavior(objectType, nonStopConfiguration, toolkitObjectLookup);
    }
  }

  private <E extends ToolkitObject> E resolveReadWriteTimeoutBehavior(ToolkitObjectType objectType,
                                                                      NonStopConfiguration nonStopConfiguration,
                                                                      ToolkitObjectLookup<E> toolkitObjectLookup) {
    if (objectType != ToolkitObjectType.CACHE && objectType != ToolkitObjectType.STORE) { throw new UnsupportedOperationException(
                                                                                                                                  "Read TimeoutBehavior "
                                                                                                                                      + nonStopConfiguration
                                                                                                                                          .getReadOpNonStopTimeoutBehavior()
                                                                                                                                      + " Write Timeout Behavior "
                                                                                                                                      + nonStopConfiguration
                                                                                                                                          .getWriteOpNonStopTimeoutBehavior()
                                                                                                                                      + " not supported for object type : "
                                                                                                                                      + objectType); }
    E readTimeoutBehavior = resolveReadTimeoutBehavior(objectType,
                                                       nonStopConfiguration.getReadOpNonStopTimeoutBehavior(),
                                                       toolkitObjectLookup);
    E writeTimeoutBehavior = resolveWriteTimeoutBehavior(objectType,
                                                         nonStopConfiguration.getWriteOpNonStopTimeoutBehavior(),
                                                         toolkitObjectLookup);
    return (E) new TimeoutBehaviorToolkitCacheImpl((ToolkitCacheImplInterface) readTimeoutBehavior,
                                                   (ToolkitCacheImplInterface) writeTimeoutBehavior);

  }

  private <E extends ToolkitObject> E resolveWriteTimeoutBehavior(ToolkitObjectType objectType,
                                                                  NonStopWriteTimeoutBehavior behavior,
                                                                  ToolkitObjectLookup<E> toolkitObjectLookup) {
    switch (behavior) {
      case EXCEPTION:
        return (E) resolveExceptionOnTimeoutBehavior(objectType);
      case NO_OP:
        return (E) resolveNoOpTimeoutBehavior(objectType);
    }
    throw new UnsupportedOperationException();

  }

  private <E extends ToolkitObject> E resolveReadTimeoutBehavior(ToolkitObjectType objectType,
                                                                 NonStopReadTimeoutBehavior behavior,
                                                                 ToolkitObjectLookup<E> toolkitObjectLookup) {
    switch (behavior) {
      case EXCEPTION:
        return (E) resolveExceptionOnTimeoutBehavior(objectType);
      case LOCAL_READS:
        return resolveLocalReadTimeoutBehavior(objectType, toolkitObjectLookup);
      case NO_OP:
        return (E) resolveNoOpTimeoutBehavior(objectType);
    }
    throw new UnsupportedOperationException();

  }

  private <E extends ToolkitObject> E resolveLocalReadTimeoutBehavior(ToolkitObjectType objectType,
                                                                      ToolkitObjectLookup<E> toolkitObjectLookup) {
    switch (objectType) {
      case CACHE:
      case STORE:
        return (E) new LocalReadsToolkitCacheImpl(toolkitObjectLookup,
                                                  (ToolkitCacheImplInterface) resolveNoOpTimeoutBehavior(objectType));
      case ATOMIC_LONG:
      case BARRIER:
      case BLOCKING_QUEUE:
      case LIST:
      case LOCK:
      case MAP:
      case NOTIFIER:
      case READ_WRITE_LOCK:
      case SET:
      case SORTED_MAP:
      case SORTED_SET:
        throw new UnsupportedOperationException("NonStop local reads Not Supported for ToolkitType " + objectType);
    }
    return null;

  }

  public Object resolveNoOpTimeoutBehavior(ToolkitObjectType objectType) {
    switch (objectType) {
      case STORE:
      case CACHE:
        return noOpBehaviorResolver.resolve(ToolkitCacheImplInterface.class);
      case MAP:
      case SET:
      case SORTED_MAP:
      case SORTED_SET:
      case LIST:
      case ATOMIC_LONG:
      case LOCK:
      case READ_WRITE_LOCK:
      case BARRIER:
      case BLOCKING_QUEUE:
      case NOTIFIER:
        throw new UnsupportedOperationException("No op not supported for type " + objectType);
    }
    throw new UnsupportedOperationException();

  }

  Object resolveExceptionOnTimeoutBehavior(ToolkitObjectType objectType) {
    switch (objectType) {
      case ATOMIC_LONG:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitAtomicLong.class);
      case STORE:
      case CACHE:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitCacheImplInterface.class);
      case LIST:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitListInternal.class);
      case LOCK:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitLock.class);
      case MAP:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitMap.class);
      case READ_WRITE_LOCK:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitReadWriteLock.class);
      case SET:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitSet.class);
      case SORTED_MAP:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitSortedMap.class);
      case SORTED_SET:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitSortedSet.class);
      case NOTIFIER:
        return exceptionOnTimeoutBehaviorResolver.resolve(ToolkitNotifier.class);
      case BARRIER:
      case BLOCKING_QUEUE:
        throw new UnsupportedOperationException("Exception on Timeout not supported for type " + objectType);
    }
    throw new UnsupportedOperationException();

  }

  public <E> E resolveTimeoutBehaviorForSubType(ToolkitObjectType parentObjectType,
                                                NonStopConfiguration nonStopConfiguration, Class<E> klazz) {

    // write behavior will be applicable for subtypes. If user has set localRead it will default to no Op for toolkit
    // subtypes
    NonStopWriteTimeoutBehavior writeOpBehavior = nonStopConfiguration.getWriteOpNonStopTimeoutBehavior();
    switch (writeOpBehavior) {
      case EXCEPTION:
        return exceptionOnTimeoutBehaviorResolver.resolve(klazz);
      case NO_OP:
        return noOpBehaviorResolver.resolve(klazz);
    }
    throw new UnsupportedOperationException();
  }

}
