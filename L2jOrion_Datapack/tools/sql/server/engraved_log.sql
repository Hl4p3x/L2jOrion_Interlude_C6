
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `engraved_log`;
CREATE TABLE `engraved_log` (
  `object_id` int(11) NOT NULL,
  `actiondate` decimal(12,0) NOT NULL,
  `process` varchar(64) NOT NULL,
  `itemName` varchar(64) NOT NULL,
  `form_char` varchar(64) NOT NULL,
  `to_char` varchar(64) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;
