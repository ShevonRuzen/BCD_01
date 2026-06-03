package lk.jiat.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MessageImpl extends UnicastRemoteObject implements Message {

    MessageImpl() throws RemoteException{}

    @Override
    public String hello() throws  RemoteException {
        return "Server: hello";
    }
}
