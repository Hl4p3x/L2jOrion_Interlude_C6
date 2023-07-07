
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `access_levels`;
CREATE TABLE `access_levels` (
  `accessLevel` mediumint(9) NOT NULL,
  `name` varchar(255) NOT NULL DEFAULT '',
  `nameColor` char(6) NOT NULL DEFAULT 'FFFFFF',
  `useNameColor` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `titleColor` char(6) NOT NULL DEFAULT 'FFFFFF',
  `useTitleColor` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `isGm` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `allowPeaceAttack` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `allowFixedRes` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `allowTransaction` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `allowAltg` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `giveDamage` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `takeAggro` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `gainExp` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `canDisableGmStatus` tinyint(1) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`accessLevel`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `access_levels` VALUES ('1', 'Master Access', '0000FF', '1', '0000FF', '0', '1', '1', '1', '1', '1', '1', '1', '1', '1');
INSERT INTO `access_levels` VALUES ('2', 'Head GM', '009900', '1', '009900', '1', '1', '1', '1', '1', '1', '1', '1', '1', '1');
INSERT INTO `access_levels` VALUES ('3', 'Event GM', '00FFFF', '1', '00FFFF', '0', '1', '1', '1', '0', '1', '0', '0', '0', '0');
INSERT INTO `access_levels` VALUES ('4', 'Support GM', '00FFFF', '1', '00FFFF', '0', '1', '0', '1', '0', '1', '0', '0', '0', '0');
INSERT INTO `access_levels` VALUES ('5', 'General GM', '00FFFF', '0', '00FFFF', '0', '1', '0', '0', '0', '0', '0', '0', '0', '0');
