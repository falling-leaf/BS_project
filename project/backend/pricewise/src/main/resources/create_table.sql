use pricewise;

create table if not exists User (
                                    id int primary key auto_increment,
                                    account varchar(128) not null unique,
    password varchar(128) not null,
    email varchar(128) not null unique
    );