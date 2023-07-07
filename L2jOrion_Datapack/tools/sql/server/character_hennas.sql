
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_hennas`;
CREATE TABLE `character_hennas` (
  `char_obj_id` int(11) NOT NULL DEFAULT '0',
  `symbol_id` int(11) DEFAULT NULL,
  `slot` int(11) NOT NULL DEFAULT '0',
  `class_index` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_obj_id`,`slot`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `character_hennas` VALUES ('268475031', '113', '1', '0');
INSERT INTO `character_hennas` VALUES ('268475031', '173', '2', '0');
INSERT INTO `character_hennas` VALUES ('268475031', '171', '3', '0');
INSERT INTO `character_hennas` VALUES ('268475195', '170', '1', '0');
INSERT INTO `character_hennas` VALUES ('268475195', '172', '2', '0');
INSERT INTO `character_hennas` VALUES ('268475195', '179', '3', '0');
