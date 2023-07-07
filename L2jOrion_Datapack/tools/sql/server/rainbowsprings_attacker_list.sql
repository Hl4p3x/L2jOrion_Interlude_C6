
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `rainbowsprings_attacker_list`;
CREATE TABLE `rainbowsprings_attacker_list` (
  `clanId` int(10) DEFAULT NULL,
  `war_decrees_count` double(20,0) DEFAULT NULL,
  KEY `clanid` (`clanId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
