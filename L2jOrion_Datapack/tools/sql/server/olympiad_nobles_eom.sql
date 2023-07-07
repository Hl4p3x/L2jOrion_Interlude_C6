
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `olympiad_nobles_eom`;
CREATE TABLE `olympiad_nobles_eom` (
  `char_id` int(10) unsigned NOT NULL DEFAULT '0',
  `class_id` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `olympiad_points` int(10) NOT NULL DEFAULT '0',
  `competitions_done` smallint(3) NOT NULL DEFAULT '0',
  `competitions_won` smallint(3) NOT NULL DEFAULT '0',
  `competitions_lost` smallint(3) NOT NULL DEFAULT '0',
  `competitions_drawn` smallint(3) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

