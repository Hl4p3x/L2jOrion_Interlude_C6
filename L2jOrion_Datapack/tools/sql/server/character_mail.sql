
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_mail`;
CREATE TABLE `character_mail` (
  `charId` int(10) NOT NULL,
  `letterId` int(10) NOT NULL DEFAULT '0',
  `senderId` int(10) NOT NULL,
  `location` varchar(45) NOT NULL,
  `recipientNames` varchar(200) DEFAULT NULL,
  `subject` varchar(128) DEFAULT NULL,
  `message` varchar(3000) DEFAULT NULL,
  `sentDate` timestamp NULL DEFAULT NULL,
  `unread` smallint(1) DEFAULT '1',
  PRIMARY KEY (`letterId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `character_mail` VALUES ('268474879', '1', '268484457', 'inbox', 'Vilmis', 'Market', 'JohnSnow bought your an item: Homunkulus\'s Sword - Acumen from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-05-17 16:09:18', '0');
INSERT INTO `character_mail` VALUES ('268484457', '2', '268484457', 'sentbox', 'Vilmis', 'Market', 'JohnSnow bought your an item: Homunkulus\'s Sword - Acumen from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-05-17 16:09:18', '0');
INSERT INTO `character_mail` VALUES ('268474879', '3', '268497316', 'inbox', 'Vilmis', 'Market', 'ERIC bought your an item: Orion Adena from the Market. You\'ve got: 1,111 Adena. Check your inventory!', '2019-05-19 07:21:17', '0');
INSERT INTO `character_mail` VALUES ('268497316', '4', '268497316', 'sentbox', 'Vilmis', 'Market', 'ERIC bought your an item: Orion Adena from the Market. You\'ve got: 1,111 Adena. Check your inventory!', '2019-05-19 07:21:17', '0');
INSERT INTO `character_mail` VALUES ('268474879', '5', '268501219', 'inbox', 'Vilmis', 'Market', 'TRest123 bought your an item: Top-Grade Life Stone: level 76 from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-05-19 20:56:45', '0');
INSERT INTO `character_mail` VALUES ('268501219', '6', '268501219', 'sentbox', 'Vilmis', 'Market', 'TRest123 bought your an item: Top-Grade Life Stone: level 76 from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-05-19 20:56:45', '0');
INSERT INTO `character_mail` VALUES ('268480022', '7', '268484719', 'archive', 'LikeMe', 'Market', 'Oki bought your an item: Arcana Mace - Acumen from the Market. You\'ve got: 999,999 LifeDrain Adena. Check your inventory!', '2019-05-23 17:10:12', '0');
INSERT INTO `character_mail` VALUES ('268484719', '8', '268484719', 'sentbox', 'LikeMe', 'Market', 'Oki bought your an item: Arcana Mace - Acumen from the Market. You\'ve got: 999,999 LifeDrain Adena. Check your inventory!', '2019-05-23 17:10:12', '0');
INSERT INTO `character_mail` VALUES ('268484719', '9', '268480022', 'inbox', 'Oki', 'Market', 'LikeMe bought your an item: Arcana Mace - Acumen from the Market. You\'ve got: 1,111,111 Adena. Check your inventory!', '2019-05-23 17:11:19', '1');
INSERT INTO `character_mail` VALUES ('268480022', '10', '268480022', 'sentbox', 'Oki', 'Market', 'LikeMe bought your an item: Arcana Mace - Acumen from the Market. You\'ve got: 1,111,111 Adena. Check your inventory!', '2019-05-23 17:11:19', '0');
INSERT INTO `character_mail` VALUES ('268501219', '11', '268493471', 'inbox', 'TRest123', 'Market', 'dsad bought your an item: Top-Grade Life Stone: level 76 from the Market. You\'ve got: 10,000 LifeDrain Adena. Check your inventory!', '2019-07-08 19:46:06', '1');
INSERT INTO `character_mail` VALUES ('268493471', '12', '268493471', 'sentbox', 'TRest123', 'Market', 'dsad bought your an item: Top-Grade Life Stone: level 76 from the Market. You\'ve got: 10,000 LifeDrain Adena. Check your inventory!', '2019-07-08 19:46:06', '0');
INSERT INTO `character_mail` VALUES ('268474879', '13', '268493471', 'inbox', 'Vilmis', 'Market', 'dsad bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-07-08 19:46:27', '0');
INSERT INTO `character_mail` VALUES ('268493471', '14', '268493471', 'sentbox', 'Vilmis', 'Market', 'dsad bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-07-08 19:46:27', '0');
INSERT INTO `character_mail` VALUES ('268474879', '15', '268498060', 'inbox', 'Vilmis', 'Market', 'Kraken bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-13 23:57:58', '0');
INSERT INTO `character_mail` VALUES ('268498060', '16', '268498060', 'sentbox', 'Vilmis', 'Market', 'Kraken bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-13 23:57:58', '0');
INSERT INTO `character_mail` VALUES ('268482377', '17', '268498060', 'inbox', 'Admin', 'Market', 'Kraken bought your an item: Forgotten Blade - Haste from the Market. You\'ve got: 5 Event - Glittering Medal. Check your inventory!', '2019-07-14 03:18:58', '1');
INSERT INTO `character_mail` VALUES ('268498060', '18', '268498060', 'sentbox', 'Admin', 'Market', 'Kraken bought your an item: Forgotten Blade - Haste from the Market. You\'ve got: 5 Event - Glittering Medal. Check your inventory!', '2019-07-14 03:18:58', '0');
INSERT INTO `character_mail` VALUES ('268474879', '19', '268498060', 'inbox', 'Vilmis', 'Market', 'Kraken bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-14 03:19:09', '0');
INSERT INTO `character_mail` VALUES ('268498060', '20', '268498060', 'sentbox', 'Vilmis', 'Market', 'Kraken bought your an item: Heavens Divider - Focus from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-14 03:19:09', '0');
INSERT INTO `character_mail` VALUES ('268474879', '21', '268498060', 'inbox', 'Vilmis', 'Market', 'Kraken bought your an item: Imperial Crusader Breastplate from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-07-14 03:19:23', '0');
INSERT INTO `character_mail` VALUES ('268498060', '22', '268498060', 'sentbox', 'Vilmis', 'Market', 'Kraken bought your an item: Imperial Crusader Breastplate from the Market. You\'ve got: 1 Adena. Check your inventory!', '2019-07-14 03:19:23', '0');
INSERT INTO `character_mail` VALUES ('268474879', '23', '268498060', 'inbox', 'Vilmis', 'Market', 'Kraken bought your an item: Sword of Revolution from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-14 03:19:28', '0');
INSERT INTO `character_mail` VALUES ('268498060', '24', '268498060', 'sentbox', 'Vilmis', 'Market', 'Kraken bought your an item: Sword of Revolution from the Market. You\'ve got: 1 LifeDrain Adena. Check your inventory!', '2019-07-14 03:19:28', '0');
