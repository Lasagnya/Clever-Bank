package project.dao;

import project.models.User;

import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Optional;
import java.util.Properties;

public class UserDAO {
	private static final String DRIVER;
	private static final String URL;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static final Connection connection;
	private static final Properties properties;

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

	public Optional<User> findByName(String name) {
		User user = new User();
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("select * from public.user where user_name=? and user_bank_id=1");
			preparedStatement.setString(1, name);
			ResultSet rs = preparedStatement.executeQuery();
			if (rs.next()) {
				user.setId(rs.getInt("user_id"));
				user.setName(rs.getString("user_name"));
				user.setPassword(rs.getString("user_password"));
				user.setBank(rs.getInt("user_bank_id"));
				return Optional.of(user);
			}
			else return Optional.empty();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void save(User user) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("insert into public.user(user_name, user_bank_id, user_password) VALUES (?, ?, ?)");
			preparedStatement.setString(1, user.getName());
			preparedStatement.setInt(2, user.getBank());
			preparedStatement.setString(3, user.getPassword());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
