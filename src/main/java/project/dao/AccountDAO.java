package project.dao;

import project.models.Account;
import project.models.Currency;
import project.models.Transaction;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
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
