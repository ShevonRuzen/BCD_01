import com.HelloWorld;
import com.HelloWorldHelper;
import org.omg.CORBA.*;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

public class HelloClient {
    public static void main(String[] args) {
        ORB orb = ORB.init(args, null);

        try {
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
            HelloWorld helloWorld = HelloWorldHelper.narrow(ncRef.resolve_str("Hello"));
            helloWorld.print();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
