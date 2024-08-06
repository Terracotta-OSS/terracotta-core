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
package com.tc.client;

import com.tc.lang.TCThreadGroup;
import com.tc.object.ClientBuilder;
import com.tc.object.DistributedObjectClient;

import java.net.InetSocketAddress;


public class ClientFactory {
  // Note that we don't currently use classProvider in this path but it is left here as a remnant from the old shape until
  //  we can verify that it won't be used here.
  public static DistributedObjectClient createClient(Iterable<InetSocketAddress> serverAddresses, ClientBuilder builder,
                                                     TCThreadGroup threadGroup,
                                                     String uuid, String name) {
    return new DistributedObjectClient(serverAddresses, builder, threadGroup, uuid, name);
  }
}
