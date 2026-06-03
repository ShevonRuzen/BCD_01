package lk.jiat.rmi;

import lk.jiat.rmi.client.Message;
import lk.jiat.rmi.client.UserService;
import lk.jiat.rmi.model.Data;
import lk.jiat.rmi.model.User;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class RMIClient {
    public static void main(String[] args) {
        try {
            Registry registry =  LocateRegistry.getRegistry("localhost", 6666);
            String[] list = registry.list();
            for (String s : list) {
                System.out.println(s);
            }

//            Message message = (Message) registry.lookup("message_service");

//            String msg = message.hello();
//            System.out.println(msg);

//            Data data = message.getData();

            UserService userService = (UserService) registry.lookup("user_service");
            userService.addUser(1, new User(1, "Shehan", "Horana", "shehan@gmail.com"));

            userService.getAllUsers().forEach(user -> System.out.println(user.getName()));
        } catch (Exception e) {
            throw  new RuntimeException(e);
        }
    }
}
