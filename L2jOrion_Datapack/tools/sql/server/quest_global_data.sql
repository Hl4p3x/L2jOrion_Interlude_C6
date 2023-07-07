
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `quest_global_data`;
CREATE TABLE `quest_global_data` (
  `quest_name` varchar(40) NOT NULL DEFAULT '',
  `var` varchar(20) NOT NULL DEFAULT '',
  `value` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`quest_name`,`var`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `quest_global_data` VALUES ('core', 'Core_Attacked', 'false');
INSERT INTO `quest_global_data` VALUES ('IceFairySirra', 'IceFairySirra', '0');
