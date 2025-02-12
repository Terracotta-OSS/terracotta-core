/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

/**
 * Object representing a specific instance of an entity on the server
 */
public class FetchID extends AbstractIdentifier {

  /**
   * The NULL ObjectID
   */
  public final static FetchID NULL_ID = new FetchID();

  // Only the last 7 bytes are used for object id, the 1st byte represent group id.
  // This still holds about 72 trillion object (72057594037927935) and 255 groups
  public final static long     MAX_ID  = 0x00FFFFFFFFFFFFFFL;

  /**
   * Create a FetchID with the specified ID
   * 
   * @param id The id value, >= 0
   */
  public FetchID(long id) {
    super(id);
  }

  /**
   * Create a "null" ObjectID.
   */
  private FetchID() {
    super();
  }

  @Override
  public String getIdentifierType() {
    return "FetchID";
  }
}