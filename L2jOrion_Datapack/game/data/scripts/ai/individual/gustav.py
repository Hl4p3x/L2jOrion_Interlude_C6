import sys
from l2jorion.game.ai import CtrlIntention
from l2jorion.game.model.entity.siege.clanhalls import DevastatedCastle
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.managers import ClanHallManager
from l2jorion.util.random import Rnd
from l2jorion.game.model import L2Clan
from l2jorion.game.model import L2ClanMember
from java.lang import System

GUSTAV = 35410
MESSENGER = 35420
CLANLEADERS = []

class Gustav(JQuest):

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (self,npc,player):
   global CLANLEADERS
   npcId = npc.getNpcId()
   if npcId == MESSENGER :
     for clname in CLANLEADERS:
       if player.getName() == clname :
         return "<html><body>You already registered!</body></html>"
     if DevastatedCastle.getInstance().Conditions(player) :
       CLANLEADERS.append(player.getName())
       return "<html><body>You have successful registered on a siege</body></html>"
     else:
       return "<html><body>Condition are not allow to do that!</body></html>"
   return
 
 def onAttack (self,npc,player,damage,isPet):
   CLAN = player.getClan()
   if CLAN == None :
     return
   CLANLEADER = CLAN.getLeader()
   if CLANLEADER == None :
     return
   global CLANLEADERS
   for clname in CLANLEADERS:
     if clname <> None :
       if CLANLEADER.getName() == clname :
         DevastatedCastle.getInstance().addSiegeDamage(CLAN,damage)
   return

 def onKill(self,npc,player,isPet):
   DevastatedCastle.getInstance().SiegeFinish()
   return

QUEST = Gustav(-1, "gustav", "ai")
CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addTalkId(MESSENGER)
QUEST.addStartNpc(MESSENGER)

QUEST.addAttackId(GUSTAV)
QUEST.addKillId(GUSTAV)