package project.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Transaction {
	private int id;

	private Date time;

	private TypeOfTransaction typeOfTransaction;

	private int sendingBank;

	private int receivingBank;

	private int sendingAccount;

	private int receivingAccount;

	private double amount;

	public Transaction(TypeOfTransaction typeOfTransaction, int sendingBank, int receivingBank, int sendingAccount, int receivingAccount, double amount) {
		this.typeOfTransaction = typeOfTransaction;
		this.sendingBank = sendingBank;
		this.receivingBank = receivingBank;
		this.sendingAccount = sendingAccount;
		this.receivingAccount = receivingAccount;
		this.amount = amount;
	}
}
