
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `tvt_teams`;
CREATE TABLE `tvt_teams` (
  `teamId` int(4) NOT NULL DEFAULT '0',
  `teamName` varchar(255) NOT NULL DEFAULT '',
  `teamX` int(11) NOT NULL DEFAULT '0',
  `teamY` int(11) NOT NULL DEFAULT '0',
  `teamZ` int(11) NOT NULL DEFAULT '0',
  `teamColor` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`teamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `tvt_teams` VALUES ('0', 'Blue', '148179', '45841', '-3413', '16711680');
INSERT INTO `tvt_teams` VALUES ('1', 'Red', '150787', '45822', '-3413', '255');
INSERT INTO `tvt_teams` VALUES ('2', 'Green', '149496', '47826', '-3413', '26367');
