package project.controllers;

import project.dao.AccountDAO;
import project.dao.BankDAO;
import project.dao.TransactionDAO;
import project.models.Account;
import project.models.Bank;
import project.models.Transaction;
import project.models.TypeOfTransaction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.util.Date;
import java.util.List;

public class Runner {
	private static final AccountDAO accountDAO = new AccountDAO();
	private static final BankDAO bankDAO = new BankDAO();
	private static final TransactionDAO transactionDAO = new TransactionDAO();

	public static void main(String[] args) throws IOException {
		run();
	}

	public static void run() throws IOException {
		StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));
//		Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, 2, 1, 2, 100);
//		AccountDAO.transfer(transaction);
//		AccountDAO.payIn(transaction);
//		accountDAO.makeCheck(transaction);

		loop:
		while(true) {
			System.out.println();
			System.out.println(
					"Банковская программа\n" +
					"1: перевести средства на другой счёт\n" +
					"3: снять деньги со счёта\n" +
					"4: положить деньги на счёт\n" +
					"0: выйти");
			st.nextToken();

			switch ((int) st.nval) {
				case 1:
					System.out.println("Выберите банк-получатель");
					List<Bank> banks = bankDAO.findAll();
					for (Bank bank : banks)
						System.out.println(bank.getId() + ": " + bank.getName());
					st.nextToken();
					int receivingBank = (int) st.nval;

					System.out.println("Введите номер счёта получателя");
					st.nextToken();
					int receivingAccount = (int) st.nval;
					if (accountDAO.thisBank(receivingBank, receivingAccount))
						continue;
					else {
						while (!accountDAO.thisBank(receivingBank, receivingAccount)) {
							System.out.println("Счёт не найден, попробуйте ещё раз");
							st.nextToken();
							receivingAccount = (int) st.nval;
						}
					}

					System.out.println("С какого счёта вы хотите перевести деньги?");
					st.nextToken();
					int sendingAccount = (int) st.nval;
					//	тут нужно искать счета пользователя

					System.out.println("Введите сумму, которую хотите перевести");
					st.nextToken();
					int amount = (int) st.nval;

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, receivingBank, sendingAccount, receivingAccount, amount);
					accountDAO.transfer(transaction);
					break;

				case 0:
					break loop;

				default:
					break;
			}
		}
	}
}
