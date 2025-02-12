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
package com.tc.util.sequence;

public class SequenceBatch {
  private long next;
  private long end;

  public SequenceBatch(long next, long end) {
    this.next = next;
    this.end = end;
  }

  public boolean hasNext() {
    return next < end;
  }

  public long next() {
    return next++;
  }

  public long current() {
    return next - 1;
  }
  
  public long end() {
    return end;
  }

  @Override
  public String toString() {
    return "SequenceBatch@" + System.identityHashCode(this) + "[ next = " + next + " , end = " + end + " ]";
  }
}
