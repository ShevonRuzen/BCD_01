package lk.jiat.bcd.server;

import BankingApp.AccountPOA;
import BankingApp.InsufficientBalance;

import java.util.concurrent.ConcurrentHashMap;

public class AccountImpl extends AccountPOA {

    // Using ConcurrentHashMap to ensure thread safety across concurrent network clients
    private final ConcurrentHashMap<String, Double> db = new ConcurrentHashMap<>();

    public AccountImpl() {
        // Mock data initialization
        db.put("ACC001", 54000.50);
        db.put("ACC002", 149000.0);
        db.put("ACC003", 75000.0);
    }

    @Override
    public double getBalance(String accNo) {
        // If the account doesn't exist, return a sentinel flag value (-1.0)
        if (!db.containsKey(accNo)) {
            System.out.println("Server Log: Validation FAILED for account " + accNo);
            return -1.0;
        }
        return db.get(accNo);
    }

    @Override
    public void deposit(String accNo, double amount) {
        if (!db.containsKey(accNo)) {
            System.out.println("Server Log: Rejecting deposit. Account " + accNo + " does not exist.");
            return;
        }

        // Atomic thread-safe calculation
        db.computeIfPresent(accNo, (key, currentBalance) -> currentBalance + amount);
        System.out.println("Server Log: LKR " + amount + " deposited to account " + accNo);
    }

    @Override
    public void withdraw(String accNo, double amount) throws InsufficientBalance {
        if (!db.containsKey(accNo)) {
            System.out.println("Server Log: Rejecting withdrawal. Account " + accNo + " does not exist.");
            throw new InsufficientBalance("Transaction Denied: Account does not exist.");
        }

        // Synchronize on the DB object lock during evaluation to prevent race conditions
        synchronized (db) {
            double currentBalance = db.get(accNo);

            if (currentBalance < amount) {
                System.out.println("Server Log: Failed withdrawal attempt on " + accNo + " due to low balance.");
                throw new InsufficientBalance("Transaction Denied: Insufficient Account Balance..!");
            }

            db.put(accNo, currentBalance - amount);
        }
        System.out.println("Server Log: LKR " + amount + " withdrawn from account " + accNo);
    }
}