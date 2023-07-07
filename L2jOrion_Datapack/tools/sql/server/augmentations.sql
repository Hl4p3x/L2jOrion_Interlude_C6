
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `augmentations`;
CREATE TABLE `augmentations` (
  `item_id` int(11) NOT NULL DEFAULT '0',
  `attributes` int(11) DEFAULT '0',
  `skill` int(11) DEFAULT '0',
  `level` int(11) DEFAULT '0',
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `augmentations` VALUES ('268475000', '591138335', '0', '0');
INSERT INTO `augmentations` VALUES ('268475050', '592842293', '0', '0');
