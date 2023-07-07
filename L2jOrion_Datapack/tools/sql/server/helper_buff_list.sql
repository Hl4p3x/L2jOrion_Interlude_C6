
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `helper_buff_list`;
CREATE TABLE `helper_buff_list` (
  `id` int(11) NOT NULL DEFAULT '0',
  `skill_id` int(10) unsigned NOT NULL DEFAULT '0',
  `name` varchar(25) NOT NULL DEFAULT '',
  `skill_level` int(10) unsigned NOT NULL DEFAULT '0',
  `lower_level` int(10) unsigned NOT NULL DEFAULT '0',
  `upper_level` int(10) unsigned NOT NULL DEFAULT '0',
  `is_magic_class` varchar(5) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `helper_buff_list` VALUES ('0', '4322', 'WindWalk', '1', '8', '39', 'false');
INSERT INTO `helper_buff_list` VALUES ('1', '4323', 'Shield', '1', '11', '39', 'false');
INSERT INTO `helper_buff_list` VALUES ('2', '4338', 'Life Cubic', '1', '16', '36', 'false');
INSERT INTO `helper_buff_list` VALUES ('3', '4324', 'Bless the Body', '1', '12', '38', 'false');
INSERT INTO `helper_buff_list` VALUES ('4', '4325', 'Vampiric Rage', '1', '13', '38', 'false');
INSERT INTO `helper_buff_list` VALUES ('5', '4326', 'Regeneration', '1', '14', '38', 'false');
INSERT INTO `helper_buff_list` VALUES ('6', '4327', 'Haste', '1', '15', '37', 'false');
INSERT INTO `helper_buff_list` VALUES ('7', '4322', 'WindWalk', '1', '8', '39', 'true');
INSERT INTO `helper_buff_list` VALUES ('8', '4323', 'Shield', '1', '11', '39', 'true');
INSERT INTO `helper_buff_list` VALUES ('9', '4338', 'Life Cubic', '1', '16', '36', 'true');
INSERT INTO `helper_buff_list` VALUES ('10', '4328', 'Bless the Soul', '1', '12', '38', 'true');
INSERT INTO `helper_buff_list` VALUES ('11', '4329', 'Acumen', '1', '13', '38', 'true');
INSERT INTO `helper_buff_list` VALUES ('12', '4330', 'Concentration', '1', '14', '38', 'true');
INSERT INTO `helper_buff_list` VALUES ('13', '4331', 'Empower', '1', '15', '37', 'true');
