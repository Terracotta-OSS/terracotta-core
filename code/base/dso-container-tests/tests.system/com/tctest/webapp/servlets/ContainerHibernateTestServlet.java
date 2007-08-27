/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.webapp.servlets;

import org.hibernate.LockMode;
import org.hibernate.Session;

import com.tc.util.Assert;
import com.tctest.domain.Event;
import com.tctest.domain.EventManager;
import com.tctest.domain.HibernateUtil;
import com.tctest.domain.Person;
import com.tctest.domain.PhoneNumber;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class ContainerHibernateTestServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession(true);
    response.setContentType("text/plain");
    PrintWriter out = response.getWriter();

    String server = request.getParameter("server");
    if ("server0".equals(server)) {
      try {
        doServer0(session);
        out.println("OK");
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    } else if ("server1".equals(server)) {
      try {
        doServer1(session);
        out.println("OK");
      } catch (Exception e) {
        e.printStackTrace(out);
      }
    }
    out.flush();
  }

  private void doServer0(HttpSession httpSession) throws Exception {
    HibernateUtil.dropAndCreateDatabaseSchema();
    EventManager mgr = new EventManager();

    // create 3 persons Steve, Orion, Tim
    Long steveId = mgr.createAndStorePerson("Steve", "Harris");
    mgr.addEmailToPerson(steveId, "steve@tc.com");
    mgr.addEmailToPerson(steveId, "sharrif@tc.com");

    PhoneNumber p1 = new PhoneNumber();
    p1.setNumberType("Office");
    p1.setPhone(111111);
    mgr.addPhoneNumberToPerson(steveId, p1);

    PhoneNumber p2 = new PhoneNumber();
    p2.setNumberType("Home");
    p2.setPhone(222222);
    mgr.addPhoneNumberToPerson(steveId, p2);

    Long orionId = mgr.createAndStorePerson("Orion", "Letizi");
    mgr.addEmailToPerson(orionId, "orion@tc.com");

    Long timId = mgr.createAndStorePerson("Tim", "Teck");
    mgr.addEmailToPerson(timId, "teck@tc.com");

    Long engMeetingId = mgr.createAndStoreEvent("Eng Meeting", new Date());
    mgr.addPersonToEvent(steveId, engMeetingId);
    mgr.addPersonToEvent(orionId, engMeetingId);
    mgr.addPersonToEvent(timId, engMeetingId);

    Long docMeetingId = mgr.createAndStoreEvent("Doc Meeting", new Date());
    mgr.addPersonToEvent(steveId, docMeetingId);
    mgr.addPersonToEvent(orionId, docMeetingId);

    List eventList = mgr.listEvents();

    // list emails of people in Eng meeting
    List emailList = mgr.listEmailsOfEvent(engMeetingId);
    System.out.println("Email list for Eng meeting: " + emailList);
    httpSession.setAttribute("events", eventList);
    System.out.println("added event list to session");

    HibernateUtil.getSessionFactory().close();
  }

  private void doServer1(HttpSession httpSession) throws Exception {
    // comment this out intentionally
    // List events = (List) httpSession.getAttribute("events");

    EventManager mgr = new EventManager();
    // this will get the data from ehcache
    List events = mgr.listEvents();

    System.out.println("events: " + events);
    Assert.assertTrue(events.size() >= 2);

    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();

    // reassociate transient pojos to this session
    for (Iterator it = events.iterator(); it.hasNext();) {
      session.lock((Event) it.next(), LockMode.NONE);
    }

    // list people in first event
    Event event = (Event) events.get(0);
    Set people = event.getParticipants();
    System.out.println("people: " + people);

    // list emails of people from first event
    Set emails = new HashSet();
    for (Iterator it = people.iterator(); it.hasNext();) {
      Person person = (Person) it.next();
      emails.addAll(person.getEmailAddresses());
    }
    System.out.println("emails: " + emails);
    Assert.assertTrue(emails.size() >= 3);

    Set phones = new HashSet();
    for (Iterator it = people.iterator(); it.hasNext();) {
      Person person = (Person) it.next();
      phones.addAll(person.getPhoneNumbers());
    }
    System.out.println("phones: " + phones);
    Assert.assertTrue(phones.size() >= 2);

    session.getTransaction().commit();
    HibernateUtil.getSessionFactory().close();
    System.out.println("DONE!");
  }
}
