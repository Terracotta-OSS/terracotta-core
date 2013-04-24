/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * TODO: add versioning for each event
 *
 * @author Eugene Shelestovich
 */
public interface Interest {

  String getCacheName();

  InterestType getType();

  Object getKey();
}
