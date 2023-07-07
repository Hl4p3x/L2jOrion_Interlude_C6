
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `pkkills`;
CREATE TABLE `pkkills` (
  `killerId` varchar(45) NOT NULL,
  `killedId` varchar(45) NOT NULL,
  `kills` decimal(11,0) NOT NULL,
  PRIMARY KEY (`killerId`,`killedId`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
