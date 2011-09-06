/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import java.io.IOException;

public interface SerializationAdapterFactory {

  public SerializationAdapter newAdapter()
      throws IOException;

}