# Rewritten by RayzoR

import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "178_IconicTrinity"

#NPC'S
KEKROPUS = 32138
ICONPAST = 32255
ICONPRESENT = 32256
ICONFUTURE = 32257

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
 
 def onEvent (self,event,st) :
     htmltext = event
     player = st.getPlayer()
     passwrd = st.getInt("pass")
     if event == "32138-03.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
     elif event == "32138-07.htm" :
       st.giveItems(956,1)
       st.exitQuest(False)
       st.playSound("ItemSound.quest_finish")
       st.addExpAndSp(20123,976)
     elif event == "32255-03.htm" :
       st.set("pass","0")
     elif event == "32255-04a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32255-04.htm"
     elif event == "32255-05a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32255-05.htm"
     elif event == "32255-06a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32255-06.htm"
     elif event == "32255-07a.htm" :
       st.set("pass",str(passwrd+1))
       if st.getInt("pass") != 4:
          return "32255-07.htm"
     elif event == "32255-12.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
       st.set("pass","0")
     elif event == "32256-03.htm" :
       st.set("pass","0")
     elif event == "32256-04a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32256-04.htm"
     elif event == "32256-05a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32256-05.htm"
     elif event == "32256-06a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32256-06.htm"
     elif event == "32256-07a.htm" :
       st.set("pass",str(passwrd+1))
       if st.getInt("pass") != 4:
          return "32256-07.htm"
     elif event == "32256-13.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
       st.set("pass","0")
     elif event == "32257-03.htm" :
       st.set("pass","0")
     elif event == "32257-04a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32257-04.htm"
     elif event == "32257-05a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32257-05.htm"
     elif event == "32257-06a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32257-06.htm"
     elif event == "32257-07a.htm" :
       st.set("pass",str(passwrd+1))
       htmltext = "32257-07.htm"
     elif event == "32257-08a.htm" :
       st.set("pass",str(passwrd+1))
       if st.getInt("pass") != 5:
          return "32257-08.htm"
     elif event == "32257-11.htm" :
       st.set("cond","4")
       st.playSound("ItemSound.quest_middle")
       st.set("pass","0")
     return htmltext

 def onTalk (self,npc,player):
     npcId = npc.getNpcId()
     htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
     st = player.getQuestState(qn)
     if not st : return htmltext
     id = st.getState()
     cond = st.getInt("cond")
     if id == COMPLETED :
       htmltext = "<html><body>This quest has already been completed.</body></html>"
     elif id == CREATED and npcId == KEKROPUS :
       if player.getRace().ordinal() != 5 :
         return "32138-02.htm"
       if st.getPlayer().getLevel() >= 17:
         htmltext = "32138-01.htm"
       else :
         htmltext = "32138-02a.htm"
         st.exitQuest(1)
     elif id == STARTED :
       if npcId == KEKROPUS :
         if cond == 1 :
           htmltext = "32138-04.htm"
         elif cond == 4 :
           htmltext = "32138-05.htm"
       elif npcId == ICONPAST :
         if cond == 1:
           htmltext = "32255-01.htm"
         elif cond == 2:
           htmltext = "32255-13.htm"
       elif npcId == ICONPRESENT :
         if cond == 2:
           htmltext = "32256-01.htm"
         elif cond == 3:
           htmltext = "32256-14.htm"
       elif npcId == ICONFUTURE :
         if cond == 3:
           htmltext = "32257-01.htm"
         elif cond == 4:
           htmltext = "32257-12.htm"
     return htmltext

QUEST       = Quest(178, qn, "Iconic Trinity")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(KEKROPUS)

QUEST.addTalkId(KEKROPUS)
QUEST.addTalkId(ICONPAST)
QUEST.addTalkId(ICONPRESENT)
QUEST.addTalkId(ICONFUTURE)
