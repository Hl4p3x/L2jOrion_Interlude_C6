import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

# Cats_Eye_Bandit
class cats_eye_bandit(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.cats_eye_bandit = 27038
        self.FirstAttacked = False
        # finally, don't forget to call the parent constructor to prepare the event triggering
        # mechanisms etc.
        JQuest.__init__(self,id,name,descr)

    def onAttack (self,npc,player,damage,isPet):
        objId=npc.getObjectId()
        if self.FirstAttacked:
           if Rnd.get(40) : return
           npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"You childish fool, do you think you can catch me?"))
        else :
           self.FirstAttacked = True
        return

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.cats_eye_bandit:
            objId=npc.getObjectId()
            if Rnd.get(80) : npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"I must do something about this shameful incident..."))

            self.FirstAttacked = False
        elif self.FirstAttacked :
            self.addSpawn(npcId, npc.getX(), npc.getY(), npc.getZ())
        return

QUEST = cats_eye_bandit(-1,"cats_eye_bandit","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.cats_eye_bandit)
QUEST.addAttackId(QUEST.cats_eye_bandit)