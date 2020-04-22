# Rewritten by RayzoR
import sys
from l2jorion.game.managers import QuestManager
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "183_Relic_Exploration"

#NPCs
Kusto = 30512
Lorain = 30673
Nikola = 30621

class Quest (JQuest) :
    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)

    def onAdvEvent (self,event,npc, player) :
        st = player.getQuestState(qn)
        if not st: return
        htmltext = event
        if event == "30512-03.htm":
            st.playSound("ItemSound.quest_accept")
            st.set("cond","1")
            st.setState(STARTED)
        elif event == "30673-04.htm":
            st.set("cond","2")
            st.playSound("ItemSound.quest_middle")
        elif event == "30621-02.htm":
            if player.getLevel() < 50:
               st.addExpAndSp(60000,3000)
            st.giveItems(57,18100)
            st.exitQuest(False)
            st.playSound("ItemSound.quest_finish")
        elif event == "Contract" :
            q1 = QuestManager.getInstance().getQuest("184_Nikolas_Cooperation_Contract")
            if q1 :
                qs1 = q1.newQuestState(player)
                qs1.setState(STARTED)
                q1.notifyEvent("30621-01.htm",npc,player)
            return
        elif event == "Consideration" :
            q2 = QuestManager.getInstance().getQuest("185_Nikolas_Cooperation_Consideration")
            if q2 :
                qs2 = q2.newQuestState(st.getPlayer())
                qs2.setState(STARTED)
                q2.notifyEvent("30621-01.htm",npc,player)
            return
        return htmltext

    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
        st = player.getQuestState(qn)
        if not st : return htmltext
        npcId = npc.getNpcId()
        id = st.getState()
        cond = st.getInt("cond")
        if id == COMPLETED :
            if npcId == Kusto :
                htmltext = "<html><body>This quest has already been completed.</body></html>"
            elif npcId == Nikola :
                qs1 = player.getQuestState("184_Nikolas_Cooperation_Contract")
                qs2 = player.getQuestState("185_Nikolas_Cooperation_Consideration")
                if not qs1 and not qs2 :
                    htmltext = "30621-03.htm"
                else :
                    htmltext = "<html><body>This quest has already been completed.</body></html>"
        elif npcId == Kusto :
            if id == CREATED :
                if player.getLevel() < 40 :
                    htmltext = "30512-00.htm"
                else :
                    htmltext = "30512-01.htm"
            else :
                htmltext = "30512-04.htm"
        elif npcId == Lorain :
            if cond == 1 :
                htmltext = "30673-01.htm"
            else :
                htmltext = "30673-05.htm"
        elif npcId == Nikola :
            if cond == 2 :
                htmltext = "30621-01.htm"
        return htmltext

QUEST       = Quest(183,qn,"Relics Exploration")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(Kusto)
QUEST.addStartNpc(Nikola)
QUEST.addTalkId(Kusto)
QUEST.addTalkId(Lorain)
QUEST.addTalkId(Nikola)