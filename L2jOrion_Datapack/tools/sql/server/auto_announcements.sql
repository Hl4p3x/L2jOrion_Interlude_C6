
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `auto_announcements`;
CREATE TABLE `auto_announcements` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `announcement` varchar(255) NOT NULL,
  `delay` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
