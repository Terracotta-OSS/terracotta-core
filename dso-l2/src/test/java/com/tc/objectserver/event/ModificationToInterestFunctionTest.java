/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.tc.object.ObjectID;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugene Shelestovich
 */
public class ModificationToInterestFunctionTest {

  @Test
  public void testShouldTransformModificationToTypedInterests() {
    final String cacheName = "test-cache";
    final List<Modification> modifications = new ArrayList<Modification>(3);
    modifications.add(new Modification(ModificationType.PUT, 1, new ObjectID(101), new byte[] { 11 }, cacheName));
    modifications.add(new Modification(ModificationType.REMOVE, 2, new ObjectID(102), cacheName));
    modifications.add(new Modification(ModificationType.EVICT, 3, new ObjectID(103), cacheName));

    final List<Interest> interests = Lists.transform(modifications, ModificationToInterest.FUNCTION);
    assertEquals(3, interests.size());
    assertTrue(interests.get(0) instanceof PutInterest);
    assertEquals(1, interests.get(0).getKey());
    assertEquals(11, ((PutInterest)interests.get(0)).getValue()[0]);

    assertTrue(interests.get(1) instanceof RemoveInterest);
    assertEquals(2, interests.get(1).getKey());

    assertTrue(interests.get(2) instanceof EvictionInterest);
    assertEquals(3, interests.get(2).getKey());
  }
}
