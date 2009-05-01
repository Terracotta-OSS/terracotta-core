/*
@COPYRIGHT@
*/
package demo.jmx.web;

import demo.jmx.ICounter;
import demo.jmx.IHistory;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

/**
* Web controller
*/
public class CounterFormController 
extends MultiActionController 
{
   private transient ICounter localCounter;
   private transient IHistory localHistory;
   private transient ICounter clusteredCounter;
   private transient IHistory clusteredHistory;

   public void setLocalCounter(ICounter counter) 
   {
      this.localCounter = counter;
   }

   public void setLocalHistory(IHistory history) 
   {
      this.localHistory = history;
   }

   public void setClusteredCounter(ICounter clusteredCounter) 
   {
      this.clusteredCounter = clusteredCounter;
   }

   public void setClusteredHistory(IHistory clusteredHistory) 
   {
      this.clusteredHistory = clusteredHistory;
   }

   /**
   * Controller method to handle refresh action
   */
   public ModelAndView handleRefresh(HttpServletRequest request, HttpServletResponse response) 
   throws Exception 
   {
      Map model = new HashMap();
      model.put("localCounter", new Integer(localCounter.getCurrent()));
      model.put("localHistory", localHistory.getHistory());
      model.put("clusteredCounter", new Integer(clusteredCounter.getCurrent()));
      model.put("clusteredHistory", clusteredHistory.getHistory());
      return new ModelAndView("index", model);
   }

   /**
   * Controller method to handle counter increment action
   */
   public ModelAndView incrementLocal(HttpServletRequest request, HttpServletResponse response) 
   throws Exception 
   {
      localCounter.next();
      return new ModelAndView("redirect:index.jsp", null);
   }

   public ModelAndView incrementClustered(HttpServletRequest request, HttpServletResponse response) 
   throws Exception 
   {
      clusteredCounter.next();
      return new ModelAndView("redirect:index.jsp", null);
   }
}
