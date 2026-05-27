package lk.jiat.bcd.server;

import BankingApp.Account;
import BankingApp.AccountHelper;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

public class BankServer {

    public static void main(String[] args) {
        ORB orb = ORB.init(args, null);

        try {
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();

            AccountImpl account = new AccountImpl();
            org.omg.CORBA.Object object1 = rootPOA.servant_to_reference(account);

            Account href = AccountHelper.narrow(object1);

            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            String name = "Bank";
            NameComponent path[] = ncRef.to_name(name);
            ncRef.rebind(path, href);

            System.out.println("Bank server started.");

            orb.run();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

}
