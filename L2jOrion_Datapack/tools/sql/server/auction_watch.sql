
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `auction_watch`;
CREATE TABLE `auction_watch` (
  `charObjId` int(11) NOT NULL DEFAULT '0',
  `auctionId` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`charObjId`,`auctionId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
