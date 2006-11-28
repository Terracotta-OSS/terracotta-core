/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.idprovider.api;

import com.tc.object.ObjectID;

/**
 * @author steve responsible for generating the next unique objectID accross processes in an efficient manner
 */
public interface ObjectIDProvider {
  public ObjectID next();
}