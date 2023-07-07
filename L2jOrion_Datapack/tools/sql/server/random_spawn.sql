
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `random_spawn`;
CREATE TABLE `random_spawn` (
  `groupId` int(11) NOT NULL DEFAULT '0',
  `npcId` int(11) NOT NULL DEFAULT '0',
  `count` int(11) NOT NULL DEFAULT '0',
  `initialDelay` bigint(20) NOT NULL DEFAULT '-1',
  `respawnDelay` bigint(20) NOT NULL DEFAULT '-1',
  `despawnDelay` bigint(20) NOT NULL DEFAULT '-1',
  `broadcastSpawn` varchar(5) NOT NULL DEFAULT 'false',
  `randomSpawn` varchar(5) NOT NULL DEFAULT 'true',
  `respawn_time` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `random_spawn` VALUES ('1', '30556', '1', '-1', '1800000', '1800000', 'false', 'true', '0');
INSERT INTO `random_spawn` VALUES ('9', '31111', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('10', '31112', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('11', '31113', '1', '-1', '-1', '-1', 'true', 'true', '0');
INSERT INTO `random_spawn` VALUES ('12', '31126', '1', '-1', '-1', '-1', 'true', 'true', '0');
INSERT INTO `random_spawn` VALUES ('13', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('14', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('15', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('16', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('17', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('18', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('19', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('20', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('21', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('22', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('23', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('24', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('25', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('26', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('27', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('28', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('29', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('30', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('31', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('32', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('33', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('34', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('35', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('36', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('37', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('38', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('39', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('40', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('41', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('42', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('43', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('44', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('45', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('46', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('47', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('48', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('49', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('50', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('51', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('52', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('53', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('54', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('55', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('56', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('57', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('58', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('59', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('60', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('61', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('62', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('63', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('64', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('65', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('66', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('67', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('68', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('69', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('70', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('71', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('72', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('73', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('74', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('75', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('76', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('77', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('78', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('79', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('80', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('81', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('82', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('83', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('84', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('85', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('86', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('87', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('88', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('89', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('90', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('91', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('92', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('93', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('94', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('95', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('96', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('97', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('98', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('99', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('100', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('101', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('102', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('103', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('104', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('105', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('106', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('107', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('108', '31170', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('109', '31171', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('110', '25283', '1', '-1', '86400', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('111', '25286', '1', '-1', '86400', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('112', '27316', '1', '1800000', '14400000', '1800000', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('113', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('114', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('115', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('116', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('117', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('118', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('119', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('120', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('121', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('122', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('123', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('124', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('125', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('126', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('127', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('128', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('129', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('130', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('131', '31093', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('132', '31094', '1', '-1', '60', '0', 'false', 'false', '0');
INSERT INTO `random_spawn` VALUES ('133', '32014', '1', '-1', '1800000', '1800000', 'false', 'true', '0');
INSERT INTO `random_spawn` VALUES ('134', '32013', '1', '-1', '1800000', '1800000', 'false', 'true', '0');
INSERT INTO `random_spawn` VALUES ('135', '32049', '1', '-1', '1200000', '1200000', 'false', 'true', '0');
INSERT INTO `random_spawn` VALUES ('136', '32012', '1', '-1', '1800000', '1800000', 'false', 'true', '0');
