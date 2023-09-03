package project.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class Account {
	private int id;

	private Currency currency;

	private Date opening;

	private double balance = 0.0;

	private int bank;

	private int user;

	private boolean isPercents = false;

	public Account(Currency currency, Date opening, int bank, int user) {
		this.currency = currency;
		this.opening = opening;
		this.bank = bank;
		this.user = user;
	}
}
