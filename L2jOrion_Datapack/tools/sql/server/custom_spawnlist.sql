
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_spawnlist`;
CREATE TABLE `custom_spawnlist` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `location` varchar(40) NOT NULL DEFAULT '',
  `count` int(9) NOT NULL DEFAULT '0',
  `npc_templateid` int(9) NOT NULL DEFAULT '0',
  `locx` int(9) NOT NULL DEFAULT '0',
  `locy` int(9) NOT NULL DEFAULT '0',
  `locz` int(9) NOT NULL DEFAULT '0',
  `randomx` int(9) NOT NULL DEFAULT '0',
  `randomy` int(9) NOT NULL DEFAULT '0',
  `heading` int(9) NOT NULL DEFAULT '0',
  `respawn_delay` int(9) NOT NULL DEFAULT '0',
  `loc_id` int(9) NOT NULL DEFAULT '0',
  `periodOfDay` decimal(2,0) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `key_npc_templateid` (`npc_templateid`)
) ENGINE=MyISAM AUTO_INCREMENT=570493 DEFAULT CHARSET=latin1;

INSERT INTO `custom_spawnlist` VALUES ('203346', '', '1', '31760', '-45022', '-113508', '-202', '0', '0', '32074', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('203345', '', '1', '31760', '115631', '-178039', '-916', '0', '0', '36123', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('203344', '', '1', '31760', '12123', '16727', '-4587', '0', '0', '62345', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('203343', '', '1', '31760', '45517', '48361', '-3063', '0', '0', '51353', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('203342', '', '1', '31760', '-84052', '243205', '-3732', '0', '0', '10970', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('481265', '', '1', '1', '82212', '148730', '-3464', '0', '0', '64265', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('481266', '', '1', '3', '82208', '148480', '-3464', '0', '0', '64349', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('476233', '', '1', '6', '82372', '148799', '-3466', '0', '0', '64750', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('481264', '', '1', '2', '82222', '148251', '-3469', '0', '0', '1382', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('478836', '', '1', '4', '82373', '148416', '-3466', '0', '0', '64668', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567964', '', '1', '1', '147670', '46568', '-3400', '0', '0', '16187', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('481263', '', '1', '5', '82217', '148960', '-3464', '0', '0', '64590', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567962', '', '1', '2', '147561', '46579', '-3400', '0', '0', '15862', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567965', '', '1', '4', '147448', '46574', '-3400', '0', '0', '17423', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567966', '', '1', '2', '151430', '46859', '-3400', '0', '0', '47188', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567967', '', '1', '1', '151321', '46869', '-3400', '0', '0', '49151', '10', '0', '0');
INSERT INTO `custom_spawnlist` VALUES ('567968', '', '1', '4', '151549', '46869', '-3400', '0', '0', '49400', '10', '0', '0');
