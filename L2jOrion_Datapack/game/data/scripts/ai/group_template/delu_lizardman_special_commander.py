import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

# delu_lizardman_special_commander
class delu_lizardman_special_commander(JQuest) :

    # init function.  Add in here variables that you'd like to be inherited by subclasses (if any)
    def __init__(self,id,name,descr):
        self.delu_lizardman_special_commander = 21107
        self.FirstAttacked = False
        # finally, don't forget to call the parent constructor to prepare the event triggering
        # mechanisms etc.
        JQuest.__init__(self,id,name,descr)

    def onAttack (self,npc,player,damage,isPet):
        objId=npc.getObjectId()
        if self.FirstAttacked:
           if Rnd.get(40) : return
           npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"Come on, Ill take you on!"))
        else :
           self.FirstAttacked = True
           npc.broadcastPacket(CreatureSay(objId,0,npc.getName(),"How dare you interrupt a sacred duel! You must be taught a lesson!"))
        return 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        if npcId == self.delu_lizardman_special_commander:
            objId=npc.getObjectId()
            self.FirstAttacked = False
        elif self.FirstAttacked :
            self.addSpawn(npcId,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),True,0)
        return 

QUEST		= delu_lizardman_special_commander(-1,"delu_lizardman_special_commander","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

QUEST.addKillId(QUEST.delu_lizardman_special_commander)
QUEST.addAttackId(QUEST.delu_lizardman_special_commander)