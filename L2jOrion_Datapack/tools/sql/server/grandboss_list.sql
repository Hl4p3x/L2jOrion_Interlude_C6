
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `grandboss_list`;
CREATE TABLE `grandboss_list` (
  `player_id` decimal(11,0) NOT NULL,
  `zone` decimal(11,0) NOT NULL,
  PRIMARY KEY (`player_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
