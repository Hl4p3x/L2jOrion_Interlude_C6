import sys
from l2jorion.util.random import Rnd
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.cache import HtmCache

qn = "384_WarehouseKeepersPastime"

#NPCs
Cliff = 30182
Baxt = 30685

#ITEMS
Medal = 5964

#Droplist format - npcId : chance (%)
Droplist = {
    20948 : 18, #Bartal
    20945 : 12, #Cadeine
    20946 : 15, #Sanhidro
    20947 : 16, #Connabi
    20635 : 15, #Carinkain
    20773 : 61, #Conjurer Bat Lord
    20774 : 60, #Conjurer Bat
    20760 : 24, #Dragon Bearer Archer
    20758 : 24, #Dragon Bearer Chief
    20759 : 23, #Dragon Bearer Warrior
    20242 : 22, #Dustwind Gargoyle
    20281 : 22, #Dustwind Gargoyle (2)
    20556 : 14, #Giant Monstereye
    20668 : 21, #Grave Guard
    20241 : 22, #Hunter Gargoyle
    20286 : 22, #Hunter Gargoyle (2)
    20950 : 20, #Innersen
    20949 : 19, #Luminun
    20942 : 9,  #Nightmare Guide
    20943 : 12, #Nightmare Keeper
    20944 : 11, #Nightmare Lord
    20559 : 14, #Rotting Golem
    20243 : 21, #Thunder Wyrm
    20282 : 21, #Thunder Wyrm (2)
    20677 : 34, #Tulben
    20605 : 15, #Weird Drake
}

#Rewards - item : chance (iterative)
Rewards_10_Win = {
    1888 : 16,  #Synthetic Cokes
    1887 : 32,  #Varnish of Purity
    1894 : 50,  #Crafted Leather
    952 : 80,   #Scroll: Enchant Armor (C)
    1890 : 89,  #Mithril Alloy
    1893 : 98,  #Oriharukon
    951 : 100   #Scroll: Enchant Weapon (C)
}

Rewards_10_Lose = {
    4041 : 50,  #Mold Hardener
    952 : 80,   #Scroll: Enchant Armor (C)
    1892 : 98,  #Blacksmith\'s Frame
    917 : 100   #Necklace of Mermaid
}

Rewards_100_Win = {
    883 : 50,   #Aquastone Ring
    951 : 80,   #Scroll: Enchant Weapon (C)
    852 : 98,   #Moonstone Earring
    401 : 100   #Drake Leather Armor
}

