package project.controllers;

import project.dao.AccountDAO;
import project.models.Transaction;
import project.models.TypeOfTransaction;

public class Runner {
	private static final AccountDAO accountDAO = new AccountDAO();

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		Transaction transaction = new Transaction(TypeOfTransaction.PAYIN, 1, 1, 1, 1, 100);
		accountDAO.payIn(transaction);
	}
}
