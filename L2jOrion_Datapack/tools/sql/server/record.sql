
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `record`;
CREATE TABLE `record` (
  `maxplayer` int(11) NOT NULL DEFAULT '0',
  `date` datetime DEFAULT NULL,
  PRIMARY KEY (`maxplayer`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
