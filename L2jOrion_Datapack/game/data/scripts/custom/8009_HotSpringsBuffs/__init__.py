import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.datatables import SkillTable
from l2jorion.game.model import L2Effect
from l2jorion.util.random import Rnd
	
qn = "8009_HotSpringsBuffs"

class Quest (JQuest) :
	
 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)
        
 def onAttack (self,npc,player,damage,isPet):
    npcId = npc.getNpcId()
    if npcId in [21316]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4552)):
            holera = player.getFirstEffect(int(4552)).getLevel()
            if (Rnd.get(100) < 30):
              if holera < 10:
                newholera = int(holera + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4552,newholera))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4552,1))
        else:
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
	npcId = npc.getNpcId()
    elif npcId in [21321]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4551)):
            rheumatism = player.getFirstEffect(int(4551)).getLevel()
            if (Rnd.get(100) < 30):
              if rheumatism < 10:
                newrheumatism = int(rheumatism + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4551,newrheumatism))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4551,1))
        else:
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
	npcId = npc.getNpcId()
    elif npcId in [21314]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4551)):
            rheumatism = player.getFirstEffect(int(4551)).getLevel()
            if (Rnd.get(100) < 30):
              if rheumatism < 10:
                newrheumatism = int(rheumatism + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4551,newrheumatism))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4551,1))
        else:
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
	npcId = npc.getNpcId()
    elif npcId in [21319]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4552)):
            holera = player.getFirstEffect(int(4552)).getLevel()
            if (Rnd.get(100) < 30):
              if holera < 10:
                newholera = int(holera + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4552,newholera))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4552,1))
        else:
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
	npcId = npc.getNpcId()
    elif npcId in [21317]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
        else:
          if player.getFirstEffect(int(4553)):
            flu = player.getFirstEffect(int(4553)).getLevel()
            if (Rnd.get(100) < 15):
              if flu < 10:
                newflu = int(flu + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4553,newflu))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4553,1))
	npcId = npc.getNpcId()
    elif npcId in [21322]:
      if (Rnd.get(2) == 1):
        if (Rnd.get(2) == 1):
          if player.getFirstEffect(int(4554)):
            malaria = player.getFirstEffect(int(4554)).getLevel()
            if (Rnd.get(100) < 15):
              if malaria < 10:
                newmalaria = int(malaria + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4554,newmalaria))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4554,1))
        else:
          if player.getFirstEffect(int(4553)):
            flu = player.getFirstEffect(int(4553)).getLevel()
            if (Rnd.get(100) < 15):
              if flu < 10:
                newflu = int(flu + 1)
                npc.setTarget(player)
                npc.doCast(SkillTable.getInstance().getInfo(4553,newflu))
          else:
            npc.setTarget(player)
            npc.doCast(SkillTable.getInstance().getInfo(4553,1))
    return 
        
QUEST = Quest(8009,qn,"custom")

for i in [21316,21321,21314,21319,21317,21322]: 
  QUEST.addAttackId(i)