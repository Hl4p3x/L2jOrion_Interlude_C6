SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `castle`;
CREATE TABLE `castle` (
  `id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(25) NOT NULL,
  `taxPercent` int(11) NOT NULL DEFAULT '15',
  `treasury` int(11) NOT NULL DEFAULT '0',
  `siegeDate` decimal(20,0) NOT NULL DEFAULT '0',
  `siegeDayOfWeek` int(11) NOT NULL DEFAULT '7',
  `siegeHourOfDay` int(11) NOT NULL DEFAULT '20',
  PRIMARY KEY (`name`),
  KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `castle` VALUES ('5', 'Aden', '0', '36906939', '1573329600000', '7', '20');
INSERT INTO `castle` VALUES ('2', 'Dion', '0', '0', '1573329600000', '7', '20');
INSERT INTO `castle` VALUES ('3', 'Giran', '0', '25775594', '1573401600000', '1', '16');
INSERT INTO `castle` VALUES ('1', 'Gludio', '0', '1568116', '1573329600000', '7', '20');
INSERT INTO `castle` VALUES ('7', 'Goddard', '0', '566047', '1573401600000', '1', '16');
INSERT INTO `castle` VALUES ('6', 'Innadril', '0', '44535867', '1573401600000', '1', '16');
INSERT INTO `castle` VALUES ('4', 'Oren', '0', '43684711', '1573401600000', '1', '16');
INSERT INTO `castle` VALUES ('8', 'Rune', '0', '11190833', '1573329600000', '7', '20');
INSERT INTO `castle` VALUES ('9', 'Schuttgart', '0', '0', '1573329600000', '7', '20');
