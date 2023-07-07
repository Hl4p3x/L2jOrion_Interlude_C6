
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `market_items`;
CREATE TABLE `market_items` (
  `ownerId` varchar(45) NOT NULL DEFAULT '',
  `ownerName` varchar(35) NOT NULL,
  `itemName` varchar(200) NOT NULL,
  `enchLvl` varchar(45) NOT NULL,
  `itemGrade` varchar(45) NOT NULL,
  `shopType` varchar(45) NOT NULL DEFAULT '',
  `itemType` varchar(45) NOT NULL,
  `itemId` varchar(45) NOT NULL DEFAULT '',
  `itemObjId` varchar(45) NOT NULL DEFAULT '',
  `count` varchar(45) NOT NULL DEFAULT '1',
  `priceItem` varchar(10) NOT NULL DEFAULT '',
  `price` varchar(10) NOT NULL DEFAULT '',
  `augmentationId` varchar(10) NOT NULL DEFAULT '',
  `augmentationSkill` varchar(10) NOT NULL DEFAULT '',
  `augmentationSkillLevel` varchar(10) NOT NULL DEFAULT '',
  `augmentationBonus` varchar(200) NOT NULL,
  PRIMARY KEY (`ownerId`,`ownerName`,`itemId`,`itemObjId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
