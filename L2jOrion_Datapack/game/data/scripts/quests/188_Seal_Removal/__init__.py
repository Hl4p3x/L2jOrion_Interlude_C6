# Rewritten by RayzoR

import sys

from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "188_Seal_Removal"

#NPCs
Nikola = 30621
Lorain = 30673
Dorothy = 30970

#Items
BrokenMetal = 10369

class Quest (JQuest) :
    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)
        self.questItemIds = [BrokenMetal]

    def onAdvEvent (self,event,npc, player) :
        st = player.getQuestState(qn)
        if not st: return
        htmltext = event
        if event == "30673-02.htm" :
            st.playSound("ItemSound.quest_accept")
            st.set("cond","1")
            st.giveItems(BrokenMetal,1)
        elif event == "30621-03.htm":
            st.set("cond","2")
            st.playSound("ItemSound.quest_middle")
        elif event == "30970-03.htm":
            if player.getLevel() < 50 :
               st.addExpAndSp(285935,18711)
            st.giveItems(57,98583)
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
            if npcId == Lorain:
                if not cond :
                    if player.getLevel() < 41 :
                        htmltext = "30673-00.htm"
                    else :
                        htmltext = "30673-01.htm"
                elif cond == 1 :
                    htmltext = "30673-03.htm"
            elif npcId == Nikola :
                if cond == 1 :
                    htmltext = "30621-01.htm"
                elif cond == 2 :
                    htmltext = "30621-05.htm"
            elif npcId == Dorothy :
                if cond == 2 :
                    htmltext = "30970-01.htm"
        return htmltext

QUEST       = Quest(188,qn,"Seal Removal")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addTalkId(Nikola)
QUEST.addTalkId(Lorain)
QUEST.addTalkId(Dorothy)
