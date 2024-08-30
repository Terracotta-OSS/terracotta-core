/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
