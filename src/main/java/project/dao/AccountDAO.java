package project.dao;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import project.models.*;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  Класс методов для сущности Account. Взаимодействует
 *  с таблицей account.
 */

public class AccountDAO {
	private static final String DRIVER;
	private static final String URL;
	private static final String USERNAME;
	private static final String PASSWORD;
	private static final Connection connection;
	private static final Properties properties;
	private static final TransactionDAO transactionDAO = new TransactionDAO();
	/** мэп, сопоставляющий id аккаунта с ReentrantLock */
	private final ConcurrentMap<Integer, ReentrantLock> accountLocks = new ConcurrentHashMap<>();
	/** мэп, сопоставляющий id банка с ReentrantLock */
	private final ConcurrentMap<Integer, ReentrantLock> bankLocks = new ConcurrentHashMap<>();

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
	 * Пополнение счёта
	 * @param transaction исполняемая транзация
	 */
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

	/**
	 * Снятие средств со счёта
	 * @param transaction исполняемая транзакия
	 */
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

	/**
	 * Перевод средств между счетами
	 * @param transaction исполняемая транзакия
	 */
	public void transfer(Transaction transaction) {
		if (transaction.getReceivingBank() != 1) {
			ReentrantLock accountLock = accountLocks.computeIfAbsent(transaction.getSendingAccount(), k -> new ReentrantLock());
			ReentrantLock bankLock = bankLocks.computeIfAbsent(transaction.getReceivingBank(), k -> new ReentrantLock());
			takeLocks(accountLock, bankLock);
			try {
				payIn(transaction);
				withdrawal(transaction);
			} finally {
				accountLock.unlock();
				bankLock.unlock();
			}
		} else {
			payIn(transaction);
			withdrawal(transaction);
		}
	}

