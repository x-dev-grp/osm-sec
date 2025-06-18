-- auto-generated definition
create table oauth2_registered_client
(
    id                            varchar(100)  not null
        primary key,
    client_id                     varchar(100)  not null,
    client_id_issued_at           timestamp,
    client_secret                 varchar(200),
    client_secret_expires_at      timestamp,
    client_name                   varchar(200)  not null,
    client_authentication_methods varchar(1000) not null,
    authorization_grant_types     varchar(1000) not null,
    redirect_uris                 varchar(1000),
    post_logout_redirect_uris     varchar(1000),
    scopes                        varchar(1000) not null,
    client_settings               varchar(2000) not null,
    token_settings                varchar(2000) not null
);

alter table oauth2_registered_client
    owner to postgres;

create unique index idx_oauth2_registered_client_client_id
    on oauth2_registered_client (client_id);