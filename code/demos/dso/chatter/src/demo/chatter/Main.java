/*
@COPYRIGHT@
*/
package demo.chatter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.Point;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.SwingUtilities;

public class Main 
   extends JFrame implements MessageListener, ActionListener 
{
   private MessageManager messageManager;
   private String username;
   private JTextPane display;

   public Main(String username) 
   {
      super("Chatter: " + username);
      messageManager = new MessageManager();

      setDefaultLookAndFeelDecorated(true);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      Container content = getContentPane();

      display = new JTextPane();
      display.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 9));
      display.setEditable(false);
      display.setRequestFocusEnabled(false);

      final JTextField input = new JTextField();
      input.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 9));
      input.addActionListener(this);
      JScrollPane scroll = new JScrollPane(display);
      Random r           = new Random();
      JLabel buddy       = new JLabel(username, new ImageIcon("images/buddy" + r.nextInt(10) + ".gif"), JLabel.LEFT);
      buddy.setFont(new Font("Lucida Sans Typewriter", Font.PLAIN, 16));
      buddy.setVerticalTextPosition(JLabel.CENTER);
      JPanel buddypanel = new JPanel();
      buddypanel.setBackground(Color.WHITE);
      buddypanel.setLayout(new BorderLayout());
      buddypanel.add(buddy, BorderLayout.CENTER);

      content.setLayout(new BorderLayout());
      content.add(buddypanel, BorderLayout.NORTH);
      content.add(scroll, BorderLayout.CENTER);
      content.add(input, BorderLayout.SOUTH);
      pack();

      this.username = username;
      setSize(new Dimension(300, 400));
      setVisible(true);
      
      input.requestFocus();
      login();
   }  
  
   public void read(Message message) 
   {
      String sender = message.getSender();
      String text   = message.getMessage();
      try 
      { 
         Document doc = display.getDocument();
         Style style  = display.addStyle("Style", null);

         if (sender.equals("")) 
         {
            StyleConstants.setItalic(style, true);
            StyleConstants.setForeground(style, Color.LIGHT_GRAY);
            StyleConstants.setFontSize(style, 9);
         }
         else 
         {
            if (sender.equals(username)) 
            {
               StyleConstants.setItalic(style, true);
               StyleConstants.setForeground(style, Color.GRAY);
            }            
            StyleConstants.setBold(style, true);
            doc.insertString(doc.getLength(), sender + ": ", style); 
         }

         StyleConstants.setBold(style, false);
         int offset = doc.getLength();
         doc.insertString(doc.getLength(), text, style); 
         doc.insertString(doc.getLength(), "\n", style); 

         java.util.Hashtable emoticons = new java.util.Hashtable();
         emoticons.put(":P", "belat");
         emoticons.put(":D", "biggrin");
         emoticons.put(":}", "blush");
         emoticons.put(":$", "confused");
         emoticons.put("B)", "cool");
         emoticons.put(":'(", "cry");
         emoticons.put(":O", "eek");
         emoticons.put(":(", "frown");
         emoticons.put("%)", "funky");
         emoticons.put(">|", "mad");
         emoticons.put(":|", "muted");
         emoticons.put(":/", "sarcastic");
         emoticons.put(":)", "smile");
         emoticons.put(";)", "wink");

         for (java.util.Enumeration e=emoticons.keys(); e.hasMoreElements();) 
         {
            String symbol = e.nextElement().toString();
            String icon   = emoticons.get(symbol).toString();

            while (true) 
            {
               text    = doc.getText(offset, text.length());
               int pos = text.indexOf(symbol);
               if (pos < 0) break;
               StyleConstants.setIcon(style, new ImageIcon("images/" + icon + ".gif"));
               doc.insertString(offset + pos, ".", style);
               doc.remove(offset + pos + 1, symbol.length());
            }
         }
         display.setCaretPosition(doc.getLength());
      }
      catch (javax.swing.text.BadLocationException ble) 
      { 
         System.err.println(ble.getMessage()); 
      }
   }
  
   public void actionPerformed(ActionEvent e) 
   {
      JTextField input     = (JTextField) e.getSource();
      final String message =  input.getText();
      input.setText("");
      Thread sender = new Thread(
         new Runnable() {
            public void run() {
               messageManager.send(username, message);
            }
         });
      sender.start();
   }

   synchronized void login() 
   {
      messageManager.send("", username + " entered the chat");
      Message[] messages = messageManager.getMessages();
      for(int i=0; i<messages.length; i++)
         read(messages[i]);
      messageManager.addListener(this);
   }
  
   synchronized void logout() 
   {
      messageManager.removeListener(this); 
      messageManager.send("", username + " left the chat");
   }

   private static String chatname(String name) 
   {
      Random r = new Random();
      if (name.length() > 0) return name + r.nextInt(10000);

      String[] cool = 
      { 
         "Miles", "Ella", "Nina", "Duke", "Charlie", "Billie", "Louis", "Fats", "Thelonious", "Dizzy",
         "Davis", "Fitzgerald", "Simone", "Ellington", "Parker", "Holiday", "Armstrong", "Waller", "Monk", "Gillespie"
      };
      return cool[r.nextInt(10)] + cool[r.nextInt(10) + 10];
   }

   public static void main(String[] args) 
   {
      final String name = args.length == 0 ? "" : args[0];
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            new Main(Main.chatname(name));
         }
      });
   }
}
