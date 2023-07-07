
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `buff_schemes`;
CREATE TABLE `buff_schemes` (
  `ownerId` int(10) unsigned NOT NULL DEFAULT '0',
  `skill_id` int(10) unsigned NOT NULL DEFAULT '0',
  `skill_level` int(10) unsigned NOT NULL DEFAULT '0',
  `premium` int(1) NOT NULL,
  `voter` int(1) NOT NULL,
  `useItem` int(1) NOT NULL,
  `itemId` int(4) NOT NULL,
  `itemCount` decimal(10,0) NOT NULL DEFAULT '-1',
  `scheme` varchar(20) NOT NULL DEFAULT 'default'
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
