package project.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *  Класс банка
 */

@Getter
@Setter
public class Bank {
	/** поле id */
	private int id;

	/** поле название */
	private String name;

	/** поле пользователи банка */
	private List<Integer> users;
}
