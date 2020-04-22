import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

# karul_bugbear
class karul_bugbear(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.karul_bugbear = 20600
        self.FirstAttacked = False
        # finally, don't forget to call the parent constructor to prepare the event triggering
        # mechanisms etc.
        JQuest.__init__(self,id,name,descr)

    def onAttack (self,npc,player,damage,isPet):
        objId=npc.getObjectId()
        if self.FirstAttacked:
           if Rnd.get(4) : return
           npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Your rear is practically unguarded!"))
        else :
           self.FirstAttacked = True
           if Rnd.get(4) : return
           npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Watch your back!"))
        return 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.karul_bugbear:
            objId=npc.getObjectId()
            self.FirstAttacked = False
        elif self.FirstAttacked :
            self.addSpawn(npcId,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),True,0)
        return 

QUEST = karul_bugbear(-1,"karul_bugbear","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.karul_bugbear)
QUEST.addAttackId(QUEST.karul_bugbear)