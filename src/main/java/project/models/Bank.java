package project.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Bank {
	private int id;

	private String name;

	private List<User> users;
}
