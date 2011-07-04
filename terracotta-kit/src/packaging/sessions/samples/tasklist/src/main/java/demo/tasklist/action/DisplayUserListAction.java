/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.action;

import demo.tasklist.common.Constants;
import demo.tasklist.service.DataKeeper;
import demo.tasklist.service.ErrorKeeper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * DisplayUserListAction processes the request to display the task list.
 * Task list is fetched from the HttpSession object, and a dynamic form 
 * (i.e., displayUserListForm) is populated with this data.
 */
public class DisplayUserListAction extends Action {
  public ActionForward execute( ActionMapping mapping, ActionForm form,
    HttpServletRequest request, HttpServletResponse response) throws Exception {
    HttpSession session = request.getSession();
    ErrorKeeper errorKeeper = (ErrorKeeper) session.getAttribute(Constants.ERROR_KEY);
    String errorMsg = errorKeeper != null ? errorKeeper.getErrorMsg() : "";
      
    if(errorMsg == null) {
      errorMsg = "";
    }
           
    DataKeeper dkeeper = (DataKeeper) session.getAttribute(Constants.DATA_KEY );
    if (dkeeper == null) {
      dkeeper = new DataKeeper();
    }
    String numTasks = Integer.toString(dkeeper.getListSize());

    ((DynaActionForm)form).set( "userList", dkeeper.getList());
    ((DynaActionForm)form).set( "numTasks", numTasks);
    ((DynaActionForm)form).set( "errorMsg", errorMsg);

    return mapping.findForward( Constants.SUCCESS_KEY );
  }
}
