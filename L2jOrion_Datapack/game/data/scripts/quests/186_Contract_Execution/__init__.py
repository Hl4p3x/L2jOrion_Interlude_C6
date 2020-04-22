# Rewritten by RayzoR
import sys
from l2jorion.game.managers import QuestManager
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "186_Contract_Execution"

#NPCs
Nikola = 30621
Lorain = 30673
Luka = 31437

#Items
Certificate = 10362
MetalReport = 10366
Accessory = 10367

#Monsters
Mobs = range(20577,20583)

class Quest (JQuest) :
    def __init__(self,id,name,descr):
        JQuest.__init__(self,id,name,descr)
        self.questItemIds = [Certificate,MetalReport,Accessory]

    def onAdvEvent (self,event,npc, player) :
        st = player.getQuestState(qn)
        if not st: return
        htmltext = event
        if event == "30673-02.htm" :
            st.playSound("ItemSound.quest_accept")
            st.set("cond","1")
            st.takeItems(Certificate,-1)
            st.giveItems(MetalReport,1)
        elif event == "30621-03.htm":
            st.set("cond","2")
            st.playSound("ItemSound.quest_middle")
        elif event == "31437-05.htm":
            if player.getLevel() < 50 :
               st.addExpAndSp(285935,18711)
            st.giveItems(57,105083)
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
                    htmltext = "30621-04.htm"
            elif npcId == Luka :
                if not st.getQuestItemsCount(Accessory) :
                    htmltext = "31437-01.htm"
                else :
                    htmltext = "31437-02.htm"
        return htmltext

    def onKill(self,npc,player,isPet):
        st = player.getQuestState(qn)
        if not st : return 
        if st.getState() != STARTED : return
        if not st.getQuestItemsCount(Accessory) and st.getInt("cond") == 2:
            if st.getRandom(5) == 0 :
                st.playSound("ItemSound.quest_middle")
                st.giveItems(Accessory,1)
        return

    def onFirstTalk (self,npc,player):
       qs = player.getQuestState("184_Nikolas_Cooperation_Contract")
       qs2 = player.getQuestState("185_Nikolas_Cooperation_Consideration")
       qs3 = player.getQuestState("186_Contract_Execution")
       qs4 = player.getQuestState("187_Nikolas_Heart")
       qs5 = player.getQuestState("188_Seal_Removal")
       q4 = QuestManager.getInstance().getQuest("187_Nikolas_Heart")
       q5 = QuestManager.getInstance().getQuest("188_Seal_Removal")
       if qs or qs2 :
           if qs3 or qs4 or qs5 or not q4 or not q5:
               npc.showChatWindow(player)
               return None
           if qs and qs.getState() == COMPLETED:
               if qs.getQuestItemsCount(Certificate) :
                   qs3 = self.newQuestState(player)
                   qs3.setState(STARTED)
               else :
                   qs5 = q5.newQuestState(player)
                   qs5.setState(STARTED)
           elif qs2 and qs2.getState() == COMPLETED:
               if qs2.getQuestItemsCount(Certificate) :
                   qs4 = q4.newQuestState(player)
                   qs4.setState(STARTED)
               else :
                   qs5 = q5.newQuestState(player)
                   qs5.setState(STARTED)
       npc.showChatWindow(player)
       return None

QUEST       = Quest(186,qn,"Contract Execution")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addTalkId(Nikola)
QUEST.addTalkId(Lorain)
QUEST.addTalkId(Luka)
QUEST.addFirstTalkId(Lorain)
for mob in Mobs :
    QUEST.addKillId(mob)