/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

class LookupStateTransitionAdaptor implements LookupStateTransition {

  private LookupState state = LookupState.UNINITALIZED;

  public boolean isPrefetch() {
    return this.state.isPrefetch();
  }

  public boolean isMissing() {
    return this.state.isMissing();
  }

  public boolean isPending() {
    return this.state.isPending();
  }

  public LookupState makeLookupRequest() {
    this.state = this.state.makeLookupRequest();
    return this.state;
  }

  public LookupState makeMissingObject() {
    this.state = this.state.makeMissingObject();
    return this.state;
  }

  public LookupState makePending() {
    this.state = this.state.makePending();
    return this.state;
  }

  public LookupState makePrefetchRequest() {
    this.state = this.state.makePrefetchRequest();
    return this.state;
  }

  public LookupState makeUnPending() {
    this.state = this.state.makeUnPending();
    return this.state;
  }

  protected LookupState getState() {
    return this.state;
  }

}
