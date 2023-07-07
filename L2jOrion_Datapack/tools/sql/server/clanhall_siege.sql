
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `clanhall_siege`;
CREATE TABLE `clanhall_siege` (
  `clanHallId` int(10) NOT NULL DEFAULT '0',
  `name` varchar(45) DEFAULT NULL,
  `ownerId` int(10) DEFAULT NULL,
  `desc` varchar(100) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `nextSiege` bigint(20) DEFAULT NULL,
  `siegeLenght` int(10) DEFAULT NULL,
  `schedule_config` varchar(20) DEFAULT NULL,
  PRIMARY KEY (`clanHallId`),
  KEY `ownerId` (`ownerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `clanhall_siege` VALUES ('21', 'Fortress of Resistance', '0', 'Contestable Clan Hall', 'Dion', '1573387200210', '3600000', '14;0;0;12;00');
INSERT INTO `clanhall_siege` VALUES ('34', 'Devastated Castle', '0', 'Contestable Clan Hall', 'Aden', '1573387200213', '3600000', '14;0;0;12;00');
INSERT INTO `clanhall_siege` VALUES ('35', 'Bandit StrongHold', '0', 'Contestable Clan Hall', 'Oren', '1573387200215', '3600000', '14;0;0;12;00');
INSERT INTO `clanhall_siege` VALUES ('62', 'Rainbow Springs', '0', 'Contestable Clan Hall', 'Goddard', '1573387200216', '3600000', '14;0;0;12;00');
INSERT INTO `clanhall_siege` VALUES ('63', 'Beast Farm', '0', 'Contestable Clan Hall', 'Rune', '1573387200218', '3600000', '14;0;0;12;00');
INSERT INTO `clanhall_siege` VALUES ('64', 'Fortresss of the Dead', '0', 'Contestable Clan Hall', 'Rune', '1573387200219', '3600000', '14;0;0;12;00');
