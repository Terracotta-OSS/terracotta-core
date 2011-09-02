/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.managedobject;

/**
 * This is a marker interface added to the generated physical state objects to indicate that a class contains storage
 * (ie. has a field added) to store a parentID reference (ie. non-static inner class). Only one type in the hierarchy
 * needs to have this storage (no matter how many derived subclasses get generated due to class evolution)
 */
public interface HasParentIdStorage {
  // 
}
