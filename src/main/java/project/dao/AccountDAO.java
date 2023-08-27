package project.dao;

import project.models.Account;

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

	public void payInTransaction(Account account, double amount) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance+? where id=?");
			preparedStatement.setDouble(1, amount);
			preparedStatement.setInt(2, account.getId());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
