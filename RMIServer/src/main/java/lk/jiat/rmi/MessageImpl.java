package lk.jiat.rmi;

import lk.jiat.rmi.client.Message;
import lk.jiat.rmi.model.Data;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MessageImpl extends UnicastRemoteObject implements Message {

    MessageImpl() throws RemoteException{}

    @Override
    public String hello() throws  RemoteException {
        return "Server: hello";
    }

    @Override
    public Data getData() throws RemoteException {
        return new Data(10, "App Version");
    }

    @Override
    public String getResult(int num1, int num2) throws RemoteException {
        return "Result is: " + num1 + num2;
    }
}
