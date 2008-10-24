/*
 * @COPYRIGHT@
 */
package demo.webflow;

import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.definition.TransitionDefinition;
import org.springframework.webflow.engine.State;
import org.springframework.webflow.execution.EnterStateVetoException;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.View;

/**
 * Flow execution listener used to capture execution history
 */
public class WebFlowHistory implements FlowExecutionListener {
  public void sessionStarted(RequestContext ctx, FlowSession flowSession) {
    // String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
    String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
    System.err.println("### WebFlowHistory.sessionStarted() " + id);
  }

  public void stateEntering(RequestContext ctx, State state) throws EnterStateVetoException {
    // String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
    String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
    System.err.println("### WebFlowHistory.stateEntering() " + id);
  }

  // public void sessionEnded(RequestContext ctx, FlowSession flowSession, UnmodifiableAttributeMap attributeMap)
  public void sessionEnded(RequestContext ctx, FlowSession flowSession, LocalAttributeMap attributeMap) {
    // String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
    String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
    System.err.println("### WebFlowHistory.sessionEnded() " + id);
  }

  public void sessionEnding(RequestContext ctx, FlowSession flowSession, AttributeMap attributeMap) {
    // String id = flowSession.getFlow().getId();
    String id = flowSession.getDefinition().getId();
  }

  public void eventSignaled(RequestContext arg0, Event arg1) {
    // TODO Auto-generated method stub

  }

  public void exceptionThrown(RequestContext arg0, FlowExecutionException arg1) {
    // TODO Auto-generated method stub

  }

  public void paused(RequestContext arg0) {
    // TODO Auto-generated method stub

  }

  public void requestProcessed(RequestContext arg0) {
    // TODO Auto-generated method stub

  }

  public void requestSubmitted(RequestContext arg0) {
    // TODO Auto-generated method stub

  }

  public void resuming(RequestContext arg0) {
    // TODO Auto-generated method stub

  }

  public void sessionCreating(RequestContext arg0, FlowDefinition arg1) {
    // TODO Auto-generated method stub

  }

  public void sessionEnded(RequestContext arg0, FlowSession arg1, String arg2, AttributeMap arg3) {
    // TODO Auto-generated method stub

  }

  public void sessionEnding(RequestContext arg0, FlowSession arg1, String arg2, MutableAttributeMap arg3) {
    // TODO Auto-generated method stub

  }

  public void sessionStarting(RequestContext arg0, FlowSession arg1, MutableAttributeMap arg2) {
    // TODO Auto-generated method stub

  }

  public void stateEntered(RequestContext arg0, StateDefinition arg1, StateDefinition arg2) {
    // TODO Auto-generated method stub

  }

  public void stateEntering(RequestContext arg0, StateDefinition arg1) throws EnterStateVetoException {
    // TODO Auto-generated method stub

  }

  public void transitionExecuting(RequestContext arg0, TransitionDefinition arg1) {
    // TODO Auto-generated method stub

  }

  public void viewRendered(RequestContext arg0, View arg1, StateDefinition arg2) {
    // TODO Auto-generated method stub

  }

  public void viewRendering(RequestContext arg0, View arg1, StateDefinition arg2) {
    // TODO Auto-generated method stub

  }

}
