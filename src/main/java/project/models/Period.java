package project.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Period {
	MONTH ("1"), YEAR ("2"), ALL ("3");

	private final String number;
}
