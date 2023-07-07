
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `merchant_areas_list`;
CREATE TABLE `merchant_areas_list` (
  `merchant_area_id` int(10) unsigned NOT NULL DEFAULT '0',
  `merchant_area_name` varchar(25) NOT NULL DEFAULT '',
  `tax` double(3,2) unsigned NOT NULL DEFAULT '0.00',
  `Chaotic` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`merchant_area_id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `merchant_areas_list` VALUES ('1', 'Starter Town', '0.15', '0');
INSERT INTO `merchant_areas_list` VALUES ('2', 'West of Alter of Rights', '0.50', '1');
INSERT INTO `merchant_areas_list` VALUES ('3', 'Gludin', '0.20', '0');
INSERT INTO `merchant_areas_list` VALUES ('4', 'Gludio', '0.20', '0');
INSERT INTO `merchant_areas_list` VALUES ('5', 'South Wastelands', '0.50', '1');
INSERT INTO `merchant_areas_list` VALUES ('6', 'Dion', '0.20', '0');
INSERT INTO `merchant_areas_list` VALUES ('7', 'Floran', '0.50', '1');
INSERT INTO `merchant_areas_list` VALUES ('8', 'Hunters Village', '0.30', '0');
INSERT INTO `merchant_areas_list` VALUES ('9', 'Giran', '0.10', '0');
INSERT INTO `merchant_areas_list` VALUES ('10', 'Oren', '0.15', '0');
INSERT INTO `merchant_areas_list` VALUES ('11', 'Ivory Tower', '0.15', '0');
INSERT INTO `merchant_areas_list` VALUES ('12', 'Harden\'s Ac.', '0.20', '0');
INSERT INTO `merchant_areas_list` VALUES ('13', 'Aden', '0.20', '0');
INSERT INTO `merchant_areas_list` VALUES ('14', 'Castle', '0.00', '0');
