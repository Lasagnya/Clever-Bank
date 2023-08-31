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
							4: сформировать выписку по счёту
							0: выйти""");

			switch (scanner.nextInt()) {
				case 1: {
					System.out.println("Выберите банк-получатель:");
					int receivingBank = sim.getReceivingBank();
					System.out.println("Введите номер счёта получателя:");
					int receivingAccount = sim.getReceivingAccount(receivingBank);
					System.out.println("С какого счёта вы хотите перевести деньги?");
					Account sendingAccount = sim.getSendingAccount();
					System.out.println("Введите сумму, которую хотите перевести:");
					double amount = sim.getAmount(sendingAccount);

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, receivingBank, sendingAccount.getId(), receivingAccount, amount, Currency.BYN);
					accountDAO.transfer(transaction);
					break;
				}

				case 2: {
					System.out.println("С какого счёта вы хотите снять деньги?");
					Account sendingAccount = sim.getSendingAccount();
					System.out.println("Введите сумму, которую хотите снять:");
					double amount = sim.getAmount(sendingAccount);

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.WITHDRAWAL, 1, 1, sendingAccount.getId(), sendingAccount.getId(), amount, Currency.BYN);
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

					Transaction transaction = new Transaction(new Date(), TypeOfTransaction.PAYIN, 1, receivingBank, 1, receivingAccount, amount, Currency.BYN);
					accountDAO.transfer(transaction);
					break;
				}

				case 4: {
					System.out.println("По какому счёту хотите сформировать выписку?");
					Account account = sim.getSendingAccount();
					System.out.println("""
								За какой период формировать выписку?
								1: за месяц
								2: за год
								3: за всё время""");
					accountDAO.excerpt(account, scanner.nextInt());	// add validator
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
