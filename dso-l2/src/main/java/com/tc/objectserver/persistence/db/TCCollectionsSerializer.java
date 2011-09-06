/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.db;

import java.io.IOException;

public interface TCCollectionsSerializer {

  public byte[] serialize(final long id, final Object o) throws IOException;

  public byte[] serialize(final Object o) throws IOException;

  public Object deserialize(final byte[] data) throws IOException, ClassNotFoundException;

  public Object deserialize(final int start, final byte[] data) throws IOException, ClassNotFoundException;
}
