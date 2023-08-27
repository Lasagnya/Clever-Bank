package project.dao;

import project.models.Account;
import project.models.Transaction;
import project.models.TypeOfTransaction;

import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

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
			PreparedStatement preparedStatement = connection.prepareStatement("update account set balance=balance+? where account_id=?");
			preparedStatement.setDouble(1, transaction.getAmount());
			preparedStatement.setInt(2, transaction.getReceivingAccount());
			preparedStatement.executeUpdate();
			saveTransaction(transaction);
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
			saveTransaction(transaction);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void saveTransaction(Transaction transaction) {
		try {
			PreparedStatement preparedStatement1 = connection.prepareStatement(
					"insert into transaction(execution_time, type_of_transaction, sending_bank, receiving_bank, sending_account, receiving_account, amount) values(?, ?, ?, ?, ?, ?, ?)");
			preparedStatement1.setTimestamp(1, new Timestamp(transaction.getTime().getTime()));
			preparedStatement1.setString(2, transaction.getTypeOfTransaction().toString());
			preparedStatement1.setInt(3, transaction.getSendingBank());
			preparedStatement1.setInt(4, transaction.getReceivingBank());
			preparedStatement1.setInt(5, transaction.getSendingAccount());
			preparedStatement1.setInt(6, transaction.getReceivingAccount());
			preparedStatement1.setDouble(7, transaction.getAmount());
			preparedStatement1.executeUpdate();
			makeCheck(transaction);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public void makeCheck(Transaction transaction) {
		int id;
		String sendingBankName = "";
		String receivingBankName = "";
		int sendingAccountId = 0;
		int receivingAccountId = 0;
		try {
			PreparedStatement preparedStatement1 = connection.prepareStatement(
					"select * from transaction " +
							"join bank on sending_bank=bank.bank_id or receiving_bank=bank.bank_id " +
							"join account on sending_account=account.account_id or receiving_account=account.account_id " +
							"where " +
							"type_of_transaction=? and sending_bank=? and receiving_bank=? and sending_account=? and receiving_account=? and amount=? and execution_time=?");
			preparedStatement1.setString(1, transaction.getTypeOfTransaction().toString());
			preparedStatement1.setInt(2, transaction.getSendingBank());
			preparedStatement1.setInt(3, transaction.getReceivingBank());
			preparedStatement1.setInt(4, transaction.getSendingAccount());
			preparedStatement1.setInt(5, transaction.getReceivingAccount());
			preparedStatement1.setDouble(6, transaction.getAmount());
			preparedStatement1.setTimestamp(7, new Timestamp(transaction.getTime().getTime()));
			ResultSet resultSet = preparedStatement1.executeQuery();
			resultSet.next();
			id = resultSet.getInt("transaction_id");
			sendingBankName = resultSet.getString("bank_name");
			sendingAccountId = resultSet.getInt("account_id");
			while (resultSet.next()) {
				receivingBankName = resultSet.getString("bank_name");
				receivingAccountId = resultSet.getInt("account_id");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyy");
		DateFormat timeFormat = new SimpleDateFormat("hh:mm:ss");
		System.out.println(String.format("%40s", "").replace(" ", "-"));
		String title = "Банковский чек";
		String output = String.format("|%13s%12s|", "#", "").replace("#", title);
		System.out.println(output);
		System.out.printf("%-20s%21s", "| Чек:", String.format("%d |\n", id));
		System.out.printf("%-20s%21s", "| " + dateFormat.format(transaction.getTime().getTime()), timeFormat.format(transaction.getTime().getTime()) + " |\n");
		System.out.printf("%-20s%21s", "| Тип транзакции:", transaction.getTypeOfTransaction().getTitle() + " |\n");
		System.out.printf("%-20s%21s", "| Банк отправителя:", String.format("%s |\n", sendingBankName));
		if (transaction.getTypeOfTransaction() == TypeOfTransaction.TRANSFER)
			System.out.printf("%-20s%21s", "| Банк получателя:", String.format("%s |\n", receivingBankName));
		System.out.printf("%-20s%21s", "| Счёт отправителя:", String.format("%s |\n", sendingAccountId));
		if (transaction.getTypeOfTransaction() == TypeOfTransaction.TRANSFER)
			System.out.printf("%-20s%21s", "| Счёт отправителя:", String.format("%s |\n", receivingAccountId));
		System.out.printf("%-20s%21s", "| Сумма:", String.format("%s |\n", transaction.getAmount()));
		System.out.println(String.format("%40s", "").replace(" ", "-"));
	}
}
