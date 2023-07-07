import sys
from l2jorion.game.model.actor.instance import      L2PcInstance
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "1102_toivortex_blue"

BLUE_DIMENSION_STONE    = 4402
DIMENSION_VORTEX_1      = 30952
DIMENSION_VORTEX_3      = 30954

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   if npcId in [ DIMENSION_VORTEX_1,DIMENSION_VORTEX_3 ] :
     if st.getQuestItemsCount(BLUE_DIMENSION_STONE) >= 1 :
       st.takeItems(BLUE_DIMENSION_STONE,1)
       st.getPlayer().teleToLocation(114097,19935,935)
       st.exitQuest(1)
       return
     else :
       st.exitQuest(1)
       return "1.htm"

QUEST       = Quest(1102,qn,"Teleports")
CREATED     = State('Start',QUEST)

QUEST.setInitialState(CREATED)

for i in [DIMENSION_VORTEX_1,DIMENSION_VORTEX_3] :
   QUEST.addStartNpc(i)
   QUEST.addTalkId(i)