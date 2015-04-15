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
package com.tc.logging;

public class ConnectionIdLogger implements TCLogger {

  private final ConnectionIDProvider cidp;
  private final TCLogger             logger;

  public ConnectionIdLogger(ConnectionIDProvider connectionIDProvider, TCLogger logger) {
    this.cidp = connectionIDProvider;
    this.logger = logger;
  }

  @Override
  public void debug(Object message) {
    logger.debug(msg(message));
  }

  @Override
  public void debug(Object message, Throwable t) {
    logger.debug(msg(message), t);
  }

  @Override
  public void error(Object message) {
    logger.error(msg(message));
  }

  @Override
  public void error(Object message, Throwable t) {
    logger.error(msg(message), t);
  }

  @Override
  public void fatal(Object message) {
    logger.fatal(msg(message));
  }

  @Override
  public void fatal(Object message, Throwable t) {
    logger.fatal(msg(message), t);
  }

  @Override
  public void info(Object message) {
    logger.info(msg(message));
  }

  @Override
  public void info(Object message, Throwable t) {
    logger.info(msg(message), t);
  }

  @Override
  public void warn(Object message) {
    logger.warn(msg(message));
  }

  @Override
  public void warn(Object message, Throwable t) {
    logger.warn(msg(message), t);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void setLevel(LogLevel level) {
    logger.setLevel(level);
  }

  @Override
  public LogLevel getLevel() {
    return logger.getLevel();
  }

  private Object msg(Object msg) {
    return cidp.getConnectionId() + ": " + msg;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

}
