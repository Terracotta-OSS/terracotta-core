/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.testing.common;


/**
 * Just is just a list of names used by the server-client lock-step IPC mechanism.
 */
public class IPCMessageConstants {
  public static String synFrom(String message) {
    return message + "_SYN";
  }
  public static String ackFrom(String message) {
    return message + "_ACK";
  }

  public static final String SYNC = "SYNC";
  public static final String SYNC_SYN = synFrom(SYNC);
  public static final String SYNC_ACK = ackFrom(SYNC);

  public static final String TERMINATE_ACTIVE = "TERMINATE_ACTIVE";
  public static final String TERMINATE_ACTIVE_SYN = synFrom(TERMINATE_ACTIVE);
  public static final String TERMINATE_ACTIVE_ACK = ackFrom(TERMINATE_ACTIVE);

  public static final String TERMINATE_ONE_PASSIVE = "TERMINATE_ONE_PASSIVE";
  public static final String TERMINATE_ONE_PASSIVE_SYN = synFrom(TERMINATE_ONE_PASSIVE);
  public static final String TERMINATE_ONE_PASSIVE_ACK = ackFrom(TERMINATE_ONE_PASSIVE);

  public static final String START_ONE_SERVER = "START_ONE_SERVER";
  public static final String START_ONE_SERVER_SYN = synFrom(START_ONE_SERVER);
  public static final String START_ONE_SERVER_ACK = ackFrom(START_ONE_SERVER);

  public static final String START_ALL_SERVERS = "START_ALL_SERVERS";
  public static final String START_ALL_SERVERS_SYN = synFrom(START_ALL_SERVERS);
  public static final String START_ALL_SERVERS_ACK = ackFrom(START_ALL_SERVERS);

  public static final String SHUT_DOWN_STRIPE = "SHUT_DOWN_STRIPE";
  public static final String SHUT_DOWN_STRIPE_SYN = synFrom(SHUT_DOWN_STRIPE);
  public static final String SHUT_DOWN_STRIPE_ACK = ackFrom(SHUT_DOWN_STRIPE);

  public static final String WAIT_FOR_ACTIVE = "WAIT_FOR_ACTIVE";
  public static final String WAIT_FOR_ACTIVE_SYN = synFrom(WAIT_FOR_ACTIVE);
  public static final String WAIT_FOR_ACTIVE_ACK = ackFrom(WAIT_FOR_ACTIVE);

  public static final String WAIT_FOR_PASSIVE = "WAIT_FOR_PASSIVE";
  public static final String WAIT_FOR_PASSIVE_SYN = synFrom(WAIT_FOR_PASSIVE);
  public static final String WAIT_FOR_PASSIVE_ACK = ackFrom(WAIT_FOR_PASSIVE);

  public static final String CLIENT_SHUT_DOWN = "CLIENT_SHUT_DOWN";
  public static final String CLIENT_SHUT_DOWN_SYN = synFrom(CLIENT_SHUT_DOWN);
  public static final String CLIENT_SHUT_DOWN_ACK = ackFrom(CLIENT_SHUT_DOWN);
}
