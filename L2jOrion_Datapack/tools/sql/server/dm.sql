
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `dm`;
CREATE TABLE `dm` (
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
  `joinTime` int(11) NOT NULL DEFAULT '0',
  `eventTime` int(11) NOT NULL DEFAULT '0',
  `minPlayers` int(11) NOT NULL DEFAULT '0',
  `maxPlayers` int(11) NOT NULL DEFAULT '0',
  `color` int(11) NOT NULL DEFAULT '0',
  `playerX` int(11) NOT NULL DEFAULT '0',
  `playerY` int(11) NOT NULL DEFAULT '0',
  `playerZ` int(11) NOT NULL DEFAULT '0',
  `delayForNextEvent` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`eventName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `dm` VALUES ('DM', 'The Event - Death Match', 'Giran', '61', '81', '70012', '82838', '147916', '-3472', '1', '57', '10000', '5', '5', '2', '100', '2552550', '-14374', '123753', '-3120', '300000');
