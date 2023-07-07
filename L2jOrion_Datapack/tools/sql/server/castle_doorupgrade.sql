
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `castle_doorupgrade`;
CREATE TABLE `castle_doorupgrade` (
  `doorId` int(11) NOT NULL DEFAULT '0',
  `hp` tinyint(4) NOT NULL DEFAULT '0',
  `castleId` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`doorId`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;
