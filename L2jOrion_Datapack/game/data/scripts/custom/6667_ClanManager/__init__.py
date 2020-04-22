import sys
from l2jorion.game.model.actor.instance import L2PcInstance
from l2jorion.game.model.actor.instance import L2NpcInstance
from java.util import Iterator
from l2jorion.util.database import L2DatabaseFactory
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "6667_ClanManager"

NPC=[66667]
REQUESTED_ITEM=3470
REQUESTED_AMOUNT=2
NEW_REP_SCORE=3000000
QuestId     = 6667
QuestName   = "ClanManager"
QuestDesc   = "custom"
InitialHtml = "66667-1.htm"

print "INFO  Clan Manager (66667) Enabled..."

class Quest (JQuest) :

	def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

	def onEvent(self,event,st):
               htmltext = "<html><head><body>I have nothing to say you</body></html>"
               count=st.getQuestItemsCount(REQUESTED_ITEM)
               if event == "66667-clanOk.htm" :
                   if st.getPlayer().isClanLeader() and st.getPlayer().getClan().getLevel()<8:
                       if st.getPlayer().isNoble() and count >= REQUESTED_AMOUNT:
                            htmltext=event
                            st.getPlayer().getClan().changeLevel(8)
                            st.playSound("ItemSound.quest_finish")
                            st.takeItems(REQUESTED_ITEM,REQUESTED_AMOUNT)
                       else :
                            htmltext="66667-no_clan.htm"
                            st.exitQuest(1)
                   else :
                       htmltext="66667-no_clan.htm"
                       st.exitQuest(1)
               elif event == "66667-repOk.htm" :
                   if st.getPlayer().isClanLeader() and st.getPlayer().getClan().getLevel() >= 5 and st.getPlayer().getClan().getReputationScore() < NEW_REP_SCORE :
                       if st.getPlayer().isNoble() and count > REQUESTED_AMOUNT:
                            htmltext=event
                            st.getPlayer().getClan().setReputationScore(NEW_REP_SCORE, 1);
                            st.playSound("ItemSound.quest_finish")
                            st.takeItems(REQUESTED_ITEM,REQUESTED_AMOUNT)
                       else :
                            htmltext="66667-no_points.htm"
                            st.exitQuest(1)
                   else :
                       htmltext="66667-no_points.htm"
                       st.exitQuest(1)
               return htmltext

	def onTalk (self,npc,player):
	   htmltext = "<html><head><body>I have nothing to say you</body></html>"
           st = player.getQuestState(qn)
           if not st : return htmltext
           npcId = npc.getNpcId()
           id = st.getState()
           if id == CREATED :
               htmltext="66667-1.htm"
           elif id == COMPLETED :
               htmltext = "<html><head><body>This quest have already been completed.</body></html>"
           else :
               st.exitQuest(1)
           return htmltext


QUEST = Quest(6667,qn,"custom")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)
QUEST.setInitialState(CREATED)

for npcId in NPC:
 QUEST.addStartNpc(npcId)
 QUEST.addTalkId(npcId)
