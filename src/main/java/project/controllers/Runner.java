package project.controllers;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import project.dao.AccountDAO;
import project.dao.BankDAO;
import project.dao.TransactionDAO;
import project.dao.UserDAO;
import project.models.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Runner {
	private static final AccountDAO accountDAO = new AccountDAO();
	private static final BankDAO bankDAO = new BankDAO();
	private static final TransactionDAO transactionDAO = new TransactionDAO();
	private static final UserDAO userDAO = new UserDAO();
	private static User user;

	public static void main(String[] args) throws IOException {
		run();
	}

	public static void run() throws IOException {
		StreamTokenizer st = new StreamTokenizer(new BufferedReader(new InputStreamReader(System.in)));
//		Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, 2, 1, 2, 100);
//		AccountDAO.transfer(transaction);
//		AccountDAO.payIn(transaction);
//		accountDAO.makeCheck(transaction);



		System.out.println("Необходимо войти в аккаунт\n" +
				"Введите имя пользователя:");
		st.nextToken();
		String name = st.sval;

		username:
		while (!userDAO.findByName(name).isPresent()) {
			System.out.println("Такого пользователя не существует. Хотите создать?\n" +
					"1: создать пользователя\n" +
					"2: ввести имя ещё раз");

			st.nextToken();
			switch ((int) st.nval) {
				case 1:
					user = new User();
					user.setName(name);
					byte[] password;
					byte[] password2;
					do {
						System.out.println("Введите пароль");
						st.nextToken();
						password = st.sval.getBytes(StandardCharsets.UTF_8);
						System.out.println("Введите пароль ещё раз");
						st.nextToken();
						password2 = st.sval.getBytes(StandardCharsets.UTF_8);
						if (Arrays.equals(password2, password)) {
							break;
						} else System.out.println("Пароли не совпадают, попробуйте ещё раз!");
					} while (true);
					Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);
					String hash = argon2.hash(22, 65536, 1, password);
					user.setPassword(hash);
					userDAO.save(user);
					break username;

				case 2:
					break;

				default:
					break;
			}

			System.out.println("Введите имя пользователя");
			st.nextToken();
			name = st.sval;
		}
		user = userDAO.findByName(name).get();

		System.out.println("Введите пароль");
		st.nextToken();
		byte[] password = st.sval.getBytes(StandardCharsets.UTF_8);
		Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);
//		String hash = argon2.hash(22, 65536, 1, password);
		while (!argon2.verify(user.getPassword(), password)) {
			System.out.println("Неверный пароль, попробуйте ещё раз!");
			st.nextToken();
			password = st.sval.getBytes(StandardCharsets.UTF_8);
		}

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
					while (!accountDAO.thisBank(receivingBank, receivingAccount)) {
						System.out.println("Счёт не найден, попробуйте ещё раз");
						st.nextToken();
						receivingAccount = (int) st.nval;
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
