package project.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Transaction {
	private int id;

	private Date time;

	private TypeOfTransaction typeOfTransaction;

	private Bank sendingBank;

	private Bank receivingBank;

	private Account sendingAccount;

	private Account receivingAccount;

	private double amount;
}
