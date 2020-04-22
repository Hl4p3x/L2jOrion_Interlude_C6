import sys
from l2jorion.game.ai import CtrlIntention
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.util.random import Rnd

FrozenId = {
    #Head Mob ID [Minion ID, Minion count]
    22094:[22093,5],
    22088:[22087,5]
}

class frozen(JQuest) :

    def __init__(self,id,name,descr):
        self.AlwaysSpawn = False
        JQuest.__init__(self,id,name,descr)

    def onAttack(self,npc,player,isPet,damage):
        npcId = npc.getNpcId()
        NewMob,mob_count = FrozenId[npcId]
        if FrozenId.has_key(npcId) :
            if self.AlwaysSpawn == True:
                return
            elif self.AlwaysSpawn == False and player.isAttackingNow():
                count = 0
                self.AlwaysSpawn = True
                while count < mob_count:
                    New = self.addSpawn(NewMob,npc.getX(), npc.getY(), npc.getZ(),npc.getHeading(),True,0)
                    New.addDamageHate(player,0,999)
                    New.getAI().setIntention(CtrlIntention.AI_INTENTION_ATTACK, player)
                    count = count+1
        return 

    def onKill (self,npc,player,isPet):
        npcId = npc.getNpcId()
        NewMob,mob_count = FrozenId[npcId]
        if FrozenId.has_key(npcId) :
            self.AlwaysSpawn = False
        return

QUEST = frozen(-1,"frozen","ai")

CREATED = State('Start', QUEST)
QUEST.setInitialState(CREATED)

for i in FrozenId.keys() :
   QUEST.addAttackId(i)

for j in FrozenId.keys() :
   QUEST.addKillId(j)