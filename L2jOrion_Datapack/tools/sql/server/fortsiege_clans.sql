
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `fortsiege_clans`;
CREATE TABLE `fortsiege_clans` (
  `fort_id` int(1) NOT NULL DEFAULT '0',
  `clan_id` int(11) NOT NULL DEFAULT '0',
  `type` int(1) DEFAULT NULL,
  `fort_owner` int(1) DEFAULT NULL,
  PRIMARY KEY (`clan_id`,`fort_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
