create table bank(
                     bank_id integer generated by default as identity primary key ,
                     bank_name varchar not null
);

create table "user"(
                       user_id integer generated by default as identity primary key ,
                       user_name varchar not null,
                       user_password char(98) not null,
                       user_bank_id integer references bank(bank_id) on delete cascade
);
alter table "user" add constraint user_unique unique (user_name, user_bank_id);

create table account(
                        account_id integer generated by default as identity primary key ,
                        currency varchar(3) not null ,
                        opening date not null ,
                        balance numeric(15, 2) default 0.0,
                        account_bank_id integer references bank(bank_id) on delete cascade,
                        account_user_id integer references "user"(user_id) on delete cascade,
                        account_is_percents bool default false
);

create table transaction(
                            transaction_id integer generated by default as identity primary key ,
                            execution_time timestamp not null ,
                            type_of_transaction char(3),
                            sending_bank integer references bank(bank_id),
                            receiving_bank integer references bank(bank_id),
                            sending_account integer references account(account_id),
                            receiving_account integer references account(account_id),
                            amount numeric(15, 2),
                            transaction_currency varchar(3) not null
);
alter table transaction add constraint transaction_unique unique (execution_time, type_of_transaction, sending_bank, receiving_bank, sending_account, receiving_account, amount);

create table user_bank(
                          user_id integer references "user"(user_id),
                          bank_id integer references bank(bank_id)
);