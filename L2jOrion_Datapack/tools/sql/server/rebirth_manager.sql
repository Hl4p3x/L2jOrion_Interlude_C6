
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `rebirth_manager`;
CREATE TABLE `rebirth_manager` (
  `playerId` int(20) NOT NULL,
  `rebirthCount` int(2) NOT NULL,
  PRIMARY KEY (`playerId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
