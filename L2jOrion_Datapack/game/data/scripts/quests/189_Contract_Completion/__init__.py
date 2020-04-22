# Rewritten by RayzoR

import sys

from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "189_Contract_Completion"

#NPCs
Kusto = 30512
Lorain = 30673
Luka = 31437
Shegfield = 30068

#Items
Metal = 10370

class Quest (JQuest) :
    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)
        self.questItemIds = [Metal]

    def onAdvEvent (self,event,npc, player) :
        st = player.getQuestState(qn)
        if not st: return
        htmltext = event
        if event == "31437-02.htm" :
            st.playSound("ItemSound.quest_accept")
            st.set("cond","1")
            st.giveItems(Metal,1)
        elif event == "30673-02.htm" :
            st.playSound("ItemSound.quest_middle")
            st.set("cond","2")
            st.takeItems(Metal,-1)
        elif event == "30068-03.htm":
            st.set("cond","3")
            st.playSound("ItemSound.quest_middle")
        elif event == "30512-02.htm":
            if player.getLevel() < 50 :
               st.addExpAndSp(309467,20614)
            st.giveItems(57,121527)
            st.exitQuest(False)
            st.playSound("ItemSound.quest_finish")
        return htmltext

    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
        st = player.getQuestState(qn)
        if not st : return htmltext
        npcId = npc.getNpcId()
        id = st.getState()
        cond = st.getInt("cond")
        if id == COMPLETED:
            htmltext = "<html><body>This quest has already been completed.</body></html>"
        elif id == STARTED:
            if npcId == Luka:
                if not cond :
                    if player.getLevel() < 42 :
                        htmltext = "31437-00.htm"
                    else :
                        htmltext = "31437-01.htm"
                elif cond  == 1:
                    htmltext = "31437-03.htm"
            elif npcId == Lorain:
                if cond == 1 :
                    htmltext = "30673-01.htm"
                elif cond == 2 :
                    htmltext = "30673-03.htm"
                elif cond == 3 :
                    htmltext = "30673-04.htm"
                    st.set("cond","4")
                    st.playSound("ItemSound.quest_middle")
                elif cond == 4 :
                    htmltext = "30673-05.htm"
            elif npcId == Shegfield:
                if cond == 2 :
                    htmltext = "30068-01.htm"
                elif cond == 3 :
                    htmltext = "30068-04.htm"
            elif npcId == Kusto:
                if cond == 4 :
                    htmltext = "30512-01.htm"
        return htmltext

    def onFirstTalk (self,npc,player):
       st = player.getQuestState(qn)
       qs = player.getQuestState("186_Contract_Execution")
       if not st and qs and qs.getState() == COMPLETED:
           st = self.newQuestState(player)
           st.setState(STARTED)
       npc.showChatWindow(player)
       return None

QUEST       = Quest(189,qn,"Contract Completion")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addTalkId(Kusto)
QUEST.addTalkId(Lorain)
QUEST.addTalkId(Luka)
QUEST.addTalkId(Shegfield)
QUEST.addFirstTalkId(Luka)
