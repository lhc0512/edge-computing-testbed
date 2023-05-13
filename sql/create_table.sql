create table ec_edge_node
(
    id                     int auto_increment
        primary key,
    edge_node_id           int null,
    cpu_num                int null,
    execution_failure_rate double null,
    task_rate              double null,
    edge_node_reliability  double null
);

create table ec_link
(
    id                        int auto_increment
        primary key,
    source                    varchar(20) not null,
    destination               varchar(20) not null,
    transmission_rate         double      not null,
    transmission_failure_rate double      not null,
    link_reliability          double null
);

create table ec_task
(
    id                        int auto_increment
        primary key,
    job_id                    int null,
    time_slot                 int null,
    source                    varchar(20) null,
    destination               varchar(200) null,
    runtime_info              text null,
    avail_action              varchar(200) null,
    action                    varchar(200) null,
    status                    varchar(20) null,
    task_size                 int null,
    task_complexity           int null,
    cpu_cycle                 bigint null,
    deadline                  int null,
    transmission_waiting_time int null,
    transmission_time         int null,
    execution_waiting_time    int null,
    execution_time            int null,
    task_reliability          double null,
    reliability_requirement   double null
);

