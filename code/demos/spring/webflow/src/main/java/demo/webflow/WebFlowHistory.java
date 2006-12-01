/*
@COPYRIGHT@
*/
package demo.webflow;

//import org.springframework.webflow.UnmodifiableAttributeMap;
import org.springframework.webflow.core.collection.AttributeMap;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.core.collection.MutableAttributeMap;
import org.springframework.webflow.definition.FlowDefinition;
import org.springframework.webflow.definition.StateDefinition;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.engine.State;
import org.springframework.webflow.execution.EnterStateVetoException;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.FlowExecutionException;
import org.springframework.webflow.execution.FlowExecutionListener;
import org.springframework.webflow.execution.FlowSession;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.ViewSelection;

/**
* Flow execution listener used to capture execution history 
*/
public class WebFlowHistory 
implements FlowExecutionListener 
{
   public void sessionStarted(RequestContext ctx, FlowSession flowSession) 
   {
      //String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
      String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
      System.err.println("### WebFlowHistory.sessionStarted() "+id);
   }

   public void stateEntering(RequestContext ctx, State state) 
   throws EnterStateVetoException 
   {
      //String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
      String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
      System.err.println("### WebFlowHistory.stateEntering() "+id);
   }

   //  public void sessionEnded(RequestContext ctx, FlowSession flowSession, UnmodifiableAttributeMap attributeMap) 
   public void sessionEnded(RequestContext ctx, FlowSession flowSession, LocalAttributeMap attributeMap) 
   {
      //String id = ctx.getFlowExecutionContext().getActiveSession().getFlow().getId();
      String id = ctx.getFlowExecutionContext().getActiveSession().getDefinition().getId();
      System.err.println("### WebFlowHistory.sessionEnded() "+id);
   }

   public void sessionEnding(RequestContext ctx, FlowSession flowSession, AttributeMap attributeMap) 
   {
      //String id = flowSession.getFlow().getId();
      String id = flowSession.getDefinition().getId();
   }

   public void sessionStarting(RequestContext context, FlowDefinition definition, MutableAttributeMap input) 
   {
      // TODO
   }

   public void stateEntered(RequestContext ctx, State previousState, State state) 
   {
      // TODO
   }

   public void eventSignaled(RequestContext ctx, Event event) 
   {
      // TODO
   }

   public void paused(RequestContext ctx, ViewSelection viewSelection) 
   {
      // TODO
   }

   public void requestProcessed(RequestContext ctx) 
   {
      // TODO
   }

   public void requestSubmitted(RequestContext ctx) 
   {
      // TODO
   }

   public void resumed(RequestContext ctx) 
   {
      // TODO
   }

   public void exceptionThrown(RequestContext context, FlowExecutionException exception) 
   {
      // TODO
   }

   public void sessionEnded(RequestContext context, FlowSession session, AttributeMap output) 
   {
      // TODO
   }

   public void sessionEnding(RequestContext context, FlowSession session, MutableAttributeMap output) 
   {
      // TODO
   }

   public void stateEntered(RequestContext context, StateDefinition previousState, StateDefinition state)  
   {
      // TODO
   }

   public void stateEntering(RequestContext context, StateDefinition state)  
   {
      // TODO
   }
}
