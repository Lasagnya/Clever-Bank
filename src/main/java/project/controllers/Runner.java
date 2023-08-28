package project.controllers;

import project.dao.TransactionDAO;
import project.models.Transaction;
import project.models.TypeOfTransaction;

import java.util.Date;

public class Runner {
	private static final TransactionDAO transactionDAO = new TransactionDAO();

	public static void main(String[] args) {
		run();
	}

	public static void run() {
		Transaction transaction = new Transaction(new Date(), TypeOfTransaction.TRANSFER, 1, 2, 2, 3, 100);
		transactionDAO.payIn(transaction);
//		accountDAO.makeCheck(transaction);
	}
}
