
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `clan_subpledges`;
CREATE TABLE `clan_subpledges` (
  `clan_id` int(11) NOT NULL DEFAULT '0',
  `sub_pledge_id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(45) DEFAULT NULL,
  `leader_name` varchar(35) DEFAULT NULL,
  PRIMARY KEY (`clan_id`,`sub_pledge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
