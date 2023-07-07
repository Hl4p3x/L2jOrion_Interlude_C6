
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `merchant_lease`;
CREATE TABLE `merchant_lease` (
  `merchant_id` int(11) NOT NULL DEFAULT '0',
  `player_id` int(11) NOT NULL DEFAULT '0',
  `bid` int(11) DEFAULT NULL,
  `type` int(11) NOT NULL DEFAULT '0',
  `player_name` varchar(35) DEFAULT NULL,
  PRIMARY KEY (`merchant_id`,`player_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

