import com.HelloWorld;
import com.HelloWorldHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.*;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import sun.security.acl.WorldGroupImpl;

public class HelloServer {
    public static void main(String[] args) {
        ORB orb = ORB.init(args, null);
        try {
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();

            HelloWorldImpl hello = new HelloWorldImpl();

            org.omg.CORBA.Object obj1 = rootPOA.servant_to_reference(hello);

            HelloWorld href = HelloWorldHelper.narrow(obj1);

            org.omg.CORBA.Object obj2 = orb.resolve_initial_references("NameService");

            NamingContextExt ncRef = NamingContextExtHelper.narrow(obj2);

            NameComponent path[] = ncRef.to_name("Hello");

            ncRef.rebind(path, href);
            System.out.println("CORBA Server Ready");
            orb.run();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