	/**
	 * Взятие двух ReentrantLock во избежание блокировки
	 * @param lock1
	 * @param lock2
	 */
	private void takeLocks(ReentrantLock lock1, ReentrantLock lock2) {
		boolean firstLockTaken = false;
		boolean secondLockTaken = false;
		while (true) {
			try {
				firstLockTaken = lock1.tryLock();
				secondLockTaken = lock2.tryLock();
			} finally {
				if (firstLockTaken && secondLockTaken)
					return;
				if (firstLockTaken)
					lock1.unlock();
				if (secondLockTaken)
					lock2.unlock();
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Получение сущности счёт
	 * @param id id счёта
	 * @return возвращает счёт, если он найден по id, иначе empty
	 */
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
				account.setPercents(rs.getBoolean("account_is_percents"));
				return Optional.of(account);
			}
			else return Optional.empty();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Поиск всех счетов, принадлежащих банку с id
	 * @param id id банка-владельца
	 * @return возвращает список найденых счетов
	 */
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

	/**
	 * Проверяет, приндлежит ли счёт с id accountId банку с id bankId
	 * @param bankId id банка для выяснения принадлежности
	 * @param accountId id счёта для выяснения принадлежности
	 * @return true, если принадлежит; false, если не принадлежит
	 */
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

	/**
	 * Поиск счетов по их принадлежности пользователю с id
	 * @param id id пользователя
	 * @return список найденных счетов
	 */
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

	/**
	 * Сохранение нового счёта в базу данных
	 * @param account новый счёт
	 */
	public void save(Account account) {
		try {
			PreparedStatement preparedStatement = connection.prepareStatement("insert into account(currency, opening, account_bank_id, account_user_id) VALUES (?, ?, ?, ?)");
			preparedStatement.setString(1, account.getCurrency().toString());
			preparedStatement.setDate(2, new java.sql.Date(account.getOpening().getTime()));
			preparedStatement.setInt(3, account.getBank());
			preparedStatement.setInt(4, account.getUser());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Создание выписки по счёту в txt файл
	 * @param account по этому счёту осуществляется выписка
	 * @param period выписка по этому периоду
	 */
	public void excerpt(Account account, Period period) {
		List<Transaction> transactions = getTransactions(account, period);

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
			if (period == Period.MONTH) {
				Date startDate = Date.from(LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				bw.write(String.format(" %-26s| %-37s\n", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == Period.YEAR) {
				Date startDate = Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				bw.write(String.format(" %-26s| %-37s\n", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == Period.ALL)
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

	/**
	 * Создание выписки по счёту в pdf файл
	 * @param account по этому счёту осуществляется выписка
	 * @param period выписка по этому периоду
	 */
	public void excerptInPDF(Account account, Period period) {
		List<Transaction> transactions = getTransactions(account, period);

		try {
			Files.createDirectories(Path.of("excerpt"));
			PDDocument document = new PDDocument();
			PDPage page = new PDPage();
			document.addPage(page);

			PDPageContentStream contentStream = new PDPageContentStream(document, page);

			PDFont font = PDType0Font.load(document, new File("fonts/CascadiaMono-SemiLight.ttf"));
			contentStream.beginText();
			contentStream.setFont(font, 12);
			contentStream.setLeading(14.5f);
			contentStream.newLineAtOffset(25, 750);
			String title = "Выписка";
			String output = String.format("%30s%30s", "#", "").replace("#", title);
			contentStream.showText(output);
			contentStream.newLine();
			output = String.format("%28s%28s", "#", "").replace("#", "Clever-Bank");
			contentStream.showText(output);
			contentStream.newLine();
			contentStream.showText(String.format(" %-26s| %-37s", "Клиент", UserDAO.getUser().getName()));
			contentStream.newLine();
			contentStream.showText(String.format(" %-26s| %-37s", "Счёт", account.getId()));
			contentStream.newLine();
			contentStream.showText(String.format(" %-26s| %-37s", "Валюта", account.getCurrency().toString()));
			contentStream.newLine();
			DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyy");
			DateFormat timeFormat = new SimpleDateFormat("dd.MM.yyy, HH:mm");
			contentStream.showText(String.format(" %-26s| %-37s", "Дата открытия", dateFormat.format(account.getOpening().getTime())));
			contentStream.newLine();
			if (period == Period.MONTH) {
				Date startDate = Date.from(LocalDate.now().minusMonths(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				contentStream.showText(String.format(" %-26s| %-37s", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == Period.YEAR) {
				Date startDate = Date.from(LocalDate.now().minusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
				contentStream.showText(String.format(" %-26s| %-37s", "Период выписки", dateFormat.format(startDate) + " - " + dateFormat.format(new Date())));
			}
			if (period == Period.ALL)
				contentStream.showText(String.format(" %-26s| %-37s", "Период выписки", "За всё время"));
			contentStream.newLine();
			contentStream.showText(String.format(" %-26s| %-37s", "Дата и время формирования", timeFormat.format(new Date().getTime())));
			contentStream.newLine();
			contentStream.showText(String.format(" %-26s| %-37s", "Остаток", account.getBalance()));
			contentStream.newLine();
			contentStream.showText(String.format(" %-12s| %-33s | %-15s", "   Дата", "           Примечание", "    Сумма"));
			contentStream.newLine();
			contentStream.showText(String.format("%66s", "").replace(" ", "-"));
			contentStream.newLine();
			int i = 1;
			for (Transaction transaction : transactions) {
				if (i == 41) {
					contentStream.endText();
					contentStream.close();
					page = new PDPage();
					document.addPage(page);
					contentStream = new PDPageContentStream(document, page);
					font = PDType0Font.load(document, new File("fonts/CascadiaMono-SemiLight.ttf"));
					contentStream.beginText();
					contentStream.setFont(font, 12);
					contentStream.setLeading(14.5f);
					contentStream.newLineAtOffset(25, 750);
					i = 1;
				}

				String amount = transaction.getAmount() + " " + transaction.getCurrency().toString();
				if (transaction.getTypeOfTransaction().equals(TypeOfTransaction.WITHDRAWAL) ||
						(transaction.getTypeOfTransaction().equals(TypeOfTransaction.TRANSFER) &&
								(transaction.getReceivingAccount() != account.getId() || transaction.getReceivingBank() != account.getBank()))) {
					amount = "-" + amount;
				}
				contentStream.showText(String.format(" %-12s| %-33s | %-15s",
						dateFormat.format(transaction.getTime().getTime()),
						transaction.getTypeOfTransaction().getTitle(),
						amount));
				contentStream.newLine();
				i++;
			}
			contentStream.endText();
			contentStream.close();

			document.save(String.format("excerpt/excerpt%d.pdf", account.getId()));
			document.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Создание списка счетов по выводу базы данных
	 * @param rs переданный ResultSet
	 * @return список полученных счетов
	 */
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
				account.setPercents(rs.getBoolean("account_is_percents"));
				accounts.add(account);
			}
			return accounts;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Создание списка транзакций, связанных со счётом
	 * @param account ищутся транзакии, связанные с этим счётом
	 * @param period период, за который ищутся транзакции
	 * @return список найденных транзакций
	 */
	private List<Transaction> getTransactions(Account account, Period period) {
		List<Transaction> transactions = new ArrayList<>();
		try {
			String SQL = "select * from transaction" +
					" where " +
					"((sending_bank=1 and sending_account=?)" +
					" or " +
					"(receiving_bank=1 and receiving_account=?))" +
					" order by " +
					"execution_time desc";
			if (period == Period.MONTH)
				SQL = SQL + " and execution_time>now()-'1 month'::interval";
			else if (period == Period.YEAR)
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
		return transactions;
	}
}
