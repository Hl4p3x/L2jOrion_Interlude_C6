
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `buff_templates`;
CREATE TABLE `buff_templates` (
  `id` int(11) unsigned NOT NULL,
  `name` varchar(35) NOT NULL DEFAULT '',
  `skill_id` int(10) unsigned NOT NULL,
  `skill_name` varchar(35) DEFAULT NULL,
  `skill_level` int(10) unsigned NOT NULL DEFAULT '1',
  `skill_force` int(1) NOT NULL DEFAULT '1',
  `skill_order` int(10) unsigned NOT NULL,
  `char_min_level` int(10) unsigned NOT NULL DEFAULT '0',
  `char_max_level` int(10) unsigned NOT NULL DEFAULT '0',
  `premium` int(1) NOT NULL,
  `voter` int(1) NOT NULL,
  `useItem` int(1) NOT NULL,
  `itemId` int(4) NOT NULL,
  `itemCount` decimal(10,0) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`id`,`name`,`skill_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `buff_templates` VALUES ('155', 'WindWalkbyUsweer', '1204', 'Wind Walk', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('156', 'InvigorebyUsweer', '1032', 'Invigore', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('157', 'MentalShieldbyUsweer', '1035', 'Mental Shield', '4', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('158', 'MagicBarrierbyUsweer', '1036', 'Magic Barrier', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('159', 'ShieldbyUsweer', '1040', 'Shield', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('160', 'HilyWeaponbyUsweer', '1043', 'Hily Weapon', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('161', 'RegenerationbyUsweer', '1044', 'Regeneration', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('162', 'BlessedBodybyUsweer', '1045', 'Blessed Body', '6', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('163', 'BlessedSoulbyUsweer', '1048', 'Blessed Soul', '6', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('164', 'BerserkerSpiritbyUsweer', '1062', 'Berserker Spirit', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('165', 'MightbyUsweer', '1068', 'Might', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('166', 'FocusbyUsweer', '1077', 'Focus', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('167', 'ConcentrationbyUsweer', '1078', 'Concentration', '6', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('168', 'AcumenbyUsweer', '1085', 'Acumen', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('169', 'HastebyUsweer', '1086', 'Haste', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('170', 'AgilitybyUsweer', '1087', 'Agility', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('171', 'ResistAquabyUsweer', '1182', 'Resist Aqua', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('172', 'ResistWindbyUsweer', '1189', 'Resist Wind', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('173', 'ResistFirebyUsweer', '1191', 'Resist Fire', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('174', 'ResistPotionbyUsweer', '1033', 'Resist Potion', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('175', 'GuidancebyUsweer', '1240', 'Guidance', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('176', 'DeathWhisperbyUsweer', '1242', 'Death Whisper', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('177', 'BlessShieldbyUsweer', '1243', 'Bless Shield', '6', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('178', 'DecreaseWeightbyUsweer', '1257', 'Decrease Weight', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('179', 'ResistShockbyUsweer', '1259', 'Resist Shock', '4', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('180', 'WildMagicbyUsweer', '1303', 'Wild Magic', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('181', 'AdvancedBlockbyUsweer', '1304', 'Advanced Block', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('182', 'ElementalProtectionbyUsweer', '1352', 'Elemental Protection', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('183', 'DivineProtectionbyUsweer', '1353', 'Divine Protection', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('184', 'ArcaneProtectionbyUsweer', '1354', 'Arcane Protection', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('185', 'HolyResistancebyUsweer', '1392', 'Holy Resistance', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('186', 'UnHolyResistancebyUsweer', '1393', 'UnHoly Resistance', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('187', 'ClaritybyUsweer', '1397', 'Clarity', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('188', 'GreaterEmpowerbyUsweer', '1059', 'Greater Empower', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('189', 'VampiricRagebyUsweer', '1268', 'Vampiric Rage', '4', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('190', 'KissofEvebyUsweer', '1073', 'Kiss of Eve', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('191', 'DanceofWariorbyUsweer', '271', 'Dance of Warior', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('192', 'DanceofInspirationbyUsweer', '272', 'Dance of Inspiration', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('193', 'DanceofMysticbyUsweer', '273', 'Dance of Mystic', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('194', 'DanceofFirebyUsweer', '274', 'Dance of Fire', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('195', 'DanceofFurybyUsweer', '275', 'Dance of Fury', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('196', 'DanceofConcentrationbyUsweer', '276', 'Dance of Concentration', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('197', 'DanceofLightbyUsweer', '277', 'Dance of Light', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('198', 'DanceofAquaGuardbyUsweer', '307', 'Dance of Aqua Guard', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('199', 'DanceofEarthGuardbyUsweer', '309', 'Dance of Earth Guard', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('200', 'DanceoftheVampirebyUsweer', '310', 'Dance of the Vampire', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('201', 'DanceofProtectionbyUsweer', '311', 'Dance of Protection', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '-1');
INSERT INTO `buff_templates` VALUES ('202', 'SirensDancebyUsweer', '365', 'Sirens Dance', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('203', 'DanceofShadowbyUsweer', '366', 'Dance of Shadow', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('204', 'SongofEarthbyUsweer', '264', 'Song of Earth', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('205', 'SongofLifebyUsweer', '265', 'Song of Life', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('206', 'SongofWaterbyUsweer', '266', 'Song of Water', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('207', 'SongofWardingbyUsweer', '267', 'Song of Warding', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('208', 'SongofWindbyUsweer', '268', 'Song of Wind', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('209', 'SongofHunterbyUsweer', '269', 'Song of Hunter', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('210', 'SongofInvocationbyUsweer', '270', 'Song of Invocation', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('211', 'SongofVitalitybyUsweer', '304', 'Song of Vitality', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('212', 'SongofVengeancebyUsweer', '305', 'Song of Vengeance', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('213', 'SongofFlameGuardbyUsweer', '306', 'Song of Flame Guard', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('214', 'SongofStormGuardbyUsweer', '308', 'Song of Storm Guard', '1', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('215', 'SongofRenewalbyUsweer', '349', 'Song of Renewal', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('216', 'SongofMeditationbyUsweer', '363', 'Song of Meditation', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('217', 'SongofChampionbyUsweer', '364', 'Song of Champion', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('218', 'PaagrianGiftbyUsweer', '1003', 'Pa agrian Gift', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('219', 'BlessingofPaagriobyUsweer', '1005', 'Blessing of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('220', 'TheWisdomofPaagriobyUsweer', '1004', 'The Wisdom of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('221', 'TheGloryofPaagriobyUsweer', '1008', 'The Glory  of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('222', 'TheVisionofPaagriobyUsweer', '1249', 'The Vision of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('223', 'UnderTheProtectionofPaagriobyUsweer', '1250', 'Under The Protection  of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('224', 'TheTactofPaagriobyUsweer', '1260', 'The Tact  of Pa agrio', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('225', 'TheRageofPaagriobyUsweer', '1261', 'The Rage  of Pa agrio', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('226', 'PaagrioHastebyUsweer', '1282', ' Pa agrio Haste', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('227', 'TheEyeofPaagriobyUsweer', '1364', 'The Eye of Pa agrio', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('228', 'TheSoulofPaagriobyUsweer', '1365', 'The Soul of Pa agrio', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('229', 'VictoriesofPaagriobyUsweer', '1414', 'Victories  of Pa agrio', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('230', 'PaagrioFistbyUsweer', '1416', ' Pa agrio Fist', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('231', 'FlameChantbyUsweer', '1002', 'Flame Chant', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('232', 'ChantofFirebyUsweer', '1006', 'Chant of Fire', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('233', 'ChantofBattlebyUsweer', '1007', 'Chant of Battle', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('234', 'ChantofShieldingbyUsweer', '1009', 'Chant of Shielding', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('235', 'ChantofFurybyUsweer', '1251', 'Chant of Fury', '2', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('236', 'ChantofEvasionbyUsweer', '1252', 'Chant of Evasion', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('237', 'ChantofRagebyUsweer', '1253', 'Chant of Rage', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('238', 'ChantofRevengebyUsweer', '1284', 'Chant of Revenge', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '-1');
INSERT INTO `buff_templates` VALUES ('239', 'ChantofPredatorbyUsweer', '1308', 'Chant of Predator', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('240', 'ChantofEaglebyUsweer', '1309', 'Chant of Eagle', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('241', 'ChantofVampirebyUsweer', '1310', 'Chant of Vampire', '4', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('242', 'ChantofSpiritbyUsweer', '1362', 'Chant of Spirit', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('243', 'ProphecyofFirebyUsweer', '1356', 'Prophecy of Fire', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('244', 'ProphecyofWaterbyUsweer', '1355', 'Prophecy of Water', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('245', 'ProphecyofWindbyUsweer', '1357', 'Prophecy of Wind', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('246', 'ChantofVictorybyUsweer', '1363', 'Chant of Victory', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('247', 'GreateMightbyUsweer', '1388', 'Greate Might', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('248', 'GreateShieldbyUsweer', '1389', 'Greate Shield', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('249', 'WarChantbyUsweer', '1390', 'War Chant', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('250', 'EarthChantbyUsweer', '1391', 'Earth Chant', '3', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('251', 'BlessingofSeraphimbyUsweer', '4702', 'Blessing of Seraphim', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('252', 'GiftofSeraphimbyUsweer', '4703', 'Gift of Seraphim', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('253', 'BlessingofQueenbyUsweer', '4699', 'Blessing of Queen', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('254', 'GiftofQueenbyUsweer', '4700', 'Gift of Queen', '3', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('255', 'SummonLifebyUsweer', '67', 'Summon Life', '7', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('256', 'SummonAquabyUsweer', '1280', 'Summon Aqua', '9', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('257', 'SummonStormbyUsweer', '1328', 'Summon Storm', '8', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('258', 'SummonBindingbyUsweer', '1279', 'Summon Binding', '9', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('259', 'SummonSparkbyUsweer', '1281', 'Summon Spark', '9', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('260', 'SummonPhantombyUsweer', '33', 'Summon Phantom', '8', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('261', 'MagnusChantbyUsweer', '1413', 'Magnus Chant', '1', '1', '1', '0', '80', '0', '0', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('262', 'Rheumatism', '4551', 'Rheumatism', '4', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('263', 'Cholera', '4552', 'Cholera', '4', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('264', 'Flu', '4553', 'Flu', '4', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('265', 'Malaria', '4554', 'Malaria', '4', '1', '1', '0', '80', '1', '0', '0', '6393', '1');
INSERT INTO `buff_templates` VALUES ('266', 'RheumatismVotersBuff', '4551', 'Rheumatism', '4', '1', '1', '0', '80', '0', '1', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('267', 'CholeraVotersBuff', '4552', 'Cholera', '4', '1', '1', '0', '80', '0', '1', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('268', 'FluVotersBuff', '4553', 'Flu', '4', '1', '1', '0', '80', '0', '1', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('269', 'MalariaVotersBuff', '4554', 'Malaria', '4', '1', '1', '0', '80', '0', '1', '1', '57', '-1');
INSERT INTO `buff_templates` VALUES ('270', 'Noblesse', '1323', 'Noblesse', '1', '1', '1', '0', '0', '1', '0', '0', '6393', '-1');
