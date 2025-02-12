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
package com.tc.net.core;

/**
 */
public enum ProductID {
  DIAGNOSTIC(true, false, false, false),
  INFORMATIONAL(true, true, true, false),
  STRIPE(false, true, true, false),
  SERVER(false, false, true, false),
  PERMANENT(false, true, true, true),
  DISCOVERY(true, false, false, false);

  private final boolean internal;
  private final boolean reconnect;
  private final boolean redirect;
  private final boolean permanent;

  ProductID(boolean internal, boolean reconnect, boolean redirect, boolean permanent) {
    this.internal = internal;
    this.reconnect = reconnect;
    this.redirect = redirect;
    this.permanent = permanent;
  }

  public boolean isInternal() {
    return internal;
  }

  public boolean isReconnectEnabled() {
    return reconnect;
  }

  public boolean isRedirectEnabled() {
    return redirect;
  }

  public boolean isPermanent() {
    return permanent;
  }

  @Override
  public String toString() {
    return name().charAt(0) + name().substring(1).toLowerCase();
  }
}
