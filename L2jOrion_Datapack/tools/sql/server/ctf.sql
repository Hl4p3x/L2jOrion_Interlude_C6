
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `ctf`;
CREATE TABLE `ctf` (
  `eventName` varchar(255) NOT NULL DEFAULT '',
  `eventDesc` varchar(255) NOT NULL DEFAULT '',
  `joiningLocation` varchar(255) NOT NULL DEFAULT '',
  `minlvl` int(4) NOT NULL DEFAULT '0',
  `maxlvl` int(4) NOT NULL DEFAULT '0',
  `npcId` int(8) NOT NULL DEFAULT '0',
  `npcX` int(11) NOT NULL DEFAULT '0',
  `npcY` int(11) NOT NULL DEFAULT '0',
  `npcZ` int(11) NOT NULL DEFAULT '0',
  `npcHeading` int(11) NOT NULL DEFAULT '0',
  `rewardId` int(11) NOT NULL DEFAULT '0',
  `rewardAmount` int(11) NOT NULL DEFAULT '0',
  `teamsCount` int(4) NOT NULL DEFAULT '0',
  `joinTime` int(11) NOT NULL DEFAULT '0',
  `eventTime` int(11) NOT NULL DEFAULT '0',
  `minPlayers` int(4) NOT NULL DEFAULT '0',
  `maxPlayers` int(4) NOT NULL DEFAULT '0',
  `delayForNextEvent` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`eventName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `ctf` VALUES ('CTF', 'The Event - Capture The Flag', 'Giran', '61', '80', '70011', '82838', '147916', '-3472', '0', '57', '10000', '2', '5', '5', '2', '100', '300000');
