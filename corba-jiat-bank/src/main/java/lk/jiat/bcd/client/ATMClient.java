package lk.jiat.bcd.client;

import BankingApp.Account;
import BankingApp.AccountHelper;
import BankingApp.InsufficientBalance;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import java.util.Scanner;

public class ATMClient {

    public static void main(String[] args) {
        // Initialize the ORB runtime environment
        ORB orb = ORB.init(args, null);

        try {
            // Locate the CORBA Name Service
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

            // Resolve the "Bank" object reference registered by the server
            Account account = AccountHelper.narrow(ncRef.resolve_str("Bank"));

            Scanner sc = new Scanner(System.in);
            String accountID = "";

            // --- ACCOUNT VALIDATION LOOP ---
            while (true) {
                System.out.print("Enter your account ID: ");
                accountID = sc.nextLine().trim();

                // Check account existence using getBalance execution
                double statusCheck = account.getBalance(accountID);

                if (statusCheck == -1.0) {
                    System.out.println("[ERROR] Incorrect account number! Please enter again.\n");
                } else {
                    System.out.println("\n[SUCCESS] Account verified. Welcome, " + accountID + "!");
                    break; // Escape the loop if it's a valid account
                }
            }

            // --- MAIN TRANSACTION MENU LOOP ---
            boolean running = true;

            while (running) {
                System.out.println("\n=== ATM SERVICES ===");
                System.out.println("1. Check Balance | 2. Deposit | 3. Withdrawal | 4. Exit");
                System.out.print("Choose an option: ");

                String inputStr = sc.nextLine().trim();
                int choice;
                try {
                    choice = Integer.parseInt(inputStr);
                } catch (NumberFormatException e) {
                    System.out.println("[Error] Please enter a valid number (1-4).");
                    continue;
                }

                switch (choice) {
                    case 1:
                        System.out.printf("Current Balance: LKR %.2f\n", account.getBalance(accountID));
                        break;

                    case 2:
                        System.out.print("Enter amount to deposit (LKR): ");
                        double amount = Double.parseDouble(sc.nextLine().trim());
                        account.deposit(accountID, amount);
                        System.out.printf("Successfully deposited LKR %.2f into account %s\n", amount, accountID);
                        System.out.printf("Current Balance: LKR %.2f\n", account.getBalance(accountID));
                        break;

                    case 3:
                        boolean withdrawalLoop = true;

                        while (withdrawalLoop) {
                            System.out.print("Enter amount to withdraw (LKR): ");
                            double withdrawAmount = Double.parseDouble(sc.nextLine().trim());

                            try {
                                account.withdraw(accountID, withdrawAmount);
                                System.out.printf("Successfully withdrew LKR %.2f from account %s\n", withdrawAmount, accountID);
                                System.out.printf("Current Balance: LKR %.2f\n", account.getBalance(accountID));

                                withdrawalLoop = false; // Transaction succeeded, break out to main menu

                            } catch (InsufficientBalance e) {
                                // 1. Catch exception and print reason
                                System.out.println("\n[TRANSACTION DENIED] " + e.msg);

                                // 2. Show current balance immediately after failure
                                System.out.printf("Your Current Balance is: LKR %.2f\n\n", account.getBalance(accountID));

                                // 3. Ask user whether to retry or go back
                                while (true) {
                                    System.out.println("Do you want to:");
                                    System.out.println("1. Try another withdrawal amount");
                                    System.out.println("2. Exit to Main Menu");
                                    System.out.print("Choose an option (1 or 2): ");

                                    String retryChoice = sc.nextLine().trim();

                                    if (retryChoice.equals("1")) {
                                        System.out.println(); // Just print a new line and let the loop retry
                                        break;
                                    } else if (retryChoice.equals("2")) {
                                        System.out.println("Returning to Main Menu...");
                                        withdrawalLoop = false; // Break the withdrawal cycle
                                        break; // Break the choice loop
                                    } else {
                                        System.out.println("[Error] Invalid option. Please enter 1 or 2.\n");
                                    }
                                }
                            }
                        }
                        break;

                    case 4:
                        System.out.println("\nThank you for using BankingApp!");
                        System.out.println("Exiting...");
                        running = false;
                        break;

                    default:
                        System.out.println("[Error] Invalid choice. Please pick between 1 and 4.");
                }
            }

            sc.close();

        } catch (Exception e) {
            System.err.println("Fatal Client Exception occurred:");
            e.printStackTrace();
        }
    }
}