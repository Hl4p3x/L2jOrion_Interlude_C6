
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_merchant_buylists`;
CREATE TABLE `custom_merchant_buylists` (
  `item_id` decimal(9,0) NOT NULL DEFAULT '0',
  `price` decimal(11,0) NOT NULL DEFAULT '0',
  `shop_id` decimal(9,0) NOT NULL DEFAULT '0',
  `order` decimal(4,0) NOT NULL DEFAULT '0',
  `count` int(11) NOT NULL DEFAULT '-1',
  `currentCount` int(11) NOT NULL DEFAULT '-1',
  `time` int(11) NOT NULL DEFAULT '0',
  `savetimer` decimal(20,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`shop_id`,`order`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
