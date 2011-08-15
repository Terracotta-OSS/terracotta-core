/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.invalidation;


public interface InvalidationsProcessor {

  void processInvalidations(Invalidations invalidations);

}
