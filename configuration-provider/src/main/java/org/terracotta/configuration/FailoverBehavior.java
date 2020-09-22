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
package org.terracotta.configuration;

/**
 *
 */
public class FailoverBehavior {
  public enum Type {
    AVAILABILITY, CONSISTENCY;
  }
  
  private final Type type;
  private final int voters;

  public FailoverBehavior(Type type, int voters) {
    this.type = type;
    this.voters = voters;
  }
  
  public Type getBehaviorType() {
    return type;
  }
  
  public int getExternalVoters() {
    return voters;
  }
  
  public boolean isAvailability() {
    return type == Type.AVAILABILITY;
  }
  
  
  public boolean isConsistency() {
    return type == Type.CONSISTENCY;
  }  
}
