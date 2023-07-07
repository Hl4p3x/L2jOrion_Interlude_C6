
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `armorsets`;
CREATE TABLE `armorsets` (
  `id` int(3) NOT NULL AUTO_INCREMENT,
  `chest` decimal(11,0) NOT NULL DEFAULT '0',
  `legs` decimal(11,0) NOT NULL DEFAULT '0',
  `head` decimal(11,0) NOT NULL DEFAULT '0',
  `gloves` decimal(11,0) NOT NULL DEFAULT '0',
  `feet` decimal(11,0) NOT NULL DEFAULT '0',
  `skill_id` decimal(11,0) NOT NULL DEFAULT '0',
  `shield` decimal(11,0) NOT NULL DEFAULT '0',
  `shield_skill_id` decimal(11,0) NOT NULL DEFAULT '0',
  `enchant6skill` decimal(11,0) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`,`chest`)
) ENGINE=MyISAM AUTO_INCREMENT=52 DEFAULT CHARSET=latin1;

INSERT INTO `armorsets` VALUES ('1', '23', '2386', '43', '0', '0', '3500', '0', '0', '0');
INSERT INTO `armorsets` VALUES ('2', '1101', '1104', '44', '0', '0', '3501', '0', '0', '0');
INSERT INTO `armorsets` VALUES ('3', '58', '59', '47', '0', '0', '3502', '628', '3543', '3611');
INSERT INTO `armorsets` VALUES ('4', '352', '2378', '2411', '0', '0', '3506', '2493', '3544', '3611');
INSERT INTO `armorsets` VALUES ('5', '394', '416', '0', '0', '2422', '3503', '0', '0', '3612');
INSERT INTO `armorsets` VALUES ('6', '395', '417', '0', '0', '2424', '3505', '0', '0', '3612');
INSERT INTO `armorsets` VALUES ('7', '436', '469', '0', '2447', '0', '3504', '0', '0', '3613');
INSERT INTO `armorsets` VALUES ('8', '437', '470', '0', '2450', '0', '3507', '0', '0', '3613');
INSERT INTO `armorsets` VALUES ('9', '354', '381', '2413', '0', '0', '3509', '2495', '3545', '3614');
INSERT INTO `armorsets` VALUES ('10', '60', '0', '517', '0', '0', '3512', '107', '3546', '3614');
INSERT INTO `armorsets` VALUES ('11', '356', '0', '2414', '0', '0', '3516', '2497', '3547', '3614');
INSERT INTO `armorsets` VALUES ('12', '397', '2387', '0', '0', '62', '3508', '0', '0', '3615');
INSERT INTO `armorsets` VALUES ('13', '398', '418', '0', '0', '2431', '3511', '0', '0', '3615');
INSERT INTO `armorsets` VALUES ('14', '400', '420', '0', '0', '2436', '3514', '0', '0', '3615');
INSERT INTO `armorsets` VALUES ('15', '401', '0', '0', '0', '2437', '3515', '0', '0', '3615');
INSERT INTO `armorsets` VALUES ('16', '439', '471', '0', '2454', '0', '3510', '0', '0', '3616');
INSERT INTO `armorsets` VALUES ('17', '441', '472', '0', '2459', '0', '3513', '0', '0', '3616');
INSERT INTO `armorsets` VALUES ('18', '442', '473', '0', '2463', '0', '3517', '0', '0', '3616');
INSERT INTO `armorsets` VALUES ('19', '357', '383', '503', '5710', '5726', '3518', '0', '0', '3617');
INSERT INTO `armorsets` VALUES ('20', '2384', '2388', '503', '5711', '5727', '3520', '0', '0', '3618');
INSERT INTO `armorsets` VALUES ('21', '2397', '2402', '503', '5712', '5728', '3522', '0', '0', '3619');
INSERT INTO `armorsets` VALUES ('22', '2376', '2379', '2415', '5714', '5730', '3519', '673', '3548', '3617');
INSERT INTO `armorsets` VALUES ('23', '2390', '0', '2415', '5715', '5731', '3521', '0', '0', '3618');
INSERT INTO `armorsets` VALUES ('24', '2406', '0', '2415', '5716', '5732', '3523', '0', '0', '3619');
INSERT INTO `armorsets` VALUES ('25', '358', '2380', '2416', '5718', '5734', '3524', '0', '0', '3617');
INSERT INTO `armorsets` VALUES ('26', '2391', '0', '2416', '5719', '5735', '3526', '0', '0', '3618');
INSERT INTO `armorsets` VALUES ('27', '2398', '2403', '2416', '5720', '5736', '3528', '0', '0', '3619');
INSERT INTO `armorsets` VALUES ('28', '2381', '0', '2417', '5722', '5738', '3525', '110', '3549', '3617');
INSERT INTO `armorsets` VALUES ('29', '2392', '0', '2417', '5723', '5739', '3527', '0', '0', '3618');
INSERT INTO `armorsets` VALUES ('30', '2399', '2404', '2417', '5724', '5740', '3529', '0', '0', '3619');
INSERT INTO `armorsets` VALUES ('31', '365', '388', '512', '5765', '5777', '3530', '641', '3550', '3620');
INSERT INTO `armorsets` VALUES ('32', '2385', '2389', '512', '5766', '5778', '3532', '0', '0', '3621');
INSERT INTO `armorsets` VALUES ('33', '2407', '0', '512', '5767', '5779', '3535', '0', '0', '3622');
INSERT INTO `armorsets` VALUES ('34', '2382', '0', '547', '5768', '5780', '3531', '0', '0', '3620');
INSERT INTO `armorsets` VALUES ('35', '2393', '0', '547', '5769', '5781', '3533', '0', '0', '3621');
INSERT INTO `armorsets` VALUES ('36', '2400', '2405', '547', '5770', '5782', '3534', '0', '0', '3622');
INSERT INTO `armorsets` VALUES ('37', '374', '0', '2418', '5771', '5783', '3536', '2498', '3551', '3620');
INSERT INTO `armorsets` VALUES ('38', '2394', '0', '2418', '5772', '5784', '3538', '0', '0', '3621');
INSERT INTO `armorsets` VALUES ('39', '2408', '0', '2418', '5773', '5785', '3540', '0', '0', '3622');
INSERT INTO `armorsets` VALUES ('40', '2383', '0', '2419', '5774', '5786', '3537', '0', '0', '3620');
INSERT INTO `armorsets` VALUES ('41', '2395', '0', '2419', '5775', '5787', '3539', '0', '0', '3621');
INSERT INTO `armorsets` VALUES ('42', '2409', '0', '2419', '5776', '5788', '3541', '0', '0', '3622');
INSERT INTO `armorsets` VALUES ('43', '6373', '6374', '6378', '6375', '6376', '3553', '6377', '3554', '3623');
INSERT INTO `armorsets` VALUES ('44', '6379', '0', '6382', '6380', '6381', '3555', '0', '0', '3624');
INSERT INTO `armorsets` VALUES ('45', '6383', '0', '6386', '6384', '6385', '3556', '0', '0', '3625');
INSERT INTO `armorsets` VALUES ('46', '7851', '0', '7850', '7852', '7853', '3605', '0', '0', '3611');
INSERT INTO `armorsets` VALUES ('47', '7854', '0', '7850', '7855', '7856', '3606', '0', '0', '3612');
INSERT INTO `armorsets` VALUES ('48', '7857', '0', '7850', '7858', '7859', '3607', '0', '0', '3613');
INSERT INTO `armorsets` VALUES ('49', '7861', '0', '7860', '7862', '7863', '3608', '0', '0', '3620');
INSERT INTO `armorsets` VALUES ('50', '7864', '0', '7860', '7865', '7866', '3609', '0', '0', '3621');
INSERT INTO `armorsets` VALUES ('51', '7867', '0', '7860', '7868', '7869', '3610', '0', '0', '3622');
