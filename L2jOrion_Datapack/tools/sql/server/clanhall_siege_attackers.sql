
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `clanhall_siege_attackers`;
CREATE TABLE `clanhall_siege_attackers` (
  `clanhall_id` int(3) NOT NULL DEFAULT '0',
  `attacker_id` int(10) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
