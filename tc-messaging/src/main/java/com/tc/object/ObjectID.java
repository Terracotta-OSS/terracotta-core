/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import com.tc.util.AbstractIdentifier;

/**
 * Object representing the ID of any managed object
 */
public class ObjectID extends AbstractIdentifier {

  /**
   * The NULL ObjectID
   */
  public final static ObjectID NULL_ID = new ObjectID();

  // Only the last 7 bytes are used for object id, the 1st byte represent group id.
  // This still holds about 72 trillion object (72057594037927935) and 255 groups
  public final static long     MAX_ID  = 0x00FFFFFFFFFFFFFFL;

  /**
   * Create an ObjectID with the specified ID
   * 
   * @param id The id value, >= 0
   */
  public ObjectID(long id) {
    super(id);
  }

  /**
   * Create a "null" ObjectID.
   */
  private ObjectID() {
    super();
  }

  public ObjectID(long oid, int gid) {
    super(getObjectID(oid, gid));
  }

  private static long getObjectID(long oid, int gid) {
    if (gid < 0 || gid > 254 || oid < 0 || oid > MAX_ID) { throw new AssertionError(
                                                                                    "Currently only supports upto 255 Groups and "
                                                                                        + MAX_ID + " objects : " + gid
                                                                                        + "," + oid); }
    long gidLong = (long) gid & 0xFF;
    gidLong = gidLong << 56;
    oid = gidLong | oid;
    return oid;
  }

  @Override
  public String getIdentifierType() {
    return "ObjectID";
  }

  public int getGroupID() {
    if (isNull()) { return 0; } // Null ObjectIDs are mapped to Group 0
    long oid = toLong();
    long gid = oid & 0xFF00000000000000L;
    gid = gid >>> 56;
    if ((gid < 0 || gid > 254)) { throw new AssertionError("Group ID is not between 0 and 254, the value was = " + gid
                                                           + " id = " + toLong()); }
    return (int) gid;
  }

  public long getMaskedObjectID() {
    if (isNull()) { throw new AssertionError("Can't call getMaskedObjectID() on NULL ID"); }
    long oid = toLong() & 0x00FFFFFFFFFFFFFFL;
    if ((oid > MAX_ID || oid < 0)) { throw new AssertionError("Oid is not between 0 and " + MAX_ID
                                                              + ", the value was = " + oid + " id = " + toLong()); }
    return oid;
  }

  @Override
  public String toString() {
    if (toLong() == -1 || getGroupID() == 0) { return super.toString(); }
    return getIdentifierType() + "=" + "[" + getGroupID() + ":" + getMaskedObjectID() + "]";
  }

}