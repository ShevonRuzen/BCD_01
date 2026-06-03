//  COUNT RMIServer & RMIClient projects as a SINGLE Practical

package lk.jiat.rmi;

import lk.jiat.rmi.client.Message;
import lk.jiat.rmi.client.UserService;
import lk.jiat.rmi.model.Data;
import lk.jiat.rmi.model.User;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

public class RMIClient {
    public static void main(String[] args) {
        try {
//      --------------------method_01-------------------------
//            Registry registry =  LocateRegistry.getRegistry("localhost", 6666);
//            String[] list = registry.list();
//            for (String s : list) {
//                System.out.println(s);
//            }
//      --------------------method_02------------------------
            UserService userService = (UserService) Naming.lookup("rmi://127.0.0.1:6666/user_service");

//      --------------------method_03------------------------
//            Properties prop = new Properties();
//            prop.put(Context.PROVIDER_URL, "rmi://127.0.0.1:6666");
//            prop.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
//
//            InitialContext initialContext = new InitialContext();
//            UserService userService = (UserService) initialContext.lookup("user_service");


//--------------------------------------------------------------

//            Message message = (Message) registry.lookup("message_service");

//            String msg = message.hello();
//            System.out.println(msg);

//            Data data = message.getData();

//            UserService userService = (UserService) registry.lookup("user_service");
            userService.addUser(1, new User(1, "Shehan", "Horana", "shehan@gmail.com"));

            userService.getAllUsers().forEach(user -> System.out.println(user.getName()));

        } catch (Exception e) {
            throw  new RuntimeException(e);
        }
    }
}
