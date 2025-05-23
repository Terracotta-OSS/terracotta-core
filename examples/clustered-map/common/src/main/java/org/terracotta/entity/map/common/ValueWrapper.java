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
package org.terracotta.entity.map.common;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serializable;
import java.util.Arrays;

/**
 * ValueWrapper
 */
public class ValueWrapper implements Serializable {

  private static final long serialVersionUID = -4794738044295644587L;

  private final int hashCode;
  private final byte[] value;

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public ValueWrapper(int hashCode, byte[] value) {
    this.hashCode = hashCode;
    this.value = value;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ValueWrapper that = (ValueWrapper) o;

    if (hashCode != that.hashCode) return false;
    return Arrays.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }
}
