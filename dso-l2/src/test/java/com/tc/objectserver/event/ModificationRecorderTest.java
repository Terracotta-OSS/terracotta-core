/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.interest;

import org.junit.Test;

import com.tc.object.ObjectID;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Eugene Shelestovich
 */
public class ModificationRecorderTest {

  @Test
  public void testShouldResolveKeyToValuePairsOnGet() {
    final String cacheName = "test-cache";
    final ModificationRecorder recorder = new ModificationRecorder();
    final ObjectID objectId1 = new ObjectID(1001);
    recorder.recordOperation(ModificationType.PUT, 1, objectId1, cacheName);
    final ObjectID objectId2 = new ObjectID(1002);
    recorder.recordOperation(ModificationType.REMOVE, 1, objectId2, cacheName);
    recorder.recordOperationValue(objectId2, new byte[] { 12 });
    recorder.recordOperationValue(objectId1, new byte[] { 11 });

    final ObjectID objectId3 = new ObjectID(1003);
    recorder.recordOperation(ModificationType.PUT, 2, objectId3, cacheName);
    recorder.recordOperationValue(objectId3, new byte[] { 13 });

    final List<Modification> modifications = recorder.getModifications();
    assertEquals(3, modifications.size());

    assertEquals(ModificationType.PUT, modifications.get(0).getType());
    assertEquals(11, modifications.get(0).getValue()[0]);

    assertEquals(ModificationType.REMOVE, modifications.get(1).getType());
    assertEquals(12, modifications.get(1).getValue()[0]);

    assertEquals(ModificationType.PUT, modifications.get(2).getType());
    assertEquals(13, modifications.get(2).getValue()[0]);
  }

}
