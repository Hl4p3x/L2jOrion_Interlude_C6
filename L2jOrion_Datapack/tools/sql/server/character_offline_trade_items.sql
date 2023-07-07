
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_offline_trade_items`;
CREATE TABLE `character_offline_trade_items` (
  `charId` int(10) NOT NULL DEFAULT '0',
  `item` int(10) NOT NULL DEFAULT '0',
  `count` int(20) NOT NULL DEFAULT '0',
  `price` int(20) NOT NULL DEFAULT '0',
  `enchant` int(20) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
