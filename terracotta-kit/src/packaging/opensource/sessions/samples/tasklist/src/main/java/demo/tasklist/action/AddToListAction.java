/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.action;

import demo.tasklist.common.Constants;
import demo.tasklist.form.AddToListForm;
import demo.tasklist.service.DataKeeper;
import demo.tasklist.service.ErrorKeeper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * AddToListAction processes the request to add an item to the task list.  
 * Task list is fetched from the HttpSession object, the item indicated in 
 * the AddToListForm is added to the list, and the modified list is loaded back 
 * into the HttpSession object.
 */
public class AddToListAction extends Action {
  public ActionForward execute(ActionMapping mapping,
                               ActionForm form,
                               HttpServletRequest request,
                               HttpServletResponse response)
    throws Exception {
    HttpSession session = (HttpSession)request.getSession();

    AddToListForm addToListForm = (AddToListForm) form;
    String newListItem = addToListForm.getNewListItem();
    String errorMsg = addToListForm.getErrorMsg();

    if(errorMsg != null) {
      session.setAttribute(Constants.ERROR_KEY, new ErrorKeeper(errorMsg));
    } else {
      session.removeAttribute(Constants.ERROR_KEY);
    }
      
    DataKeeper dkeeper = (DataKeeper)session.getAttribute( Constants.DATA_KEY);
    if (dkeeper == null) {
      dkeeper = new DataKeeper();
    }
    dkeeper.addListItem(newListItem);
      
    session.setAttribute( Constants.DATA_KEY, dkeeper );
           
    return mapping.findForward(Constants.SUCCESS_KEY ); 
  }
}
