package project.models;

import lombok.Getter;
import lombok.Setter;

/**
 *  Класс пользователя
 */

@Getter
@Setter
public class User {
	/** id пользователя */
	private int id;

	/** имя пользователя */
	private String name;

	/** пароль пользователя */
	private String password;

	/** id банка-владельца */
	private int bank = 1;
}
