import sys
import time
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

# Cats_Eye_Bandit
class cats_eye_bandit(JQuest) :

    def __init__(self,id,name,descr):
        self.cats_eye_bandit = 27038
        JQuest.__init__(self,id,name,descr)

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        time.sleep(3)
        if npcId == self.cats_eye_bandit:
            objId=npc.getObjectId()
            npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"I must do something about this shameful incident..."))
        return

QUEST = cats_eye_bandit(-1,"cats_eye_bandit","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.cats_eye_bandit)