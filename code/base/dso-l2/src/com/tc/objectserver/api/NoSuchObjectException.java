/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.object.ObjectID;

import java.io.Serializable;

public class NoSuchObjectException extends Exception implements Serializable {

  public NoSuchObjectException(ObjectID id) {
    super(id + " does not exist");
  }

}
