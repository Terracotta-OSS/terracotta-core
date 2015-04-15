/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
