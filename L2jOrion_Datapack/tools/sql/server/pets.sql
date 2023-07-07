
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `pets`;
CREATE TABLE `pets` (
  `item_obj_id` decimal(11,0) NOT NULL DEFAULT '0',
  `name` varchar(16) DEFAULT NULL,
  `level` decimal(11,0) DEFAULT NULL,
  `curHp` decimal(18,0) DEFAULT NULL,
  `curMp` decimal(18,0) DEFAULT NULL,
  `exp` decimal(20,0) DEFAULT NULL,
  `sp` decimal(11,0) DEFAULT NULL,
  `karma` decimal(11,0) DEFAULT NULL,
  `pkkills` decimal(11,0) DEFAULT NULL,
  `fed` decimal(11,0) DEFAULT NULL,
  PRIMARY KEY (`item_obj_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

