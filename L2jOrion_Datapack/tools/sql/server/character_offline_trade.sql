
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_offline_trade`;
CREATE TABLE `character_offline_trade` (
  `charId` int(11) NOT NULL,
  `name` varchar(20) NOT NULL,
  `time` bigint(20) unsigned NOT NULL DEFAULT '0',
  `type` tinyint(4) NOT NULL DEFAULT '0',
  `title` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`charId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
