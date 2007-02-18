/*
@COPYRIGHT@
*/
package demo.chatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class MessageManager
{
	private transient List listeners = new ArrayList();
	private List messages;

	public MessageManager() 
	{ 
	   messages = Collections.synchronizedList(new ArrayList());
	}

	public synchronized void send(String sender, String message)
	{
	   Message msg = new Message(sender, message); 
	   messages.add(msg);
	   notifyListeners(msg);
	}

	public void addListener(MessageListener l) 
	{
	   listeners.add(l);
	}

	public void removeListener(MessageListener l) 
	{
           listeners.remove(l);
	}

	private synchronized void notifyListeners(Message message) 
	{
	   for (Iterator iterator=listeners.iterator(); iterator.hasNext();) 
	   {
	      MessageListener l = (MessageListener)iterator.next();
              l.read(message);
           }
	}
	
	public Message[] getMessages()
	{
	   return (Message[])messages.toArray(new Message[0]);
	}
}
