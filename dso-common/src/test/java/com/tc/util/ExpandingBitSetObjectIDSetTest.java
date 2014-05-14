package com.tc.util;

import com.tc.object.ObjectID;

import java.util.Collection;

import junit.framework.TestCase;

public class ExpandingBitSetObjectIDSetTest extends ObjectIDSetTestBase {
  @Override
  protected ObjectIDSet create() {
    return new ExpandingBitSetObjectIDSet();
  }

  @Override
  protected ObjectIDSet create(final Collection<ObjectID> copy) {
    return new ExpandingBitSetObjectIDSet(copy);
  }
}