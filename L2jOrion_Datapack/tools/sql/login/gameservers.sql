SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `gameservers`;
CREATE TABLE `gameservers` (
  `server_id` int(11) NOT NULL DEFAULT '0',
  `hexid` varchar(50) NOT NULL DEFAULT '',
  `host` varchar(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`server_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `gameservers` VALUES ('1', '6e954f544e4c4f7e86a590ae7deb1096', '');
