/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.idprovider.api;

import com.tc.object.ObjectID;

/**
 * @author steve responsible for generating the next unique objectID accross processes in an efficient manner
 */
public interface ObjectIDProvider {
  public ObjectID next();
}