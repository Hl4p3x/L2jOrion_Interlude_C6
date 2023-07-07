import sys
from l2jorion.game.model.actor.instance import      L2PcInstance
from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "1104_NewbieTravelToken"

TOKEN = 8542

DATA={
30600:[ 12160,  16554,-4583],#DE
30601:[115594,-177993, -912],#DW
30599:[ 45470,  48328,-3059],#EV
30602:[-45067,-113563, -199],#OV
30598:[-84053, 243343,-3729] #TI
}

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
   htmltext = event
   if event.isdigit():
      dest=int(event)
      if dest in DATA.keys():
         x,y,z=DATA[dest]
         if x and y and z:
            if st.getQuestItemsCount(TOKEN):
              st.takeItems(TOKEN,1)
              st.getPlayer().teleToLocation(x,y,z)
            else:
              st.exitQuest(1)
              return "Incorrect item count"
   st.exitQuest(1)
   return

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)  
   npcId = npc.getNpcId()
   if player.getLevel() >= 20:
     htmltext="1.htm"
     st.exitQuest(1)
   else:
     htmltext=str(npcId)+".htm"
   return htmltext

QUEST       = Quest(1104,qn,"Teleports")
CREATED     = State('Start',QUEST)

QUEST.setInitialState(CREATED)

for i in DATA.keys() :
    QUEST.addStartNpc(i)
    QUEST.addTalkId(i)