package revpay.App;

import revpay.DAO.TransactionDAO;
import revpay.DAO.Cards_DAOs;
import revpay.Services.*;

import java.util.Scanner;

public class ProjectDemo {

    private static final long SESSION_TIMEOUT = 300000; // 5 minutes

    public static void main(String[] args) throws Exception {

        Scanner sc = new Scanner(System.in);
        AuthService authService = new AuthService();

        while (true) {

            System.out.println("\n===== RevPay Digital Wallet =====");
            System.out.println("1. Create Account");
            System.out.println("2. User Sign In");
            System.out.println("3. Recover Password");
            System.out.println("4. Close Application");
            System.out.print("Choose option: ");

            int choice = sc.nextInt();
            sc.nextLine();

            /* ================= CREATE ACCOUNT ================= */
            if (choice == 1) {

                System.out.println("Select Account Category");
                System.out.println("1. Personal User");
                System.out.println("2. Business User");

                int type = sc.nextInt();
                sc.nextLine();

                System.out.print("Full Name: ");
                String name = sc.nextLine();

                System.out.print("Email Address: ");
                String email = sc.nextLine();

                System.out.print("Phone Number: ");
                String phone = sc.nextLine();

                System.out.print("Account Password: ");
                String password = sc.nextLine();

                System.out.print("Transaction PIN: ");
                String pin = sc.nextLine();

                System.out.print("Security Question: ");
                String question = sc.nextLine();

                System.out.print("Security Answer: ");
                String answer = sc.nextLine();

                if (type == 1) {
                    authService.registerPersonal(
                            name, email, phone, password, pin, question, answer
                    );
                } else {
                    System.out.print("Business Name: ");
                    String businessName = sc.nextLine();

                    System.out.print("Tax Identification Number: ");
                    String taxId = sc.nextLine();

                    System.out.print("Business Address: ");
                    String address = sc.nextLine();

                    authService.registerBusiness(
                            name, email, phone, password, pin,
                            businessName, taxId, address,
                            question, answer
                    );
                }
            }

            /* ================= USER SIGN IN ================= */
            else if (choice == 2) {

                System.out.print("Email or Phone: ");
                String loginInput = sc.nextLine();

                System.out.print("Password: ");
                String password = sc.nextLine();

                int userId = authService.login(loginInput, password);

                if (userId == -1) {
                    System.out.println("Authentication Failed");
                    continue;
                }

                long sessionStartTime = System.currentTimeMillis();

                WalletService walletService = new WalletService();
                Cards_DAOs cardDAO = new Cards_DAOs();
                LoanService loanService = new LoanService();
                TransactionDAO txDao = new TransactionDAO();
                NotificationService notificationService = new NotificationService();
                InvoiceService invoiceService = new InvoiceService();
                MoneyRequestService moneyRequestService = new MoneyRequestService();

                /* ========== USER CONTROL PANEL ========== */
                while (true) {

                    if (isSessionExpired(sessionStartTime)) {
                        System.out.println("Session expired. Please login again.");
                        break;
                    }

                    System.out.println("\n----- User Control Panel -----");
                    System.out.println("1. Wallet Management");
                    System.out.println("2. Card Services");
                    System.out.println("3. Loan Services");
                    System.out.println("4. Transaction History");
                    System.out.println("5. Alerts & Notifications");
                    System.out.println("6. Money Requests");
                    System.out.println("7. Invoice Management");
                    System.out.println("8. Sign Out");
                    System.out.print("Choose option: ");

                    int menu = sc.nextInt();
                    sc.nextLine();

                    /* -------- WALLET -------- */
                    if (menu == 1) {

                        System.out.println("1. View Balance");
                        System.out.println("2. Add Funds");
                        System.out.println("3. Withdraw Funds");

                        int w = sc.nextInt();
                        sc.nextLine();

                        if (w == 1) {
                            walletService.viewBalance(userId);
                        }
                        else if (w == 2) {
                            System.out.print("Enter amount to add: ");
                            double amount = sc.nextDouble();
                            sc.nextLine();
                            walletService.deposit(userId, amount);
                        }
                        else if (w == 3) {
                            System.out.print("Enter amount to withdraw: ");
                            double amount = sc.nextDouble();
                            sc.nextLine();
                            walletService.withdraw(userId, amount);
                        }
                    }

                    /* -------- CARDS -------- */
                    else if (menu == 2) {

                        System.out.println("1. Add New Card");
                        System.out.println("2. View Saved Cards");
                        System.out.println("3. Set Default Card");
                        System.out.println("4. Remove Card");

                        int c = sc.nextInt();
                        sc.nextLine();

                        if (c == 1) {
                            System.out.print("Enter Card Number: ");
                            cardDAO.addCard(userId, sc.nextLine());
                        }
                        else if (c == 2) {
                            cardDAO.viewCards(userId);
                        }
                        else if (c == 3) {
                            System.out.print("Enter Card ID: ");
                            cardDAO.setDefaultCard(userId, sc.nextInt());
                            sc.nextLine();
                        }
                        else if (c == 4) {
                            System.out.print("Enter Card ID: ");
                            cardDAO.removeCard(sc.nextInt(), userId);
                            sc.nextLine();
                        }
                    }

                    /* -------- LOANS -------- */
                    else if (menu == 3) {

                        System.out.println("1. Apply Loan");
                        System.out.println("2. View Loans");
                        System.out.println("3. Repay Loan");

                        int lc = sc.nextInt();
                        sc.nextLine();

                        if (lc == 1) {
                            System.out.print("Amount: ");
                            double amt = sc.nextDouble();
                            sc.nextLine();
                            System.out.print("Purpose: ");
                            loanService.applyPersonalLoan(userId, amt, sc.nextLine());
                        }
                        else if (lc == 2) loanService.viewLoans(userId);
                        else if (lc == 3) {
                            System.out.print("Loan ID: ");
                            loanService.repayLoan(sc.nextInt(), sc.nextDouble());
                            sc.nextLine();
                        }
                    }

                    /* -------- TRANSACTIONS -------- */
                    else if (menu == 4) {
                        txDao.viewAllTransactions(userId);
                    }

                    /* -------- NOTIFICATIONS -------- */
                    else if (menu == 5) {
                        notificationService.viewNotifications(userId);
                    }

                    /* -------- MONEY REQUESTS -------- */
                    else if (menu == 6) {

                        System.out.println("1. Send Money Request");
                        System.out.println("2. View Incoming Requests");
                        System.out.println("3. Respond to Request");

                        int mr = sc.nextInt();
                        sc.nextLine();

                        if (mr == 1) {
                            System.out.print("Receiver User ID: ");
                            int rid = sc.nextInt();
                            System.out.print("Amount: ");
                            double amt = sc.nextDouble();
                            sc.nextLine();
                            moneyRequestService.sendRequest(userId, rid, amt);
                        }
                        else if (mr == 2) {
                            moneyRequestService.viewRequests(userId);
                        }
                        else if (mr == 3) {
                            System.out.print("Request ID: ");
                            int reqId = sc.nextInt();
                            System.out.print("Approve? (true/false): ");
                            boolean approve = sc.nextBoolean();
                            sc.nextLine();
                            moneyRequestService.respondToRequest(reqId, approve);
                        }
                    }

                    /* -------- INVOICE -------- */
                    else if (menu == 7) {
                        invoiceService.manageInvoices(userId);
                    }

                    /* -------- LOGOUT -------- */
                    else if (menu == 8) {
                        System.out.println("Signed out successfully");
                        break;
                    }
                }
            }

            /* ================= RECOVER PASSWORD ================= */
            else if (choice == 3) {
                System.out.print("Registered Email: ");
                String email = sc.nextLine();
                System.out.print("Security Answer: ");
                String ans = sc.nextLine();
                System.out.print("New Password: ");
                authService.recoverPassword(email, ans, sc.nextLine());
            }

            /* ================= EXIT ================= */
            else if (choice == 4) {
                System.out.println("Thank you for choosing RevPay");
                sc.close();
                break;
            }
        }
    }

    /* ✅ SESSION TIMEOUT METHOD (FIXED & PRESENT) */
    private static boolean isSessionExpired(long startTime) {
        return System.currentTimeMillis() - startTime > SESSION_TIMEOUT;
    }
}
