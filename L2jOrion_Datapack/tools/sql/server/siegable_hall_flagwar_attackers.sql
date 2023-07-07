
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `siegable_hall_flagwar_attackers`;
CREATE TABLE `siegable_hall_flagwar_attackers` (
  `hall_id` tinyint(2) unsigned NOT NULL DEFAULT '0',
  `flag` int(10) unsigned NOT NULL DEFAULT '0',
  `npc` int(10) unsigned NOT NULL DEFAULT '0',
  `clan_id` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`flag`),
  KEY `hall_id` (`hall_id`),
  KEY `clan_id` (`clan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
