
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `global_tasks`;
CREATE TABLE `global_tasks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `task` varchar(50) NOT NULL DEFAULT '',
  `type` varchar(50) NOT NULL DEFAULT '',
  `last_activation` decimal(20,0) NOT NULL DEFAULT '0',
  `param1` varchar(100) NOT NULL DEFAULT '',
  `param2` varchar(100) NOT NULL DEFAULT '',
  `param3` varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8;

INSERT INTO `global_tasks` VALUES ('1', 'CleanUp', 'TYPE_FIXED_SHEDULED', '1571856268820', '1800000', '3600000', '');
INSERT INTO `global_tasks` VALUES ('2', 'OlympiadSave', 'TYPE_FIXED_SHEDULED', '1571254866473', '900000', '1800000', '');
INSERT INTO `global_tasks` VALUES ('3', 'sp_recommendations', 'TYPE_GLOBAL_TASK', '1571220000473', '1', '13:00:00', '');
INSERT INTO `global_tasks` VALUES ('4', 'SevenSignsUpdate', 'TYPE_FIXED_SHEDULED', '1571856269892', '1800000', '1800000', '');
INSERT INTO `global_tasks` VALUES ('5', 'Restart', 'TYPE_GLOBAL_TASK', '1571104800724', '5', '05:00:00', '300');
INSERT INTO `global_tasks` VALUES ('6', 'raid_points_reset', 'TYPE_GLOBAL_TASK', '1571173800471', '1', '00:10:00', '');