Rewards_100_Lose = {
    951 : 50,   #Scroll: Enchant Weapon (C)
    500 : 80,   #Great Helmet
    2437 : 98,  #Drake Leather Boots
    135 : 100   #Samurai Longsword
}

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [Medal]
     #a dynamic dictionary mapping player names to board status and selected numbers - name : [[game],[guesses],bet]
     self.gameStatus = {}

 def generateBoard(self,player,bet) :
     board = []
     num = [1,2,3,4,5,6,7,8,9]
     for i in range(9) : board.append(num.pop(Rnd.get(1,len(num))-1))
     self.gameStatus[player.getName()] = [board,[],bet]
     return

 def fillBoard(self,player,htmltext) :
     guess = self.gameStatus[player.getName()][1]
     for i in range(9) :
        num = self.gameStatus[player.getName()][0][i]
        if num in guess :
            htmltext = htmltext.replace("<?Cell"+str(i+1)+"?>",str(num))
        else :
            htmltext = htmltext.replace("<?Cell"+str(i+1)+"?>","?")
     return htmltext

 def checkWin(self,player) :
     board = self.gameStatus[player.getName()][0]
     guess = self.gameStatus[player.getName()][1]
     new_board = [[0,0,0],[0,0,0],[0,0,0]]
     k = match = win = 0
     a = range(3)
     for i in a :
         for j in a :
             new_board[i][j] = board[k]
             k += 1
     #Checking rows
     for i in a :
         for j in a :
             match += (new_board[i][j] in guess)
         if match == 3 :
             win += 1
         match = 0
     #Checking collumns
     for j in a :
         for i in a :
             match += (new_board[i][j] in guess)
         if match == 3 :
             win += 1
         match = 0
     #Checking diagonals
     for i in a :
         match += (new_board[i][i] in guess)
     if match == 3 :
         win += 1
     match = 0
     for i,j in [[0,2],[1,1],[2,0]] :
         match += (new_board[i][j] in guess)
     if match == 3 :
         win += 1
     return win

 def getReward(self,rewards) :
     rand = Rnd.get(100)
     for item in rewards.keys() :
        if rand < rewards[item] :
            return item

 def onAdvEvent (self,event,npc,player):
   st = player.getQuestState(qn)
   if not st: return
   htmltext = event
   npcId = str(npc.getNpcId())
   if event == "30182-05.htm" :
       st.playSound("ItemSound.quest_accept")
       st.setState(STARTED)
       st.set("cond","1")
   elif event == npcId + "-08.htm" :
       st.playSound("ItemSound.quest_finish")
       st.exitQuest(1)
   elif event == npcId + "-11.htm" :
       if st.getQuestItemsCount(Medal) >= 10 :
           st.takeItems(Medal,10)
           self.generateBoard(player,10)
       else :
           htmltext = npcId + "-12.htm"
   elif event == npcId + "-13.htm" :
       if st.getQuestItemsCount(Medal) >= 100 :
           st.takeItems(Medal,100)
           self.generateBoard(player,100)
       else :
           htmltext = npcId + "-12.htm"
   elif event.startswith("select_1-") : #first pick
       selection = int(event[9])
       self.gameStatus[player.getName()][1].append(selection)
       htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-14.htm")
       htmltext = self.fillBoard(player,htmltext)
   elif event.startswith("select_2-") : #pick #2-5
       selection = int(event[9])
       guess = self.gameStatus[player.getName()][1]
       num_guesses = len(guess)
       if selection in guess : #already chose that number!
           htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-" + str(14+2*num_guesses) + ".htm")
       else :
           self.gameStatus[player.getName()][1].append(selection)
           num_guesses += 1
           htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-" + str(11+2*num_guesses) + ".htm")
       htmltext = self.fillBoard(player,htmltext)
   elif event.startswith("select_3-") : #pick #6
       selection = int(event[9])
       guess = self.gameStatus[player.getName()][1]
       if selection in guess : #already chose that number!
           htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-26.htm")
           htmltext = self.fillBoard(player,htmltext)
       else :
           self.gameStatus[player.getName()][1].append(selection)
           wins = self.checkWin(player)
           bet = self.gameStatus[player.getName()][2]
           if wins == 3 :
               item = self.getReward(eval("Rewards_"+str(bet)+"_Win"))
               st.giveItems(item,1)
               htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-23.htm")
           elif wins == 0 :
               item = self.getReward(eval("Rewards_"+str(bet)+"_Lose"))
               if item == 2437 : st.giveItems(2463,1)
               st.giveItems(item,1)
               htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-25.htm")
           else :
               htmltext = HtmCache.getInstance().getHtm("data/scripts/quests/" + qn + "/" + npcId + "-24.htm")
           guess = self.gameStatus[player.getName()][1]
           for i in range(9) :
              num = self.gameStatus[player.getName()][0][i]
              htmltext = htmltext.replace("<?Cell"+str(i+1)+"?>",str(num))
              if num in guess :
                  htmltext = htmltext.replace("<?FontColor"+str(i+1)+"?>","ff0000")
              else :
                  htmltext = htmltext.replace("<?FontColor"+str(i+1)+"?>","ffffff")
   return htmltext

 def onTalk (self,npc,player) :
   st = player.getQuestState(qn)
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
   if not st: return htmltext
   npcId = npc.getNpcId()
   id = st.getState()
   cond = st.getInt("cond")
   if npcId == Cliff :
       if id == CREATED :
           htmltext = "04"
           if player.getLevel() >= 40 :
               htmltext = "01"
       elif st.getQuestItemsCount(Medal) < 10 :
           htmltext = "06"
       elif st.getQuestItemsCount(Medal) >= 10 :
           htmltext = "07"
   elif npcId == Baxt :
       if st.getQuestItemsCount(Medal) < 10 :
           htmltext = "01"
       elif st.getQuestItemsCount(Medal) >= 10 :
           htmltext = "02"
   if htmltext.isdigit() :
       htmltext = str(npcId) + "-" + htmltext + ".htm"
   return htmltext

 def onKill(self,npc,player,isPet) :
     partyMember = self.getRandomPartyMemberState(player, STARTED)
     if not partyMember: return
     st = partyMember.getQuestState(qn)
     if not st : return
     npcId = npc.getNpcId()
     if npcId in Droplist.keys() :
         if Rnd.get(100) < Droplist[npcId] :
             st.giveItems(Medal,1)
             st.playSound("ItemSound.quest_itemget")
     return

QUEST = Quest(384,qn,"Warehouse Keeper\’s Pastime")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(Cliff)
QUEST.addTalkId(Cliff)
QUEST.addTalkId(Baxt)

for mob in Droplist.keys() :
    QUEST.addKillId(mob)