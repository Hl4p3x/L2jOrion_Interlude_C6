
SET FOREIGN_KEY_CHECKS=0;

DROP TABLE IF EXISTS `auto_chat`;
CREATE TABLE `auto_chat` (
  `groupId` int(11) NOT NULL DEFAULT '0',
  `npcId` int(11) NOT NULL DEFAULT '0',
  `chatDelay` bigint(20) NOT NULL DEFAULT '-1',
  PRIMARY KEY (`groupId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `auto_chat` VALUES ('1', '31093', '-1');
INSERT INTO `auto_chat` VALUES ('2', '31172', '-1');
INSERT INTO `auto_chat` VALUES ('3', '31174', '-1');
INSERT INTO `auto_chat` VALUES ('4', '31176', '-1');
INSERT INTO `auto_chat` VALUES ('5', '31178', '-1');
INSERT INTO `auto_chat` VALUES ('6', '31180', '-1');
INSERT INTO `auto_chat` VALUES ('7', '31182', '-1');
INSERT INTO `auto_chat` VALUES ('8', '31184', '-1');
INSERT INTO `auto_chat` VALUES ('9', '31186', '-1');
INSERT INTO `auto_chat` VALUES ('10', '31188', '-1');
INSERT INTO `auto_chat` VALUES ('11', '31190', '-1');
INSERT INTO `auto_chat` VALUES ('12', '31192', '-1');
INSERT INTO `auto_chat` VALUES ('13', '31194', '-1');
INSERT INTO `auto_chat` VALUES ('14', '31196', '-1');
INSERT INTO `auto_chat` VALUES ('15', '31198', '-1');
INSERT INTO `auto_chat` VALUES ('16', '31200', '-1');
INSERT INTO `auto_chat` VALUES ('17', '31094', '-1');
INSERT INTO `auto_chat` VALUES ('18', '31173', '-1');
INSERT INTO `auto_chat` VALUES ('19', '31175', '-1');
INSERT INTO `auto_chat` VALUES ('20', '31177', '-1');
INSERT INTO `auto_chat` VALUES ('21', '31179', '-1');
INSERT INTO `auto_chat` VALUES ('22', '31181', '-1');
INSERT INTO `auto_chat` VALUES ('23', '31183', '-1');
INSERT INTO `auto_chat` VALUES ('24', '31185', '-1');
INSERT INTO `auto_chat` VALUES ('25', '31187', '-1');
INSERT INTO `auto_chat` VALUES ('26', '31189', '-1');
INSERT INTO `auto_chat` VALUES ('27', '31191', '-1');
INSERT INTO `auto_chat` VALUES ('28', '31193', '-1');
INSERT INTO `auto_chat` VALUES ('29', '31195', '-1');
INSERT INTO `auto_chat` VALUES ('30', '31197', '-1');
INSERT INTO `auto_chat` VALUES ('31', '31199', '-1');
INSERT INTO `auto_chat` VALUES ('32', '31201', '-1');
