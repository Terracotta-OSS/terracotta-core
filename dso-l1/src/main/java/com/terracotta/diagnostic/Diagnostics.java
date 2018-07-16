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
package com.terracotta.diagnostic;

import org.terracotta.connection.entity.Entity;


public interface Diagnostics extends Entity {
  String getState();
  
  String getClusterState();  
  
  String getConfig();

  String getProcessArguments();

  String getThreadDump();

  String terminateServer();

  String forceTerminateServer();  
  
  String get(String name, String attribute);
 
  String set(String name, String attribute, String arg);
  
  String invoke(String name, String cmd);

  String invokeWithArg(String name, String cmd, String arg);

}
