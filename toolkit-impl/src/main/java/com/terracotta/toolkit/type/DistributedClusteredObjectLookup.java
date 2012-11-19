/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.type;

import com.terracotta.toolkit.object.TCToolkitObject;
import com.terracotta.toolkit.object.ToolkitObjectStripe;

public interface DistributedClusteredObjectLookup<S extends TCToolkitObject> {

  ToolkitObjectStripe<S>[] lookupStripeObjects(String name);

}
