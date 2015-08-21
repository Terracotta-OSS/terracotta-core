/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.api;

/**
 * EventContexts should implement this interface if they want to give some order to the processing based on some key. As
 * long as keys are the same the will be processed in the order in which they arrive. Between different contexts with
 * different keys, the order is not guaranteed.
 */
public interface MultiThreadedEventContext extends EventContext {

  Object getKey();

}
