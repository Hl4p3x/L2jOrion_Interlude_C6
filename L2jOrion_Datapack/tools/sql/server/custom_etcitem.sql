
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_etcitem`;
CREATE TABLE `custom_etcitem` (
  `item_id` decimal(11,0) NOT NULL DEFAULT '0',
  `name` varchar(100) DEFAULT NULL,
  `crystallizable` varchar(5) DEFAULT NULL,
  `item_type` varchar(12) DEFAULT NULL,
  `weight` decimal(4,0) DEFAULT NULL,
  `consume_type` varchar(9) DEFAULT NULL,
  `crystal_type` enum('none','d','c','b','a','s') NOT NULL DEFAULT 'none',
  `duration` decimal(3,0) DEFAULT NULL,
  `price` decimal(11,0) DEFAULT NULL,
  `crystal_count` int(4) DEFAULT NULL,
  `sellable` varchar(5) DEFAULT NULL,
  `dropable` varchar(5) DEFAULT NULL,
  `destroyable` varchar(5) DEFAULT NULL,
  `tradeable` varchar(5) DEFAULT NULL,
  `oldname` varchar(100) NOT NULL DEFAULT '',
  `oldtype` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`item_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `custom_etcitem` VALUES ('10015', 'Teleport Stone', 'false', 'scroll', '0', 'stackable', 'none', '-1', '400', '0', 'false', 'false', 'false', 'false', '', '');
