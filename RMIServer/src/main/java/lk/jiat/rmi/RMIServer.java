package lk.jiat.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIServer {
    public static void main(String[] args) {
         try {
             Registry registry = LocateRegistry.createRegistry(6666);
             registry.bind("message_service", new MessageImpl());

             System.out.println("RMI server started on port 6666.");
         } catch (Exception e) {
             throw  new RuntimeException(e);
         }
    }
}
