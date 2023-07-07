
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_quests`;
CREATE TABLE `character_quests` (
  `char_id` int(11) NOT NULL DEFAULT '0',
  `name` varchar(40) NOT NULL DEFAULT '',
  `var` varchar(20) NOT NULL DEFAULT '',
  `value` varchar(255) DEFAULT NULL,
  `class_index` int(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_id`,`name`,`var`,`class_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `character_quests` VALUES ('268474976', '7000_HeroItems', '<state>', 'Created', '0');
