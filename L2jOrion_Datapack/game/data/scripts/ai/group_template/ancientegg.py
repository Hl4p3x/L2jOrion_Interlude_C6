import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.datatables import SkillTable
from java.lang import System

EGG = 18344

class AncientEgg(JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAttack (self,npc,player,damage,isPet):
   player.setTarget(player)
   player.doCast(SkillTable.getInstance().getInfo(5088,1))
   return

QUEST = AncientEgg(-1, "ancientegg", "ai")
CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addAttackId(EGG)
