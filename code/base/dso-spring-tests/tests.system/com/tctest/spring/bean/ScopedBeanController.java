/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class ScopedBeanController extends MultiActionController implements BeanFactoryAware {

  private BeanFactory beanFactory;

  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }
  
  
  public ModelAndView getValue(HttpServletRequest request, HttpServletResponse response) throws Exception {
    Map model = new HashMap();
    model.put("scopedValue", getSessionScopedBean().getValue());
    return new ModelAndView("scopedBeans", model);
  }
  
  public ModelAndView setValue(HttpServletRequest request, HttpServletResponse response) throws Exception {
    String value = request.getParameter("value");
    if(value!=null) {
      getSessionScopedBean().setValue(value);
    }
    Map model = new HashMap();
    model.put("scopedValue", getSessionScopedBean().getValue());
    return new ModelAndView("scopedBeans", model);
  }

  private ScopedBean getSessionScopedBean() {
    return (ScopedBean) beanFactory.getBean("sessionScopedBean");
  }

}

