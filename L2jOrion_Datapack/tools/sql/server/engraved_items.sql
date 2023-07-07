
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `engraved_items`;
CREATE TABLE `engraved_items` (
  `object_id` int(11) NOT NULL,
  `item_id` int(11) NOT NULL,
  `engraver_id` int(11) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
