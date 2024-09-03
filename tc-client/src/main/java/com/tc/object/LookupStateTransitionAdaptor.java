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
package com.tc.object;

class LookupStateTransitionAdaptor implements LookupStateTransition {

  private LookupState state = LookupState.UNINITALIZED;

  @Override
  public boolean isMissing() {
    return this.state.isMissing();
  }

  @Override
  public boolean isPending() {
    return this.state.isPending();
  }

  @Override
  public LookupState makeLookupRequest() {
    this.state = this.state.makeLookupRequest();
    return this.state;
  }

  @Override
  public LookupState makeMissingObject() {
    this.state = this.state.makeMissingObject();
    return this.state;
  }

  @Override
  public LookupState makePending() {
    this.state = this.state.makePending();
    return this.state;
  }

  @Override
  public LookupState makeUnPending() {
    this.state = this.state.makeUnPending();
    return this.state;
  }

  protected LookupState getState() {
    return this.state;
  }

}
