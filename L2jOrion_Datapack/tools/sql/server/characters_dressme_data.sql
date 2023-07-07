SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `characters_dressme_data`;
CREATE TABLE `characters_dressme_data` (
  `obj_Id` decimal(11,0) NOT NULL DEFAULT '0',
  `armor_skins` varchar(255) DEFAULT NULL,
  `armor_skin_option` int(10) DEFAULT '0',
  `weapon_skins` varchar(255) DEFAULT NULL,
  `weapon_skin_option` int(10) DEFAULT '0',
  `hair_skins` varchar(255) DEFAULT NULL,
  `hair_skin_option` int(10) DEFAULT '0',
  `face_skins` varchar(255) DEFAULT NULL,
  `face_skin_option` int(10) DEFAULT '0',
  PRIMARY KEY (`obj_Id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
