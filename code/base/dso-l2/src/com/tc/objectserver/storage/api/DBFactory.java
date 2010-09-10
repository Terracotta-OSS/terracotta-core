/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.api;

import com.tc.stats.counter.sampled.SampledCounter;

import java.io.File;
import java.io.IOException;

/**
 * This class is responsible for creating db and other classes specific to a particular db.
 */
public interface DBFactory {

  DBEnvironment createEnvironment(boolean paranoid, File envHome, SampledCounter l2FaultFromDisk) throws IOException;

}
