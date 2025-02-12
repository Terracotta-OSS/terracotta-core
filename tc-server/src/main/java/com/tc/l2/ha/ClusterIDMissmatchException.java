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
package com.tc.l2.ha;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;

public class ClusterIDMissmatchException extends RuntimeException {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  public ClusterIDMissmatchException(String myid, String otherid) {
    super(wrapper.wrap("The cluster IDs of the servers dont match." + "\n\nThis server's cluster ID   : " + myid
                       + "\nActive server's cluster ID : " + otherid
                       + "\n\nThis could happen when two servers from different clusters are cross-talking"
                       + "\nto each other (due to a setup error) or when the passive is restarted before"
                       + "\nthe active server after a complete cluster shutdown (an user error)."));

  }

  public static void main(String arg[]) {
    try {
      throw new ClusterIDMissmatchException("f96ce271e0db44e19f5e1bd834706c08", "fe2b3d2126ed4d5e87d1f9f058d1699a");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
