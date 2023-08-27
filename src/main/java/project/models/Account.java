package project.models;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class Account {
	private int id;

	private Currency currency;

	private Date opening;

	private double balance;

	private Bank bank;
}
