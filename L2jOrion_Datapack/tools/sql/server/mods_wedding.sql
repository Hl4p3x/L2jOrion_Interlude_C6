
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `mods_wedding`;
CREATE TABLE `mods_wedding` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `player1Id` int(11) NOT NULL DEFAULT '0',
  `player2Id` int(11) NOT NULL DEFAULT '0',
  `married` varchar(5) DEFAULT NULL,
  `affianceDate` decimal(20,0) DEFAULT '0',
  `weddingDate` decimal(20,0) DEFAULT '0',
  `coupleType` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM AUTO_INCREMENT=269147281 DEFAULT CHARSET=utf8;
