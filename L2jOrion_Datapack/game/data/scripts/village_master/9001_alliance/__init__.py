#
# Created by DraX on 2005.08.12
# minor fixes by DrLecter 2005.09.10

import sys

from l2jorion.game.model.quest        import State
from l2jorion.game.model.quest        import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
qn = "9001_alliance"
#print "9001. Alliance"
NPC=[30026,30031,30037,30066,30070,30109,30115,30120,30154,30174,30175,30176,30187,30191,30195,30288,30289,30290,30297,30358,30373,30462,30474,30498,30499,30500,30503,30504,30505,30508,30511,30512,30513,30520,30525,30565,30594,30595,30676,30677,30681,30685,30687,30689,30694,30699,30704,30845,30847,30849,30854,30857,30862,30865,30894,30897,30900,30905,30910,30913,31269,31272,31276,31279,31285,31288,31314,31317,31321,31324,31326,31328,31331,31334,31755,31958,31961,31965,31968,31974,31977,31996,32092,32093,32094,32095,32096,32097,32098]

class Quest (JQuest) :

 def onEvent (self,event,st):
   ClanLeader = st.getPlayer().isClanLeader();
   Clan = st.getPlayer().getClanId();
   htmltext = event
   if event == "9001-01.htm": htmltext = "9001-01.htm"
   elif (Clan == 0):
    st.exitQuest(1);
    htmltext = "<html><body>You must be in Clan.</body></html";
   elif event == "9001-02.htm": htmltext = "9001-02.htm"
   return htmltext

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onTalk (Self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   ClanLeader = st.getPlayer().isClanLeader();
   Clan = st.getPlayer().getClan();
   if npcId in NPC:
     st.set("cond","0")
     st.setState(STARTED)
     return "9001-01.htm"

QUEST      = Quest(9001,qn,"village_master")
CREATED    = State('Start',     QUEST)
STARTED    = State('Started',   QUEST)
COMPLETED  = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

for item in NPC:
### Quest NPC starter initialization
   QUEST.addStartNpc(item)
### Quest NPC initialization
   QUEST.addTalkId(item)