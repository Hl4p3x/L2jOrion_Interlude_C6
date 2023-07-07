
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `itemsonground`;
CREATE TABLE `itemsonground` (
  `object_id` int(11) NOT NULL DEFAULT '0',
  `item_id` int(11) DEFAULT NULL,
  `count` int(11) DEFAULT NULL,
  `enchant_level` int(11) DEFAULT NULL,
  `x` int(11) DEFAULT NULL,
  `y` int(11) DEFAULT NULL,
  `z` int(11) DEFAULT NULL,
  `drop_time` decimal(20,0) DEFAULT NULL,
  `equipable` int(1) DEFAULT '0',
  PRIMARY KEY (`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

