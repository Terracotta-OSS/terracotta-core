/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.action;

import demo.tasklist.common.Constants;
import demo.tasklist.form.DeleteFromListForm;
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
 * DeleteFromListAction processes the request to delete one or more items from
 * the task list.  Task list is fetched from the HttpSession object, 
 * items indicated in the DeleteFromListForm are deleted from the list, and
 * the modified list is loaded back into the HttpSession object.
 */
public class DeleteFromListAction extends Action {
   public ActionForward execute( ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest request,
                                 HttpServletResponse response)
    throws Exception {

     HttpSession session = (HttpSession)request.getSession();
     
     DeleteFromListForm deleteFromListForm = (DeleteFromListForm)form;
     String[] itemsForDelete = deleteFromListForm.getItemsForDelete();
     String errorMsg = deleteFromListForm.getErrorMsg();
     
     if(errorMsg != null) {
       session.setAttribute(Constants.ERROR_KEY, new ErrorKeeper(errorMsg));
     }
     else {
       session.removeAttribute(Constants.ERROR_KEY);
     }
      
      DataKeeper dkeeper = (DataKeeper)session.getAttribute( Constants.DATA_KEY);
      if (dkeeper == null) {
        dkeeper = new DataKeeper();
      }
      dkeeper.deleteListItems(itemsForDelete);
      
      session.setAttribute( Constants.DATA_KEY, dkeeper );
  
      return mapping.findForward(Constants.SUCCESS_KEY ); 
  }
}
