// Source code is decompiled from a .class file using FernFlower decompiler.
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * This class does all of the group chat client's job
 * The only changes I really made was in the run() method, 
 * where I added the username prompt and the message sending.
 */
public class GroupChatClient implements Runnable {
   private BufferedReader fromUserReader;
   private PrintWriter toSockWriter;

   public GroupChatClient(BufferedReader reader, PrintWriter writer) {
      this.fromUserReader = reader;
      this.toSockWriter = writer;
   }

   public void run() {
      try {
         // basically checks first message from client as its username
         String username = fromUserReader.readLine();
         if (username == null) {
            System.out.println("*** Client closing connection");
            return;
         }
         toSockWriter.println(username);

         // Then send messages once username is set. rest of run() is same as TwoWayAsyncMesgClient.java
         while (true) {
            String message = fromUserReader.readLine();
            if (message == null) {
               System.out.println("*** Client closing connection");
               break;
            }
            toSockWriter.println(message);
         }
      } catch (Exception e) {
         System.out.println(e);
         System.exit(1);
      }
      System.exit(0);
   }

   //args[0] is host, args[1] is port
   //this is practically the same as TwoWayAsyncMesgClient.java main method 
   public static void main(String[] args) {
      if (args.length != 2) {
         System.out.println("usage: java GroupChatClient <host> <port>");
         System.exit(1);
      }

      Socket sock = null;

      try {
         sock = new Socket(args[0], Integer.parseInt(args[1]));
         System.out.println("Connected to server at " + args[0] + ":" + args[1]);
      } catch (Exception var6) {
         System.out.println(var6);
         System.exit(1);
      }

      try {
         PrintWriter toSockWriter = new PrintWriter(sock.getOutputStream(), true);
         BufferedReader fromUserReader = new BufferedReader(new InputStreamReader(System.in));
         // Thread child = new Thread(new TwoWayAsyncMesgClient(fromUserReader, toSockWriter));
         Thread child = new Thread(new GroupChatClient(fromUserReader, toSockWriter));
         child.start();
      } catch (Exception e) {
         System.out.println(e);
         System.exit(1);
      }

      try {
         BufferedReader fromSockReader = new BufferedReader(new InputStreamReader(sock.getInputStream()));

         while(true) {
            String line = fromSockReader.readLine();
            if (line == null) {
               System.out.println("*** Server closed connection");
               break;
            }

            System.out.println("Server: " + line);
         }
      } catch (Exception e) {
         System.out.println(e);
         System.exit(1);
      }

      System.exit(0);
   }
}
