package project.functions;

import project.dao.AccountDAO;
import project.models.Account;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

/**
 * Класс для проверки потребности начисления процентов
 */

public class IsPercentsNeeded implements Runnable {
	private static final String DRIVER;
	private static final String URL;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static final Connection connection;
	private static final Properties properties;
	AccountDAO accountDAO = new AccountDAO();

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

	@Override
	public void run() {
		List<Account> accounts = accountDAO.findByBank(1);
		accounts.forEach(account -> account.setPercents(account.getBalance() > 0));
		try {
			PreparedStatement statement = connection.prepareStatement("update account set account_is_percents=? where account_id=?");
			for (Account account : accounts) {
				statement.setBoolean(1, account.isPercents());
				statement.setInt(2, account.getId());
				statement.addBatch();
			}
			statement.executeBatch();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
