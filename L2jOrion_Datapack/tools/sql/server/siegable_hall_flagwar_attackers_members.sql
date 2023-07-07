
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `siegable_hall_flagwar_attackers_members`;
CREATE TABLE `siegable_hall_flagwar_attackers_members` (
  `hall_id` tinyint(2) unsigned NOT NULL DEFAULT '0',
  `clan_id` int(10) unsigned NOT NULL DEFAULT '0',
  `object_id` int(10) unsigned NOT NULL DEFAULT '0',
  KEY `hall_id` (`hall_id`),
  KEY `clan_id` (`clan_id`),
  KEY `object_id` (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
