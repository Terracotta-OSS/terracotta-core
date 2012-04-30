/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public interface TCObjectSelf extends TCObject {

  public void initializeTCObject(final ObjectID id, final TCClass clazz, final boolean isNew);

  public void serialize(ObjectOutput out) throws IOException;

  public void deserialize(ObjectInput in) throws IOException;

  public void initClazzIfRequired(TCClass tcc);

  public boolean isInitialized();

}
