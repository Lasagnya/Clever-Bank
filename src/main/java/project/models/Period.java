package project.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *  Варианты периодов для выписки
 */

@AllArgsConstructor
@Getter
public enum Period {
	MONTH ("1"), YEAR ("2"), ALL ("3");

	/** поле числовое обозначение */
	private final String number;
}
