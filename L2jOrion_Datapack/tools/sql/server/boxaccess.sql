
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `boxaccess`;
CREATE TABLE `boxaccess` (
  `spawn` decimal(11,0) DEFAULT NULL,
  `charname` varchar(32) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
