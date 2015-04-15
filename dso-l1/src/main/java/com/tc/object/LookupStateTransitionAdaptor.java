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
package com.tc.object;

class LookupStateTransitionAdaptor implements LookupStateTransition {

  private LookupState state = LookupState.UNINITALIZED;

  @Override
  public boolean isPrefetch() {
    return this.state.isPrefetch();
  }

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
  public LookupState makePrefetchRequest() {
    this.state = this.state.makePrefetchRequest();
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
