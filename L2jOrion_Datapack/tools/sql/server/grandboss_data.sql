
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `grandboss_data`;
CREATE TABLE `grandboss_data` (
  `Name` varchar(100) DEFAULT NULL,
  `boss_id` int(11) NOT NULL DEFAULT '0',
  `loc_x` int(11) NOT NULL DEFAULT '0',
  `loc_y` int(11) NOT NULL DEFAULT '0',
  `loc_z` int(11) NOT NULL DEFAULT '0',
  `heading` int(11) NOT NULL DEFAULT '0',
  `respawn_time` bigint(20) NOT NULL DEFAULT '0',
  `killed_time` varchar(200) DEFAULT NULL,
  `next_respawn` varchar(200) DEFAULT NULL,
  `currentHP` decimal(8,0) DEFAULT NULL,
  `currentMP` decimal(8,0) DEFAULT NULL,
  `status` tinyint(4) NOT NULL DEFAULT '0',
  PRIMARY KEY (`boss_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `grandboss_data` VALUES ('Queen Ant', '29001', '-21610', '181594', '-5714', '0', '0', '-', '-', '229898', '667', '0');
INSERT INTO `grandboss_data` VALUES ('Core', '29006', '17726', '108915', '-6460', '0', '0', '-', '-', '162561', '575', '0');
INSERT INTO `grandboss_data` VALUES ('Orfen', '29014', '55024', '17368', '-5392', '0', '0', '-', '-', '325124', '1660', '0');
INSERT INTO `grandboss_data` VALUES ('Antharas', '29019', '181310', '114747', '-7672', '44058', '0', '-', '-', '13081412', '22197', '0');
INSERT INTO `grandboss_data` VALUES ('Baium', '29020', '115824', '17372', '10080', '14151', '0', '-', '-', '1709400', '16377', '0');
INSERT INTO `grandboss_data` VALUES ('Zaken', '29022', '56289', '220126', '-2947', '0', '0', '-', '-', '858518', '1924', '0');
INSERT INTO `grandboss_data` VALUES ('Valakas', '29028', '213016', '-114902', '-1575', '0', '0', '-', '-', '11662000', '15537', '0');
INSERT INTO `grandboss_data` VALUES ('Frintezza', '29045', '174240', '-89805', '-5002', '16048', '0', '-', '-', '790857', '1859', '0');
INSERT INTO `grandboss_data` VALUES ('Scarlet van Halisha', '29046', '174766', '-87703', '-5095', '33535', '0', '-', '-', '1832600', '11100', '0');
INSERT INTO `grandboss_data` VALUES ('Scarlet van Halisha', '29047', '174737', '-87694', '-5095', '0', '0', '-', '-', '1305796', '23310', '0');
INSERT INTO `grandboss_data` VALUES ('Benom (Siege Boss)', '29054', '12031', '-49209', '-3008', '60226', '0', '-', '-', '1352750', '1494', '0');
INSERT INTO `grandboss_data` VALUES ('High Priestess van Halter', '29062', '-16397', '-53308', '-10448', '16384', '0', '-', '-', '115708', '1866', '1');
