import sys
from java.lang import System
from l2jorion import Config
from l2jorion.game.datatables.sql import SpawnTable
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import CreatureSay
from l2jorion.util.random import Rnd

qn = "610_MagicalPowerOfWaterPart2"

#NPC
Asefa = 31372
Alter = 31560

#MOBS
Ketra_Orcs = [ 21324, 21325, 21327, 21328, 21329, 21331, 21332, 21334, 21335, \
21336, 21338, 21339, 21340, 21342, 21343, 21344, 21345, 21346, 21347, 21348, 21349 ]
Ashutar = 25316

#ITEMS
Totem2 = 7238
Ice_Heart = 7239

def AutoChat(npc,text) :
    chars = npc.getKnownList().getKnownPlayers().values().toArray()
    if chars != None:
       for pc in chars :
          sm = CreatureSay(npc.getObjectId(), 0, npc.getName(), text)
          pc.sendPacket(sm)

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [Ice_Heart]
     test = self.loadGlobalQuestVar("610_respawn")
     if test.isdigit() :
        remain = long(test) - System.currentTimeMillis()
        if remain <= 0 :
           self.addSpawn(31560,105452,-36775,-1050,34000, False, 0)
        else :
           self.startQuestTimer("spawn_npc", remain, None, None)
     else :
        self.addSpawn(31560,105452,-36775,-1050,34000, False, 0)

 def onAdvEvent (self, event, npc, player) :
   if event == "Soul of Water Ashutar has despawned" :
       npc.reduceCurrentHp(9999999,npc,False)
       self.addSpawn(31560,105452,-36775,-1050,34000, False, 0)
       AutoChat(npc,"The fetter strength is weaken Your consciousness has been defeated!")
       return
   elif event == "spawn_npc" :
       self.addSpawn(31560,105452,-36775,-1050,34000, False, 0)
       return
   st = player.getQuestState(qn)
   if not st: return
   cond = st.getInt("cond")
   id = st.getInt("id")
   Green_Totem = st.getQuestItemsCount(Totem2)
   Heart = st.getQuestItemsCount(Ice_Heart)
   htmltext = event
   if event == "31372-04.htm" :
       if st.getPlayer().getLevel() >= 75 and st.getPlayer().getAllianceWithVarkaKetra() >= 2 :
           if Green_Totem :
                st.set("cond","1")
                st.set("id","1")
                st.setState(STARTED)
                st.playSound("ItemSound.quest_accept")
                htmltext = "31372-04.htm"
           else :
                htmltext = "31372-02.htm"
                st.exitQuest(1)
       else :
           htmltext = "31372-03.htm"
           st.exitQuest(1)
   elif event == "31372-08.htm" :
       if Heart:
           htmltext = "31372-08.htm"
           st.takeItems(Ice_Heart,-1)
           st.addExpAndSp(10000,0)
           st.unset("id")
           st.unset("cond")
           st.playSound("ItemSound.quest_finish")
           st.exitQuest(1)
       else :
           htmltext = "31372-09.htm"
   elif event == "31560-02.htm" :
       if Green_Totem == 0 :
           htmltext = "31560-04.htm"
       else:
           spawnedNpc = st.addSpawn(Ashutar,104825,-36926,-1136)
           st.takeItems(Totem2,1)
           st.set("id","2")
           npc.deleteMe()
           st.set("cond","2")
           self.startQuestTimer("Soul of Water Ashutar has despawned",1200000,spawnedNpc,None)
           AutoChat(spawnedNpc,"The water charm then is the storm and the tsunami strength! Opposes with it only has the blind alley!")
   return htmltext

 def onTalk (self, npc, player):
   st = player.getQuestState(qn)
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   if st :
    npcId = npc.getNpcId()
    cond = st.getInt("cond")
    id = st.getInt("id")
    Green_Totem = st.getQuestItemsCount(Totem2)
    Heart = st.getQuestItemsCount(Ice_Heart)
    if npcId == Asefa :
        if st.getState()== CREATED :
            htmltext = "31372-01.htm"
        elif id == 1 or id == 2 :
            htmltext = "31372-05.htm"
        elif id == 3:
            if Heart :
                htmltext = "31372-06.htm"
            else :
                htmltext = "31372-07.htm"
    elif npcId == Alter :
       htmltext = "31560-01.htm"
    return htmltext

 def onKill(self,npc,player,isPet):
    npcId = npc.getNpcId()
    if npcId == Ashutar :
        respawnMinDelay = 86400000  * int(Config.RAID_MIN_RESPAWN_MULTIPLIER)
        respawnMaxDelay = 90000000 * int(Config.RAID_MAX_RESPAWN_MULTIPLIER)
        respawn_delay = Rnd.get(respawnMinDelay,respawnMaxDelay)
        self.saveGlobalQuestVar("610_respawn", str(System.currentTimeMillis()+respawn_delay))
        self.startQuestTimer("spawn_npc", respawn_delay, None, None)
        self.cancelQuestTimer("Soul of Water Ashutar has despawned",npc,None)
        party = player.getParty()
        if party :
            PartyQuestMembers = []
            for player1 in party.getPartyMembers().toArray() :
                st1 = player1.getQuestState(qn)
                if st1 :
                    if st1.getState() == STARTED and (st1.getInt("cond") == 1 or st1.getInt("cond") == 2) :
                        PartyQuestMembers.append(st1)
            if len(PartyQuestMembers) == 0 : return
            st = PartyQuestMembers[Rnd.get(len(PartyQuestMembers))]
            if st.getQuestItemsCount(Totem2) > 0 :
                st.takeItems(Totem2,1,npc)
            st.giveItems(Ice_Heart,1,npc) 
            st.set("cond","3")
            st.set("id","3")
            st.playSound("ItemSound.quest_middle")
        else :
            st = player.getQuestState(qn)
            if not st : return
            if st.getState() == STARTED and (st.getInt("cond") == 1 or st.getInt("cond") == 2) :
                if st.getQuestItemsCount(Totem2) > 0 :
                    st.takeItems(Totem2,1)
                st.giveItems(Ice_Heart,1) 
                st.set("cond","3")
                st.set("id","3")
                st.playSound("ItemSound.quest_middle")
    elif npcId in Ketra_Orcs :
      st = player.getQuestState(qn)
      if st :
         if st.getQuestItemsCount(Ice_Heart) :
             st.takeItems(Ice_Heart,-1)
         st.unset("cond")
         st.unset("id")
         st.exitQuest(1)
    return

QUEST       = Quest(610,qn,"Magical Power of Water - Part 2")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(Asefa)

QUEST.addTalkId(Asefa)
QUEST.addTalkId(Alter)

QUEST.addKillId(Ashutar)

for mobId in Ketra_Orcs:
    QUEST.addKillId(mobId)