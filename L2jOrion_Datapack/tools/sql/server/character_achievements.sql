SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_achievements`;
CREATE TABLE `character_achievements` (
  `object_id` int(10) unsigned NOT NULL DEFAULT '0',
  `type` varchar(20) NOT NULL DEFAULT '',
  `level` tinyint(10) unsigned NOT NULL DEFAULT '0',
  `count` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`object_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
