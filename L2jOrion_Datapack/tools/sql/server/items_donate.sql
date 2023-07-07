
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `items_donate`;
CREATE TABLE `items_donate` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `item_id` int(6) unsigned NOT NULL,
  `count` int(10) unsigned NOT NULL DEFAULT '1',
  `enchant_level` int(6) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `key_owner_id` (`owner_id`),
  KEY `key_item_id` (`item_id`)
) ENGINE=MyISAM AUTO_INCREMENT=18 DEFAULT CHARSET=utf8;
