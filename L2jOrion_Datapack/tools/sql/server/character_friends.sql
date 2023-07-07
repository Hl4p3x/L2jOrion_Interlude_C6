
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `character_friends`;
CREATE TABLE `character_friends` (
  `char_id` int(10) unsigned NOT NULL DEFAULT '0',
  `friend_id` int(10) unsigned NOT NULL DEFAULT '0',
  `relation` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`char_id`,`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
