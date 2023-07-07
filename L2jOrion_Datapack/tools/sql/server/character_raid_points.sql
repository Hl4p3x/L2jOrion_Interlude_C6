
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_raid_points`;
CREATE TABLE `character_raid_points` (
  `charId` int(10) unsigned NOT NULL DEFAULT '0',
  `boss_id` int(10) unsigned NOT NULL DEFAULT '0',
  `points` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`charId`,`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
