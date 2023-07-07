
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `ctf_teams`;
CREATE TABLE `ctf_teams` (
  `teamId` int(4) NOT NULL DEFAULT '0',
  `teamName` varchar(255) NOT NULL DEFAULT '',
  `teamX` int(11) NOT NULL DEFAULT '0',
  `teamY` int(11) NOT NULL DEFAULT '0',
  `teamZ` int(11) NOT NULL DEFAULT '0',
  `teamColor` int(11) NOT NULL DEFAULT '0',
  `flagX` int(11) NOT NULL DEFAULT '0',
  `flagY` int(11) NOT NULL DEFAULT '0',
  `flagZ` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`teamId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `ctf_teams` VALUES ('0', 'Blue', '-21303', '188582', '-4616', '255', '-21229', '188482', '-4608');
INSERT INTO `ctf_teams` VALUES ('1', 'Red', '-23140', '191666', '-4512', '16711680', '-23152', '191847', '-4464');
