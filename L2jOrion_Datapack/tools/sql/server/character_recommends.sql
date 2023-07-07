
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_recommends`;
CREATE TABLE `character_recommends` (
  `char_id` int(11) NOT NULL DEFAULT '0',
  `target_id` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_id`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
