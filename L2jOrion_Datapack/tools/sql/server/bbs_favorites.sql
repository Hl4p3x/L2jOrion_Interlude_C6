
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `bbs_favorites`;
CREATE TABLE `bbs_favorites` (
  `favId` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `playerId` int(10) unsigned NOT NULL,
  `favTitle` varchar(50) COLLATE utf8_unicode_ci NOT NULL,
  `favBypass` varchar(127) COLLATE utf8_unicode_ci NOT NULL,
  `favAddDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`favId`),
  UNIQUE KEY `favId_playerId` (`favId`,`playerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='This table saves the Favorite links from each player for the community board.';
