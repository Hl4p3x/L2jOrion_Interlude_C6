import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.util.random import Rnd

class assassin_beetle(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.assassin_beetle = 21381
        self.assassin_beetle_b = 21653
        JQuest.__init__(self,id,name,descr) 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.assassin_beetle:
            if Rnd.get(100) <= 20:
                self.addSpawn(self.assassin_beetle_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.assassin_beetle_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.assassin_beetle_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.assassin_beetle_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
                self.addSpawn(self.assassin_beetle_b,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),False,0)
        return 

QUEST = assassin_beetle(-1,"assassin_beetle","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.assassin_beetle)