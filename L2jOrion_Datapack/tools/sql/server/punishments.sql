
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `punishments`;
CREATE TABLE `punishments` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(16) DEFAULT NULL,
  `reason` varchar(120) DEFAULT NULL,
  `type` varchar(100) DEFAULT NULL,
  `time` varchar(100) DEFAULT NULL,
  `date` varchar(100) DEFAULT NULL,
  `punisher` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
