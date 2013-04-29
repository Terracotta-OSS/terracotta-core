/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import com.tc.object.InterestType;

/**
 * Server event.
 * TODO: add versioning for each event
 *
 * @author Eugene Shelestovich
 * @see InterestListener
 * @see TypedInterestListenerSupport
 */
public interface Interest {

  String getCacheName();

  InterestType getType();

  Object getKey();
}
