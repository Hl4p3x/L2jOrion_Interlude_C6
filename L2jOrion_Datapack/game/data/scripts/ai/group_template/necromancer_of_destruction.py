import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.util.random import Rnd

class necromancer_of_destruction(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.necromancer_of_destruction = 21384
        self.necromancer_of_destruction_b = 21654
        JQuest.__init__(self,id,name,descr) 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.necromancer_of_destruction:
            if Rnd.get(100) <= 20:
                self.addSpawn(self.necromancer_of_destruction_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.necromancer_of_destruction_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.necromancer_of_destruction_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.necromancer_of_destruction_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.necromancer_of_destruction_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
        return 

QUEST = necromancer_of_destruction(-1,"necromancer_of_destruction","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.necromancer_of_destruction)