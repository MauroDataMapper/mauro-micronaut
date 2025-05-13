create table if not exists core."email" (
    "id"                    uuid primary key not null default uuid_generate_v4(),
    "version"               integer          not null,
    "sent_to_email_address" varchar(255)     not null,
    "successfully_sent"     boolean          not null,
    "body"                  text             not null,
    "date_time_sent"        timestamp  with time zone not null,
    "email_service_used"    varchar(255)     not null,
    "failure_reason"        text,
    "subject"               text             not null

);

create index "idx_email_sent_to_email_address" on "core"."email" (sent_to_email_address);

