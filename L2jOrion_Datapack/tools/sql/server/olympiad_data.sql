
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `olympiad_data`;
CREATE TABLE `olympiad_data` (
  `id` tinyint(3) unsigned NOT NULL DEFAULT '0',
  `current_cycle` mediumint(8) unsigned NOT NULL DEFAULT '1',
  `period` mediumint(8) unsigned NOT NULL DEFAULT '0',
  `olympiad_end` bigint(13) unsigned NOT NULL DEFAULT '0',
  `validation_end` bigint(13) unsigned NOT NULL DEFAULT '0',
  `next_weekly_change` bigint(13) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
