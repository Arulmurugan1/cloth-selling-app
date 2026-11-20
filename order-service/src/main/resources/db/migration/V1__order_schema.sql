create table if not exists orders (
    id varchar(36) primary key,
    customer_id varchar(36) not null,
    customer_name varchar(120) not null,
    customer_email varchar(200),
    status varchar(30) not null,
    order_total numeric(12,2) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    version bigint not null default 0
);

create index if not exists idx_orders_customer on orders(customer_id);
create index if not exists idx_orders_status on orders(status);

create table if not exists order_lines (
    id varchar(36) primary key,
    order_id varchar(36) not null references orders(id) on delete cascade,
    sku varchar(64) not null,
    product_name varchar(200) not null,
    unit_price numeric(12,2) not null,
    qty integer not null
);

create index if not exists idx_order_lines_order on order_lines(order_id);
