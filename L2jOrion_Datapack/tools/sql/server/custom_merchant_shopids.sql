
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_merchant_shopids`;
CREATE TABLE `custom_merchant_shopids` (
  `shop_id` decimal(9,0) NOT NULL DEFAULT '0',
  `npc_id` varchar(9) DEFAULT NULL,
  PRIMARY KEY (`shop_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
