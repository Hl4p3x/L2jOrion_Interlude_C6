SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_npc`;
CREATE TABLE `custom_npc` (
  `id` decimal(11,0) NOT NULL DEFAULT '0',
  `idTemplate` int(11) NOT NULL DEFAULT '0',
  `name` varchar(200) DEFAULT NULL,
  `serverSideName` int(1) DEFAULT '0',
  `title` varchar(45) DEFAULT '',
  `serverSideTitle` int(1) DEFAULT '0',
  `class` varchar(200) DEFAULT NULL,
  `collision_radius` decimal(5,2) DEFAULT NULL,
  `collision_height` decimal(5,2) DEFAULT NULL,
  `level` decimal(2,0) DEFAULT NULL,
  `sex` varchar(6) DEFAULT NULL,
  `type` varchar(20) DEFAULT NULL,
  `attackrange` int(11) DEFAULT NULL,
  `hp` decimal(8,0) DEFAULT NULL,
  `mp` decimal(5,0) DEFAULT NULL,
  `hpreg` decimal(8,2) DEFAULT NULL,
  `mpreg` decimal(5,2) DEFAULT NULL,
  `str` decimal(7,0) DEFAULT NULL,
  `con` decimal(7,0) DEFAULT NULL,
  `dex` decimal(7,0) DEFAULT NULL,
  `int` decimal(7,0) DEFAULT NULL,
  `wit` decimal(7,0) DEFAULT NULL,
  `men` decimal(7,0) DEFAULT NULL,
  `exp` decimal(9,0) DEFAULT NULL,
  `sp` decimal(8,0) DEFAULT NULL,
  `patk` decimal(5,0) DEFAULT NULL,
  `pdef` decimal(5,0) DEFAULT NULL,
  `matk` decimal(5,0) DEFAULT NULL,
  `mdef` decimal(5,0) DEFAULT NULL,
  `atkspd` decimal(3,0) DEFAULT NULL,
  `aggro` decimal(6,0) DEFAULT NULL,
  `matkspd` decimal(4,0) DEFAULT NULL,
  `rhand` decimal(4,0) DEFAULT NULL,
  `lhand` decimal(4,0) DEFAULT NULL,
  `armor` decimal(1,0) DEFAULT NULL,
  `walkspd` decimal(3,0) DEFAULT NULL,
  `runspd` decimal(3,0) DEFAULT NULL,
  `faction_id` varchar(40) DEFAULT NULL,
  `faction_range` decimal(4,0) DEFAULT NULL,
  `isUndead` int(11) DEFAULT '0',
  `absorb_level` decimal(2,0) DEFAULT '0',
  `absorb_type` enum('FULL_PARTY','LAST_HIT','PARTY_ONE_RANDOM') NOT NULL DEFAULT 'LAST_HIT',
  `DropHerb` int(1) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

INSERT INTO `custom_npc` VALUES ('70010', '31606', 'TvT Event Manager', '1', 'L2jOrion.com', '1', 'Monster2.queen_of_cat', '8.00', '15.00', '70', 'female', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '28', '132', null, '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('70011', '31606', 'CTF Event Manager', '1', 'L2jOrion.com', '1', 'Monster2.queen_of_cat', '8.00', '15.00', '70', 'female', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '28', '132', null, '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('70012', '31606', 'DM Event Manager', '1', 'L2jOrion.com', '1', 'Monster2.queen_of_cat', '8.00', '15.00', '70', 'female', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '28', '132', null, '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('4', '31230', 'Class Changer', '1', 'L2jOrion.com', '1', 'Monster.cat_the_cat', '6.00', '16.00', '70', 'male', 'L2ClassMaster', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1335', '470', '780', '382', '278', '0', '333', '0', '0', '0', '88', '132', '', '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('3', '25286', 'Global GK', '1', 'L2jOrion.com', '1', 'NPC.broadcasting_tower', '15.00', '29.00', '70', 'etc', 'L2Teleporter', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '490', '10', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '55', '132', '', '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('2', '30088', 'Buffer', '1', 'L2jOrion.com', '1', 'Monster2.apostle_warrior', '8.00', '24.50', '70', 'female', 'L2Npc', '40', '4297', '1710', '13.43', '3.09', '40', '43', '30', '21', '20', '10', '0', '0', '2242', '534', '994', '433', '200', '0', '333', '0', '0', '0', '55', '198', '', '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('6', '31324', 'Wedding Manager', '1', 'L2jOrion.com', '1', 'NPC.a_casino_FDarkElf', '8.00', '23.00', '70', 'female', 'L2WeddingManager', '40', '3862', '1493', '500.00', '500.00', '40', '43', '30', '21', '20', '10', '0', '0', '9999', '9999', '999', '999', '278', '0', '333', '316', '0', '0', '55', '132', '', '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('1', '25283', 'GM Shop', '1', 'L2jOrion.com', '1', 'Monster.cat_the_cat', '42.00', '55.00', '70', 'male', 'L2Merchant', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '490', '10', '1335', '470', '780', '382', '278', '0', '333', '0', '0', '0', '88', '132', null, '0', '0', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('5', '30087', 'Marketer', '1', 'L2jOrion.com', '1', 'NPC.e_traderB_master_MDwarf', '7.00', '23.00', '70', 'male', 'L2Npc', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '490', '10', '1335', '99999', '780', '99999', '278', '0', '333', '0', '0', '0', '88', '132', '', '0', '0', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('7', '30842', 'Alex', '1', 'Achievements', '1', 'NPC.a_trader_FElf', '8.00', '24.00', '70', 'female', 'L2Achievement', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '55', '132', '', '0', '1', '0', 'LAST_HIT', '0');
INSERT INTO `custom_npc` VALUES ('70013', '31606', 'Tournament Manager', '1', 'Server Name', '1', 'Monster2.queen_of_cat', '8.00', '15.00', '70', 'female', 'L2Tournament', '40', '3862', '1493', '11.85', '2.78', '40', '43', '30', '21', '20', '10', '0', '0', '1314', '470', '780', '382', '278', '0', '333', '0', '0', '0', '28', '132', '', '0', '0', '0', 'LAST_HIT', '0');
