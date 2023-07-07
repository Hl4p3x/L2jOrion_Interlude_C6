
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `items`;
CREATE TABLE `items` (
  `owner_id` int(11) DEFAULT NULL,
  `object_id` int(11) NOT NULL DEFAULT '0',
  `item_id` int(11) DEFAULT NULL,
  `count` int(11) DEFAULT NULL,
  `enchant_level` int(11) DEFAULT NULL,
  `loc` varchar(10) DEFAULT NULL,
  `loc_data` int(11) DEFAULT NULL,
  `price_sell` int(11) DEFAULT NULL,
  `price_buy` int(11) DEFAULT NULL,
  `time_of_use` int(11) DEFAULT NULL,
  `custom_type1` int(11) DEFAULT '0',
  `custom_type2` int(11) DEFAULT '0',
  `mana_left` decimal(3,0) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`object_id`),
  KEY `key_owner_id` (`owner_id`),
  KEY `key_loc` (`loc`),
  KEY `key_item_id` (`item_id`),
  KEY `key_time_of_use` (`time_of_use`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
