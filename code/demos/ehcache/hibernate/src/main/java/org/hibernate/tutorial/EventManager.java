package org.hibernate.tutorial;

import org.hibernate.Session;
import org.hibernate.tutorial.domain.Event;
import org.hibernate.tutorial.util.HibernateUtil;

import java.util.Date;
import java.util.List;

public class EventManager {
  public static void main(String[] args) {
    EventManager mgr = new EventManager();

    if (args.length == 0 || args[0].equals("store")) {
      mgr.createAndStoreEvent("My Event", new Date());
    } else if (args[0].equals("list")) {
      List events = mgr.listEvents();
      for (int i = 0; i < events.size(); i++) {
        Event theEvent = (Event) events.get(i);
        System.out.println("Event: " + theEvent.getTitle() + " Time: " + theEvent.getDate());
      }
    }

    HibernateUtil.getSessionFactory().close();
  }

  private void createAndStoreEvent(String title, Date theDate) {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();

    Event theEvent = new Event();
    theEvent.setTitle(title);
    theEvent.setDate(theDate);
    session.save(theEvent);

    session.getTransaction().commit();
  }

  private List listEvents() {
    Session session = HibernateUtil.getSessionFactory().getCurrentSession();
    session.beginTransaction();
    List result = session.createQuery("from Event").list();
    session.getTransaction().commit();
    return result;
  }
}
