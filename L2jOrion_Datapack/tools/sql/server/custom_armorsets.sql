
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `custom_armorsets`;
CREATE TABLE `custom_armorsets` (
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
) ENGINE=MyISAM AUTO_INCREMENT=55 DEFAULT CHARSET=latin1;
