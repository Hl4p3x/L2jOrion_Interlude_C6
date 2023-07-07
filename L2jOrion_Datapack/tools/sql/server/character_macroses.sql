
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_macroses`;
CREATE TABLE `character_macroses` (
  `char_obj_id` int(11) NOT NULL DEFAULT '0',
  `id` int(11) NOT NULL DEFAULT '0',
  `icon` int(11) DEFAULT NULL,
  `name` varchar(40) DEFAULT NULL,
  `descr` varchar(80) DEFAULT NULL,
  `acronym` varchar(5) DEFAULT NULL,
  `commands` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`char_obj_id`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `character_macroses` VALUES ('268474907', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268474907', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268474907', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268474907', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268474907', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268474907', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268474907', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268474907', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268474930', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268474930', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268474930', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268474930', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268474930', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268474930', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268474930', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268474930', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268474959', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268474959', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268474959', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268474959', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268474959', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268474959', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268474959', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268474959', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268474976', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268474976', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268474976', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268474976', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268474976', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268474976', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268474976', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268474976', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268475031', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268475031', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268475031', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268475031', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268475031', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268475031', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268475031', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268475031', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268475062', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268475062', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268475062', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268475062', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268475062', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268475062', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268475062', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268475062', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268475104', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268475104', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268475104', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268475104', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268475104', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268475104', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268475104', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268475104', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268475134', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268475134', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268475134', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268475134', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268475134', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268475134', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268475134', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268475134', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
INSERT INTO `character_macroses` VALUES ('268475195', '1000', '0', 'Buffer', '', 'BUFF', '3,0,0,.buffs;');
INSERT INTO `character_macroses` VALUES ('268475195', '1001', '1', 'Global GK', '', 'GK', '3,0,0,.gk;');
INSERT INTO `character_macroses` VALUES ('268475195', '1002', '2', 'Vote Reward', '', 'VR', '3,0,0,.votereward;');
INSERT INTO `character_macroses` VALUES ('268475195', '1003', '3', 'Menu', '', 'MENU', '3,0,0,.menu;');
INSERT INTO `character_macroses` VALUES ('268475195', '1004', '4', 'Bosses', '', 'BOSS', '3,0,0,.boss;');
INSERT INTO `character_macroses` VALUES ('268475195', '1005', '5', 'Class Changer', '', 'CLASS', '3,0,0,.class;');
INSERT INTO `character_macroses` VALUES ('268475195', '1006', '6', 'Sub Class', '', 'SUB', '3,0,0,.sub;');
INSERT INTO `character_macroses` VALUES ('268475195', '1007', '3', 'Shop', '', 'SHOP', '3,0,0,.shop;');
