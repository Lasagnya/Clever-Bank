package project.controllers;

import project.dao.AccountDAO;
import project.dao.UserDAO;
import project.functions.SwitchInputMethods;
import project.models.*;

import java.util.Date;
import java.util.Scanner;

public class Runner {
	private static final AccountDAO accountDAO = new AccountDAO();
	private static final UserDAO userDAO = new UserDAO();
	private static final SwitchInputMethods sim = new SwitchInputMethods();

	public static void main(String[] args) {
		run();
	}

	public static void run(){
		Scanner scanner = new Scanner(System.in);

		userDAO.authentication();

		loop:
		while(true) {
			System.out.println();
			System.out.println(
					"""
							Банковская программа
							1: перевести средства на другой счёт
							2: снять деньги со счёта
							3: положить деньги на счёт
							0: выйти""");

			switch (scanner.nextInt()) {
				case 1: {
					System.out.println("Выберите банк-получатель:");
					int receivingBank = sim.getReceivingBank();
					System.out.println("Введите номер счёта получателя:");
					int receivingAccount = sim.getReceivingAccount(receivingBank);
					System.out.println("С какого счёта вы хотите перевести деньги?");
					int sendingAccount = sim.getSendingAccount();
					System.out.println("Введите сумму, которую хотите перевести:");
					double amount = sim.getAmount(sendingAccount);

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, receivingBank, sendingAccount, receivingAccount, amount);
					accountDAO.transfer(transaction);
					break;
				}

				case 2: {
					System.out.println("С какого счёта вы хотите снять деньги?");
					int sendingAccount = sim.getSendingAccount();
					System.out.println("Введите сумму, которую хотите снять:");
					double amount = sim.getAmount(sendingAccount);

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, 1, sendingAccount, sendingAccount, amount);
					accountDAO.withdrawal(transaction);
					break;
				}

				case 3: {
					System.out.println("Выберите банк-получатель:");
					int receivingBank = sim.getReceivingBank();
					System.out.println("Введите номер счёта получателя:");
					int receivingAccount = sim.getReceivingAccount(receivingBank);
					System.out.println("Введите сумму, которую хотите перевести:");
					double amount = scanner.nextDouble();

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, receivingBank, 1, receivingAccount, amount);
					accountDAO.transfer(transaction);
					break;
				}

				case 0:
					break loop;

				default:
					break;
			}
		}
	}
}
