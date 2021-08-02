/*
Navicat MySQL Data Transfer

Source Server         : Localhost
Source Server Version : 50527
Source Host           : localhost:3306
Source Database       : lifedrain_db

Target Server Type    : MYSQL
Target Server Version : 50527
File Encoding         : 65001

Date: 2020-12-10 23:34:14
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `character_achievements`
-- ----------------------------
DROP TABLE IF EXISTS `character_achievements`;
CREATE TABLE `character_achievements` (
  `object_id` int(10) unsigned NOT NULL DEFAULT '0',
  `type` varchar(20) NOT NULL DEFAULT '',
  `level` tinyint(10) unsigned NOT NULL DEFAULT '0',
  `count` int(10) unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`object_id`,`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of character_achievements
-- ----------------------------
