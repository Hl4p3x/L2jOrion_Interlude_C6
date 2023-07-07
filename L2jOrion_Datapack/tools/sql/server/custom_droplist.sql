
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_droplist`;
CREATE TABLE `custom_droplist` (
  `mobId` int(11) NOT NULL DEFAULT '0',
  `itemId` int(11) NOT NULL DEFAULT '0',
  `min` int(11) NOT NULL DEFAULT '0',
  `max` int(11) NOT NULL DEFAULT '0',
  `category` int(11) NOT NULL DEFAULT '0',
  `chance` int(11) NOT NULL DEFAULT '0',
  `enchantMin` int(11) NOT NULL DEFAULT '0',
  `enchantMax` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`mobId`,`itemId`,`category`),
  KEY `key_mobId` (`mobId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
