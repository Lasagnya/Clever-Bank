package project.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

/**
 *  Класс счёта
 */


@Getter
@Setter
@NoArgsConstructor
public class Account {
	/** поле id */
	private int id;

	/** поле валюта */
	private Currency currency;

	/** поле дата открытия */
	private Date opening;

	/** поле баланс */
	private double balance = 0.0;

	/** поле id банка */
	private int bank;

	/** поле id пользователя */
	private int user;

	/** поле нужно ли начислять проценты */
	private boolean isPercents = false;

	/**
	 *
	 * @param currency валюта счёта
	 * @param opening дата открытия
	 * @param bank банк-владелец
	 * @param user пользователь-владелец
	 */
	public Account(Currency currency, Date opening, int bank, int user) {
		this.currency = currency;
		this.opening = opening;
		this.bank = bank;
		this.user = user;
	}
}
