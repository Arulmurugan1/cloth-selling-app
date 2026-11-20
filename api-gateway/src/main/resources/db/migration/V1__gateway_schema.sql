create table if not exists app_users (
    id varchar(36) primary key,
    email varchar(200) not null unique,
    mobile varchar(20) not null unique,
    full_name varchar(120) not null,
    password_hash varchar(200) not null,
    dob timestamp with time zone not null,
    role varchar(20) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    version bigint not null default 0
);

create index if not exists idx_app_users_role on app_users(role);
