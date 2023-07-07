
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `clan_skills`;
CREATE TABLE `clan_skills` (
  `clan_id` int(11) NOT NULL DEFAULT '0',
  `skill_id` int(11) NOT NULL DEFAULT '0',
  `skill_level` int(5) NOT NULL DEFAULT '0',
  `skill_name` varchar(26) DEFAULT NULL,
  PRIMARY KEY (`clan_id`,`skill_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
