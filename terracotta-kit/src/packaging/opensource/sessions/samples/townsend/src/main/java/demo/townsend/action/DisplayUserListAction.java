/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.townsend.action;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.action.DynaActionForm;

import demo.townsend.common.Constants;
import demo.townsend.service.DataKeeper;

/**
 * DisplayUserListAction processes the request to display the user's list.
 * User's list is fetched from the HttpSession object, and a dynamic form (i.e.,
 * displayUserListForm) is populated with this data.
 */
public class DisplayUserListAction extends Action {
  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form,
      HttpServletRequest request, HttpServletResponse response)
      throws Exception {

    HttpSession session = request.getSession();
    if (session == null) {
      ActionMessages errors = new ActionMessages();
      errors.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage(
          "error.expired.session"));
      saveErrors(request, errors);
      return mapping.findForward(Constants.NO_SESSION);
    }
    DataKeeper dkeeper = (DataKeeper) session
        .getAttribute(Constants.DATA_KEY);
    if (dkeeper == null) {
      dkeeper = new DataKeeper();
    }

    ((DynaActionForm) form).set("recentList", dkeeper.getList());
    ((DynaActionForm) form).set("listLength", Integer.toString(dkeeper
        .getListSize()));
    ((DynaActionForm) form).set("currentProduct", dkeeper.getCurrent());

    return mapping.findForward(Constants.SUCCESS_KEY);
  }
}
