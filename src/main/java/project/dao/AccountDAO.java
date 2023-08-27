package project.dao;

import project.models.Account;
import project.models.Transaction;

import java.sql.*;

public class AccountDAO {
	private static final String URL = "jdbc:postgresql://localhost:5432/Clever-Bank";
	private static final String USERNAME = "postgres";
	private static final String PASSWORD = "postgres";
	private static final Connection connection;

	static {
		try {
			Class.forName("org.postgresql.Driver");
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
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance+? where id=?");
			preparedStatement.setDouble(1, transaction.getAmount());
			preparedStatement.setInt(2, transaction.getReceivingAccount());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void withdrawal(Transaction transaction) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance-? where id=?");
			preparedStatement.setDouble(1, transaction.getAmount());
			preparedStatement.setInt(2, transaction.getSendingAccount());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
