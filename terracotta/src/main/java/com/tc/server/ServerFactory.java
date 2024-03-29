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
