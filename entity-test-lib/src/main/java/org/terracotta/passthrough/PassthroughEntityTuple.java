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
 *  The Covered Software is Entity API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.passthrough;


/**
 * Contains the data required to uniquely identify an entity ref (either an entity or where an entity can be created).
 * Contains the class name and entity name.
 * The data members are public and final since this is meant to be used as an immutable struct.
 */
public class PassthroughEntityTuple {
  public final String entityClassName;
  public final String entityName;

  public PassthroughEntityTuple(String entityClassName, String entityName) {
    this.entityClassName = entityClassName;
    this.entityName = entityName;
  }

  @Override
  public boolean equals(Object obj) {
    boolean isEqual = (obj == this);
    if (!isEqual && (obj instanceof PassthroughEntityTuple)) {
      PassthroughEntityTuple other = (PassthroughEntityTuple)obj;
      isEqual = this.entityClassName.equals(other.entityClassName)
          && this.entityName.equals(other.entityName);
    }
    return isEqual;
  }

  @Override
  public int hashCode() {
    return this.entityClassName.hashCode() ^ this.entityName.hashCode();
  }
}
