
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `forums`;
CREATE TABLE `forums` (
  `forum_id` int(8) NOT NULL DEFAULT '0',
  `forum_name` varchar(255) NOT NULL DEFAULT '',
  `forum_parent` int(8) NOT NULL DEFAULT '0',
  `forum_post` int(8) NOT NULL DEFAULT '0',
  `forum_type` int(8) NOT NULL DEFAULT '0',
  `forum_perm` int(8) NOT NULL DEFAULT '0',
  `forum_owner_id` int(8) NOT NULL DEFAULT '0',
  UNIQUE KEY `forum_id` (`forum_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `forums` VALUES ('1', 'NormalRoot', '0', '0', '0', '1', '0');
INSERT INTO `forums` VALUES ('2', 'ClanRoot', '0', '0', '0', '0', '0');
INSERT INTO `forums` VALUES ('3', 'MemoRoot', '0', '0', '0', '0', '0');
INSERT INTO `forums` VALUES ('4', 'MailRoot', '0', '0', '0', '0', '0');
