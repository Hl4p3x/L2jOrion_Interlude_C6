
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `fort`;
CREATE TABLE `fort` (
  `id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(25) NOT NULL,
  `siegeDate` decimal(20,0) NOT NULL DEFAULT '0',
  `siegeDayOfWeek` int(11) NOT NULL DEFAULT '7',
  `siegeHourOfDay` int(11) NOT NULL DEFAULT '20',
  `owner` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `fort` VALUES ('101', 'Shanty', '0', '0', '0', '0');
INSERT INTO `fort` VALUES ('102', 'Southern', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('103', 'Hive', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('104', 'Valley', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('105', 'Ivory', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('106', 'Narsell', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('107', 'Basin', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('108', 'WhiteSands', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('109', 'Borderland', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('110', 'Swampland', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('111', 'Archaic', '0', '0', '20', '268502708');
INSERT INTO `fort` VALUES ('112', 'Floran', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('113', 'CloudMountain', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('114', 'Tanor', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('115', 'Dragonspine', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('116', 'Antharas', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('117', 'Western', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('118', 'Hunters', '0', '0', '20', '268499065');
INSERT INTO `fort` VALUES ('119', 'Aaru', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('120', 'Demon', '0', '0', '20', '0');
INSERT INTO `fort` VALUES ('121', 'Monastic', '0', '0', '20', '0');
