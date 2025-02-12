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
package com.tc.server;

import java.io.OutputStream;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Future;


public class ServerFactory {
  
  public static Future<Boolean> createServer(List<String> args, OutputStream console, ClassLoader loader) {
    try {
      ServiceLoader<BootstrapService> s = ServiceLoader.load(BootstrapService.class, loader);
      return s.iterator().next().createServer(args, console, loader);
    } catch (Error | RuntimeException notfound) {
      throw notfound;
    } catch (Exception notfound) {
      throw new RuntimeException(notfound);
    }
  }
}
