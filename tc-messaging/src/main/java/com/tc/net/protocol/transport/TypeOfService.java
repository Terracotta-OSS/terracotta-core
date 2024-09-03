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
package com.tc.net.protocol.transport;

/**
 * A place to keep the various TC Wire protocol type-of-service (TOS) definitions
 * 
 * @author teck
 */
public class TypeOfService {
  public static final byte          TOS_UNSPECIFIED = 0;
  public static final TypeOfService DEFAULT_TOS     = TypeOfService.getInstance(TOS_UNSPECIFIED);

  private final byte                value;

  // TODO: provide methods for testing / setting specific TOS bits

  public boolean isUnspecified() {
    return (0 == value);
  }

  private TypeOfService(byte b) {
    value = b;
  }

  public static TypeOfService getInstance(byte b) {
    // could cache instances here if need be
    return new TypeOfService(b);
  }

  byte getByteValue() {
    return value;
  }
}
