CREATE TABLE identity_zones (
  id char(36) not null primary key,
  name varchar(255) not null,
  domain varchar(255) not null,
  description varchar(4096)
);

CREATE UNIQUE INDEX unique_izk_1 on identity_zones (lower(domain));
