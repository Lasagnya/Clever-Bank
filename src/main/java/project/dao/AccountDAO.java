package project.dao;

import project.models.Account;
import project.models.Currency;
import project.models.Transaction;
import project.models.TypeOfTransaction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class AccountDAO {
	private static final String DRIVER;
	private static final String URL;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static final Connection connection;
	private static final Properties properties;
	private static final TransactionDAO transactionDAO = new TransactionDAO();

	static {
		try {
			properties = new Properties();
			FileReader reader = new FileReader("src/main/resources/configuration.yml");
			properties.load(reader);
			DRIVER = properties.getProperty("driver");
			URL = properties.getProperty("url");
			USERNAME = properties.getProperty("username");
			PASSWORD = properties.getProperty("password");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}

		try {
			connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void payIn(Transaction transaction) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance+? where account_id=?");
			preparedStatement.setDouble(1, transaction.getAmount());
			preparedStatement.setInt(2, transaction.getReceivingAccount());
			preparedStatement.executeUpdate();
			transactionDAO.saveTransaction(transaction);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void withdrawal(Transaction transaction) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance-? where account_id=?");
			preparedStatement.setDouble(1, transaction.getAmount());
			preparedStatement.setInt(2, transaction.getSendingAccount());
			preparedStatement.executeUpdate();
			transactionDAO.saveTransaction(transaction);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void transfer(Transaction transaction) {
		payIn(transaction);
		withdrawal(transaction);
	}

	public Optional<Account> findById(int id) {
		Account account = new Account();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from account where account_id=?");
			preparedStatement.setInt(1, id);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				account.setId(rs.getInt("account_id"));
				account.setCurrency(Currency.valueOf(rs.getString("currency")));
				account.setOpening(rs.getDate("opening"));
				account.setBalance(rs.getDouble("balance"));
				account.setBank(rs.getInt("account_bank_id"));
				account.setUser(rs.getInt("account_user_id"));
				return Optional.of(account);
			}
			else return Optional.empty();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Account> findByBank(int id) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from account where account_bank_id=?");
			preparedStatement.setInt(1, id);
			ResultSet rs = preparedStatement.executeQuery();
			return makeList(rs);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean thisBank(int bankId, int accountId) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from account where account_bank_id=? and account_id=?");
			preparedStatement.setInt(1, bankId);
			preparedStatement.setInt(2, accountId);
			return preparedStatement.executeQuery().next();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public List<Account> findByUser(int id) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from account where account_user_id=? and account_bank_id=1");
			preparedStatement.setInt(1, id);
			ResultSet rs = preparedStatement.executeQuery();
			return makeList(rs);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void excerpt(Account account, int period) {		// 1 - месяц, 2 - год, 3 - весь период
		List<Transaction> transactions = new ArrayList<>();
		try {
			String SQL = "select * from transaction" +
					" where " +
					"((sending_bank=1 and sending_account=?)" +
					" or " +
					"(receiving_bank=1 and receiving_account=?))";
			if (period == 1)
				SQL = SQL + " and execution_time>now()-'1 month'::interval";
			else if (period == 2)
				SQL = SQL + " and execution_time>now()-'1 year'::interval";
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);
			preparedStatement.setInt(1, account.getId());
			preparedStatement.setInt(2, account.getId());

			ResultSet rs = preparedStatement.executeQuery();
			while (rs.next()) {
				Transaction transaction = new Transaction();
				transaction.setId(rs.getInt("transaction_id"));
				transaction.setTime(rs.getTimestamp("execution_time"));
				transaction.setTypeOfTransaction(TypeOfTransaction.valueOf(rs.getString("type_of_transaction")));
				transaction.setSendingBank(rs.getInt("sending_bank"));
				transaction.setReceivingBank(rs.getInt("receiving_bank"));
				transaction.setSendingAccount(rs.getInt("sending_account"));
				transaction.setReceivingAccount(rs.getInt("receiving_account"));
				transaction.setAmount(rs.getDouble("amount"));
				transaction.setCurrency(Currency.valueOf(rs.getString("transaction_currency")));
				transactions.add(transaction);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		try {
			Files.createDirectories(Path.of("excerpt"));
			FileWriter fw = new FileWriter(String.format("excerpt/excerpt%d.txt", account.getId()));
			BufferedWriter bw = new BufferedWriter(fw);
			String title = "Выписка";
			String output = String.format("%30s%30s\n", "#", "").replace("#", title);
			bw.write(output);
			output = String.format("%28s%28s\n", "#", "").replace("#", "Clever-Bank");
			bw.write(output);
			bw.write(String.format(" %-26s| %-37s\n", "Клиент", UserDAO.getUser().getName()));
			bw.write(String.format(" %-26s| %-37s\n", "Счёт", account.getId()));
			bw.write(String.format(" %-26s| %-37s\n", "Валюта", account.getCurrency().toString()));
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy");
			DateFormat timeFormat = new SimpleDateFormat("dd.MM.yyy, HH:mm");
			bw.write(String.format(" %-26s| %-37s\n", "Дата открытия", dateFormat.format(account.getOpening().getTime())));
			if (period == 1) {
				Date startDate = Date.from(LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				bw.write(String.format(" %-26s| %-37s\n", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == 2) {
				Date startDate = Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				bw.write(String.format(" %-26s| %-37s\n", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == 3)
				bw.write(String.format(" %-26s| %-37s\n", "Период выписки", "За всё время"));
			bw.write(String.format(" %-26s| %-37s\n", "Дата и время формирования", timeFormat.format(new Date().getTime())));
			bw.write(String.format(" %-26s| %-37s\n", "Остаток", account.getBalance()));
			bw.write(String.format(" %-12s| %-33s | %-15s\n", "   Дата", "           Примечание", "    Сумма"));
			bw.write(String.format("%66s\n", "").replace(" ", "-"));
			for (Transaction transaction : transactions) {
				String amount = transaction.getAmount() + " " + transaction.getCurrency().toString();
				if (transaction.getTypeOfTransaction().equals(TypeOfTransaction.WITHDRAWAL) ||
						(transaction.getTypeOfTransaction().equals(TypeOfTransaction.TRANSFER) &&
								(transaction.getReceivingAccount() != account.getId() || transaction.getReceivingBank() != account.getBank()))) {
					amount = "-" + amount;
				}
				bw.write(String.format(" %-12s| %-33s | %-15s\n",
						dateFormat.format(transaction.getTime().getTime()),
						transaction.getTypeOfTransaction().getTitle(),
						amount));
			}
			bw.close();
			fw.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Account> makeList(ResultSet rs) {
		List<Account> accounts = new ArrayList<>();
		try {
			while (rs.next()) {
				Account account = new Account();
				account.setId(rs.getInt("account_id"));
				account.setCurrency(Currency.valueOf(rs.getString("currency")));
				account.setOpening(rs.getDate("opening"));
				account.setBalance(rs.getInt("balance"));
				account.setBank(rs.getInt("account_bank_id"));
				account.setUser(rs.getInt("account_user_id"));
				accounts.add(account);
			}
			return accounts;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
