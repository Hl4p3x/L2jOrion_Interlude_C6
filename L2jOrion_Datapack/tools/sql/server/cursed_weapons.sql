
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `cursed_weapons`;
CREATE TABLE `cursed_weapons` (
  `itemId` int(11) NOT NULL DEFAULT '0',
  `playerId` int(11) DEFAULT '0',
  `playerKarma` int(11) DEFAULT '0',
  `playerPkKills` int(11) DEFAULT '0',
  `nbKills` int(11) DEFAULT '0',
  `endTime` decimal(20,0) DEFAULT '0',
  PRIMARY KEY (`itemId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;
