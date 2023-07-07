import sys
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "2400_toivortex_exit"

NPC=[29055]

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
    st = player.getQuestState(qn)
    chance = st.getRandom(3)
    if chance == 0:
       x=108784+st.getRandom(100)
       y=16000+st.getRandom(100)
       z=-4928
    elif chance == 1:
       x=113824+st.getRandom(100)
       y=10448+st.getRandom(100)
       z=-5164
    else:
       x=115488+st.getRandom(100)
       y=22096+st.getRandom(100)
       z=-5168
    player.teleToLocation(x,y,z)
    st.exitQuest(1)
    return

QUEST       = Quest(2400,qn,"Teleports")
CREATED     = State('Start', QUEST)

QUEST.setInitialState(CREATED)

for item in NPC:
   QUEST.addStartNpc(item)
   QUEST.addTalkId(item)