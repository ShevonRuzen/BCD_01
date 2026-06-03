package lk.jiat.rmi.client;

import lk.jiat.rmi.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UserService extends Remote {
    User getUserById(int id) throws RemoteException;
    void addUser(Integer id, User user) throws RemoteException;
    void updateUser(Integer id, User user) throws RemoteException;
    void deleteUser(int id) throws RemoteException;
    List<User> getAllUsers() throws RemoteException;

}
