
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `accounts_voted`;
CREATE TABLE `accounts_voted` (
  `vote_ip` varchar(20) NOT NULL DEFAULT '',
  `last_hop_vote` bigint(20) NOT NULL,
  `last_top_vote` bigint(20) NOT NULL,
  `last_net_vote` bigint(20) NOT NULL,
  `last_bra_vote` bigint(20) NOT NULL,
  `last_l2topgr` bigint(20) NOT NULL DEFAULT '0',
  `last_l2toponline` bigint(20) NOT NULL DEFAULT '0',
  PRIMARY KEY (`vote_ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT;

