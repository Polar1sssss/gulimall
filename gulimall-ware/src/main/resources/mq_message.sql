-- mq消息发送日志表
create table `mq_message` (
    `message_id` CHAR(32) NOT NULL,
    `content` TEXT,
    `to_exchance` VARCHAR(255) DEFAULT NULL,
    `routing_key` VARCHAR(255) DEFAULT NULL,
    `class_type` VARCHAR(255) DEFAULT NULL,
    `message_status` INT(1) DEFAULT '0' COMMENT '0-新建 1-已发送 2-错误抵达 3-已抵达',
    `create_time` DATETIME DEFAULT NULL,
    `update_time` DATETIME DEFAULT NULL,
    PRIMARY KEY(`message_id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8mb4