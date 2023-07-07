
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_skills_save`;
CREATE TABLE `character_skills_save` (
  `char_obj_id` int(11) NOT NULL DEFAULT '0',
  `skill_id` int(11) NOT NULL DEFAULT '0',
  `skill_level` int(11) NOT NULL DEFAULT '0',
  `effect_count` int(11) NOT NULL DEFAULT '0',
  `effect_cur_time` int(11) NOT NULL DEFAULT '0',
  `reuse_delay` bigint(20) NOT NULL DEFAULT '0',
  `systime` bigint(20) unsigned NOT NULL DEFAULT '0',
  `restore_type` int(1) NOT NULL DEFAULT '0',
  `class_index` int(1) NOT NULL DEFAULT '0',
  `buff_index` int(2) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_obj_id`,`skill_id`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `character_skills_save` VALUES ('268474930', '78', '2', '-1', '-1', '148734', '1571852268383', '1', '0', '1');
INSERT INTO `character_skills_save` VALUES ('268474959', '264', '1', '1', '27', '0', '0', '0', '0', '15');
INSERT INTO `character_skills_save` VALUES ('268474959', '267', '1', '1', '27', '0', '0', '0', '0', '16');
INSERT INTO `character_skills_save` VALUES ('268474959', '268', '1', '1', '27', '0', '0', '0', '0', '20');
INSERT INTO `character_skills_save` VALUES ('268474959', '269', '1', '1', '27', '0', '0', '0', '0', '17');
INSERT INTO `character_skills_save` VALUES ('268474959', '271', '1', '1', '27', '0', '0', '0', '0', '11');
INSERT INTO `character_skills_save` VALUES ('268474959', '274', '1', '1', '27', '0', '0', '0', '0', '12');
INSERT INTO `character_skills_save` VALUES ('268474959', '275', '1', '1', '27', '0', '0', '0', '0', '13');
INSERT INTO `character_skills_save` VALUES ('268474959', '304', '1', '1', '27', '0', '0', '0', '0', '18');
INSERT INTO `character_skills_save` VALUES ('268474959', '310', '1', '1', '27', '0', '0', '0', '0', '14');
INSERT INTO `character_skills_save` VALUES ('268474959', '349', '1', '1', '27', '0', '0', '0', '0', '21');
INSERT INTO `character_skills_save` VALUES ('268474959', '364', '1', '1', '27', '0', '0', '0', '0', '19');
INSERT INTO `character_skills_save` VALUES ('268474959', '1035', '3', '1', '27', '0', '0', '0', '0', '2');
INSERT INTO `character_skills_save` VALUES ('268474959', '1036', '2', '1', '27', '0', '0', '0', '0', '3');
INSERT INTO `character_skills_save` VALUES ('268474959', '1040', '3', '1', '27', '0', '0', '0', '0', '4');
INSERT INTO `character_skills_save` VALUES ('268474959', '1045', '6', '1', '27', '0', '0', '0', '0', '5');
INSERT INTO `character_skills_save` VALUES ('268474959', '1048', '6', '1', '27', '0', '0', '0', '0', '6');
INSERT INTO `character_skills_save` VALUES ('268474959', '1068', '3', '1', '27', '0', '0', '0', '0', '7');
INSERT INTO `character_skills_save` VALUES ('268474959', '1077', '3', '1', '27', '0', '0', '0', '0', '8');
INSERT INTO `character_skills_save` VALUES ('268474959', '1086', '2', '1', '27', '0', '0', '0', '0', '9');
INSERT INTO `character_skills_save` VALUES ('268474959', '1204', '2', '1', '27', '0', '0', '0', '0', '1');
INSERT INTO `character_skills_save` VALUES ('268474959', '1242', '3', '1', '27', '0', '0', '0', '0', '22');
INSERT INTO `character_skills_save` VALUES ('268474959', '1268', '3', '1', '27', '0', '0', '0', '0', '10');
INSERT INTO `character_skills_save` VALUES ('268474959', '1363', '1', '1', '27', '0', '0', '0', '0', '23');
INSERT INTO `character_skills_save` VALUES ('268474959', '1388', '3', '1', '27', '0', '0', '0', '0', '24');
INSERT INTO `character_skills_save` VALUES ('268475031', '111', '2', '-1', '-1', '908181', '1571853028285', '1', '0', '1');
INSERT INTO `character_skills_save` VALUES ('268475031', '445', '1', '-1', '-1', '302727', '1571852424128', '1', '0', '2');
INSERT INTO `character_skills_save` VALUES ('268475062', '287', '3', '-1', '-1', '772422', '1571852194243', '1', '0', '1');
INSERT INTO `character_skills_save` VALUES ('268475134', '341', '1', '-1', '-1', '850212', '1571852590537', '1', '0', '1');
INSERT INTO `character_skills_save` VALUES ('268475134', '368', '1', '-1', '-1', '1275319', '1571853058449', '1', '0', '2');
