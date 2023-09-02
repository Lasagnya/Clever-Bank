package project.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TypeOfTransaction {
	TRANSFER ("Перевод"), WITHDRAWAL ("Снятие"), PAYIN ("Пополнение");

	private final String title;
}
