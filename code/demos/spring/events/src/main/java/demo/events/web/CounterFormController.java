/*
@COPYRIGHT@
*/
package demo.events.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import demo.events.EventProcessor;
import demo.events.MessageEvent;

/**
 * Web controller for the Events sample application; provides the list of current messages and publishes new messages as
 * events to the {@link ApplicationContext}, which is distributed by Terracotta for Spring.
 * 
 * @author Eugene Kuleshov
 */
public class CounterFormController extends MultiActionController {

  private transient EventProcessor eventProcessor;

  public void setEventProcessor(EventProcessor eventProcessor) {
    this.eventProcessor = eventProcessor;
  }

  /**
   * Controller method to handle refresh action
   */
  public ModelAndView handleRefresh(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Map model = new HashMap();
    model.put("events", eventProcessor.getEvents());
    return new ModelAndView("index", model);
  }

  /**
   * Controller method to handle message submission action
   */
  public ModelAndView handleMessage(HttpServletRequest request, HttpServletResponse response) throws Exception {
    String message = RequestUtils.getRequiredStringParameter(request, "message");
    String sender = RequestUtils.getRequiredStringParameter(request, "sender");

    ApplicationContext ctx = getApplicationContext();
    ctx.publishEvent(new MessageEvent(sender, message));

    return new ModelAndView("redirect:index.jsp", null);
  }

}
