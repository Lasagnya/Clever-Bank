package project.functions;

import project.dao.AccountDAO;
import project.dao.BankDAO;
import project.dao.UserDAO;
import project.models.Account;
import project.models.Bank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.List;
import java.util.Scanner;

public class SwitchInputMethods {
	private static final AccountDAO accountDAO = new AccountDAO();
	private static final BankDAO bankDAO = new BankDAO();
	Scanner scanner = new Scanner(System.in);

	public int getReceivingBank() {
		List<Bank> banks = bankDAO.findAll();
		for (Bank bank : banks)
			System.out.println(bank.getId() + ": " + bank.getName());
		return scanner.nextInt();
	}

	public int getReceivingAccount(int receivingBank) {
		int receivingAccount = scanner.nextInt();
		while (!accountDAO.thisBank(receivingBank, receivingAccount)) {
			System.out.println("Счёт не найден, попробуйте ещё раз.");
			receivingAccount = scanner.nextInt();
		}
		return receivingAccount;
	}

	public int getSendingAccount() {
		StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));
		List<Account> accounts = accountDAO.findByUser(UserDAO.getUser().getId());
		for (Account account : accounts)
			System.out.println("Номер счёта: " + account.getId() +
					", " + account.getBalance() + " " + account.getCurrency().toString());
		try {
			st.nextToken();
			while (accounts.stream().noneMatch(account -> account.getId() == (int) st.nval)) {
				System.out.println("Счёт с таким номером не найден, попробуйте ещё раз.");
				st.nextToken();
			}
			return (int) st.nval;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public double getAmount(int sendingAccount) {
		List<Account> accounts = accountDAO.findByUser(UserDAO.getUser().getId());
		double amount = scanner.nextDouble();
		double balance = accounts.stream().filter(account -> account.getId() == sendingAccount).findAny().get().getBalance();
		while (amount > balance) {
			System.out.println("Недостаточно средств! Введите другую сумму.");
			amount = scanner.nextDouble();
		}
		return amount;
	}
}
