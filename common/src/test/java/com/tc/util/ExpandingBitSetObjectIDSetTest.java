package com.tc.util;

import com.tc.object.ObjectID;

import java.util.Collection;

public class ExpandingBitSetObjectIDSetTest extends ObjectIDSetTestBase {
  @Override
  protected ObjectIDSet create() {
    return new ExpandingBitSetObjectIDSet();
  }

  @Override
  protected ObjectIDSet create(Collection<ObjectID> copy) {
    return new ExpandingBitSetObjectIDSet(copy);
  }
}