package project.dao;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.Getter;
import project.models.User;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.Properties;
import java.util.Scanner;

public class UserDAO {
	private static final String DRIVER;
	private static final String URL;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static final Connection connection;
	private static final Properties properties;
	@Getter
	private static User user;

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

	/**
	 * Определение локального пользователя
	 * @param user пользователь с аутентификационными данными
	 */
	public static void defineUser(User user) {
		UserDAO.user = user;
	}

	/**
	 * Поиск пользователя в базе данных по имени
	 * @param name имя, по которому ищется пользователь
	 * @return найденный пользователь или empty, если не найден
	 */
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

	/**
	 * Сохранение нового пользователя в базу данных
	 * @param user пользователь для сохранения
	 */
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

	/**
	 * Получение шифрованного пароля
	 * @return строка с шифрованным паролем
	 */
	private String getPasswordHash() {
		Scanner scanner = new Scanner(System.in);
		byte[] password;
		byte[] password2;
		do {
			System.out.println("Введите пароль:");
			password = scanner.next().getBytes(StandardCharsets.UTF_8);
			System.out.println("Введите пароль ещё раз:");
			password2 = scanner.next().getBytes(StandardCharsets.UTF_8);
			if (Arrays.equals(password2, password)) {
				break;
			} else System.out.println("Пароли не совпадают, попробуйте ещё раз!");
		} while (true);
		Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);
		String hash = argon2.hash(22, 65536, 1, password);
		argon2.wipeArray(password);
		argon2.wipeArray(password2);
		return hash;
	}

	/**
	 * В методе происходит аутентификация пользователя. Человек вводит имя пользователя,
	 * и, если такого не существует, ему предлагается создать нового с таким именем или ввести ещё раз.
	 * Далее происходит ввод пароля.
	 */
	public void authentication() {
		Scanner scanner = new Scanner(System.in);
		User user;
		System.out.println("Необходимо войти в аккаунт.\n" +
				"Введите имя пользователя:");
		String name = scanner.next();

		while (findByName(name).isEmpty()) {
			System.out.println("""
					Такого пользователя не существует. Хотите создать?
					1: создать пользователя
					2: ввести имя ещё раз""");

			if (scanner.nextInt() == 1) {
				user = new User();
				user.setName(name);
				user.setPassword(getPasswordHash());
				save(user);
			}

			System.out.println("Необходимо войти в аккаунт\n" +
					"Введите имя пользователя:");
			name = scanner.next();
		}
		user = findByName(name).get();

		System.out.println("Введите пароль:");
		byte[] password = scanner.next().getBytes(StandardCharsets.UTF_8);
		Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id, 16, 32);
		while (!argon2.verify(user.getPassword(), password)) {
			System.out.println("Неверный пароль, попробуйте ещё раз!");
			password = scanner.next().getBytes(StandardCharsets.UTF_8);
		}
		argon2.wipeArray(password);
		defineUser(user);
	}

	/**
	 * Обновление записи о пользователе в базе данных.
	 * @param updatedUser обновлённый пользователь
	 */
	public void update(User updatedUser) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("update public.user set user_name=?, user_password=?, user_bank_id=? where user_id=?");
			preparedStatement.setString(1, updatedUser.getName());
			preparedStatement.setString(2, updatedUser.getPassword());
			preparedStatement.setInt(3, updatedUser.getBank());
			preparedStatement.setInt(4, updatedUser.getId());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Изменяет пароль у локального пользователя и обновляет базу данных
	 */
	public void changePassword() {
		user.setPassword(getPasswordHash());
		update(user);
		System.out.println("Пароль изменён!");
	}

	/**
	 * Обновляет имя у локального пользователя и обновляет базу данных
	 */
	public void changeUsername() {
		Scanner scanner = new Scanner(System.in);
		System.out.println("Введите новое имя пользователя:");
		String name = scanner.next();
		user.setName(name);
		update(user);
		System.out.println("Имя изменено на " + name + ".");
	}
}
