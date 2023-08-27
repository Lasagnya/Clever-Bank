package project.controllers;

import project.dao.AccountDAO;
import project.models.Transaction;
import project.models.TypeOfTransaction;

import java.util.Date;

public class Runner {
	private static final AccountDAO accountDAO = new AccountDAO();

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, 2, 2, 3, 100);
		accountDAO.payIn(transaction);
	}
}
