import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.util.random import Rnd

class magma_drake(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.magma_drake = 21393
        self.magma_drake_b = 21657
        JQuest.__init__(self,id,name,descr) 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.magma_drake:
            if Rnd.get(100) <= 20:
                self.addSpawn(self.magma_drake_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.magma_drake_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.magma_drake_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.magma_drake_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.magma_drake_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
        return 

QUEST = magma_drake(-1,"magma_drake","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.magma_drake)