/*
@COPYRIGHT@
*/
package demo.chatter;

class Message {
   private String message;
   private String sender;

   public Message(String sender, String message) {
      this.sender = sender;
      this.message = message;
   }

   public String getMessage() {
      return message;
   }

   public String getSender() {
      return sender;
   }
}
