/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.type;

import org.terracotta.toolkit.object.Destroyable;

import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.rejoin.RejoinAwareToolkitObject;

/**
 * An aggregate toolkit type - aggregating clustered objects in an unclustered object wrapper, itself. The object
 * instance itself cannot be clustered.
 */
public interface DistributedToolkitType<T extends TCToolkitObject> extends RejoinAwareToolkitObject, Destroyable,
    Iterable<T> {
  //
}
