/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package org.terracotta.management.cli.rest;

import org.terracotta.management.cli.Command;

public class OssCommandManager implements RestCommandManager {

  @Override
  public Command<Context> getHelpCommand() {
    return new HelpCommand();
  }

  @Override
  public Command<Context> getPostCommand() {
    return new PostCommand();
  }

  @Override
  public Command<Context> getGetCommand() {
    return new GetCommand();
  }

  @Override
  public Command<Context> getUrlEncodingCommand() {
    return new SetupUrlEncodingCommand();
  }

  @Override
  public Command<Context> getUnsafeSSLCommand() {
    return null;
  }

}
