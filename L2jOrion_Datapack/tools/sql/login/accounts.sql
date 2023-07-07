SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `accounts`;
CREATE TABLE `accounts` (
  `login` varchar(45) NOT NULL DEFAULT '',
  `password` varchar(45) DEFAULT NULL,
  `serial` varchar(30) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `lastactive` decimal(20,0) DEFAULT NULL,
  `last_active` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `access_level` int(11) DEFAULT NULL,
  `lastIP` varchar(20) DEFAULT NULL,
  `lastServer` int(4) DEFAULT '1',
  `IPBlock` tinyint(1) DEFAULT '0',
  `HWIDBlock` varchar(45) DEFAULT NULL,
  `HWIDBlockON` int(4) DEFAULT '0',
  PRIMARY KEY (`login`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

