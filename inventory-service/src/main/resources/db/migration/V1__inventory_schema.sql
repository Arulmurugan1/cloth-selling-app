create table if not exists products (
    id varchar(36) primary key,
    sku varchar(64) not null unique,
    name varchar(200) not null,
    description varchar(2000),
    category varchar(100),
    size_label varchar(30),
    color varchar(50),
    price numeric(12,2) not null,
    image_url varchar(500),
    stock_qty integer not null default 0,
    reserved_qty integer not null default 0,
    released_qty integer not null default 0,
    defective_qty integer not null default 0,
    active boolean not null default true,
    defective boolean not null default false,
    version bigint not null default 0,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index if not exists idx_products_name on products(name);
create index if not exists idx_products_category on products(category);
