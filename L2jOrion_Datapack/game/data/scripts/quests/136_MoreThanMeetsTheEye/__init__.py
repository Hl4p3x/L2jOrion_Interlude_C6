# Rewritten by RayzoR
import sys
from l2jorion import Config
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest


qn = "136_MoreThanMeetsTheEye"

# NPC
HARDIN=30832
ERRICKIN=30701
CLAYTON=30464
AVANTGARDE=32323

# ITEM
ECTOPLASM=9787
STABILIZED_ECTOPLASM=9786
ORDER=9788
GLASS_JAGUAR_CRYSTAL=9789
BOOK_OF_SEAL=9790
ADENA=57
TRANSFORM_BOOK=9648

# mobId:[itemId,chance1,chance2]
DROPLIST = {
    20636:[ECTOPLASM,45,0],
    20637:[ECTOPLASM,50,5],
    20638:[ECTOPLASM,55,10],
    20639:[ECTOPLASM,60,120],
    20250:[GLASS_JAGUAR_CRYSTAL,100,0]
    }
# itemId:[max,cond]
DROPCONFIG = {ECTOPLASM:[35,"4"],GLASS_JAGUAR_CRYSTAL:[5,"8"]}

class Quest (JQuest) :

 def __init__(self,id,name,descr):
     JQuest.__init__(self,id,name,descr)
     self.questItemIds = [ECTOPLASM,STABILIZED_ECTOPLASM,ORDER,GLASS_JAGUAR_CRYSTAL,BOOK_OF_SEAL]

 def onEvent (self,event,st) :
    htmltext = event
    id = st.getState()
    cond = st.getInt("cond")
    if event == "30832-02.htm" :
       st.set("cond","1")
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
    elif event == "30832-05.htm" :
       st.set("cond","2")
       st.playSound("ItemSound.quest_middle")
    elif event == "30832-10.htm" :
       st.takeItems(STABILIZED_ECTOPLASM,1)
       st.giveItems(ORDER,1)
       st.set("cond","6")
       st.playSound("ItemSound.quest_middle")
    elif event == "30832-14.htm" :
       st.takeItems(BOOK_OF_SEAL,1)
       st.giveItems(ADENA,67550)
       st.giveItems(TRANSFORM_BOOK,1)
       st.playSound("ItemSound.quest_finish")
       st.setState(COMPLETED)
       st.exitQuest(0)
    elif event == "30701-02.htm" :
       st.set("cond","3")
       st.playSound("ItemSound.quest_middle")
    elif event == "30464-02.htm" :
       st.takeItems(ORDER,1)
       st.set("cond","7")
       st.playSound("ItemSound.quest_middle")
    return htmltext

 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()
   cond = st.getInt("cond")
   if id == COMPLETED :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == HARDIN :
      if cond == 0 :
         if player.getLevel() >= 50 :
            htmltext = "30832-01.htm"
         else:
            htmltext = "30832-00.htm"
            st.exitQuest(1)
      elif cond == 1 :
         htmltext = "30832-02.htm"
      elif cond == 5 :
         htmltext = "30832-06.htm"
      elif cond == 9 :
         htmltext = "30832-11.htm"
   elif npcId == ERRICKIN :
      if cond == 2 :
         htmltext = "30701-01.htm"
      elif cond == 4 :
         htmltext = "30701-03.htm"
         st.takeItems(ECTOPLASM,35)
         st.giveItems(STABILIZED_ECTOPLASM,1)
         st.set("cond","5")
         st.playSound("ItemSound.quest_middle")
   elif npcId == CLAYTON :
      if cond == 6 :
         htmltext = "30464-01.htm"
      elif cond == 8 :
         htmltext = "30464-03.htm"
         st.takeItems(GLASS_JAGUAR_CRYSTAL,5)
         st.giveItems(BOOK_OF_SEAL,1)
         st.set("cond","9")
         st.playSound("ItemSound.quest_middle")
   return htmltext

 def onFirstTalk (self,npc,player):
   st = player.getQuestState(qn)
   npcId = npc.getNpcId()
   if npcId == AVANTGARDE and st:
     if st.getState() == COMPLETED :
          return "32323-00.htm"
   npc.showChatWindow(player)
   return None

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return
   if st.getState() != STARTED : return

   if st.getInt("cond")==3 or st.getInt("cond")==7 :
      itemId,chance1,chance2=DROPLIST[npc.getNpcId()]
      count = st.getQuestItemsCount(itemId)
      max,cond = DROPCONFIG[itemId]
      drop1 = st.getRandom(100)
      drop2 = st.getRandom(100)
      qty1,chance1 = divmod(chance1*Config.RATE_DROP_QUEST,100)
      if drop1 < chance1 : qty1 += 1
      qty1 = int(qty1)
      if qty1 :
         qty2,chance2 = divmod(chance2*Config.RATE_DROP_QUEST,100)
         if drop2 < chance2 : qty2 += 1
         qty = qty1 + int(qty2)
         if (qty + count) >= max :
            qty = max - count
            st.playSound("ItemSound.quest_middle")
            st.set("cond",cond)
         else :
            st.playSound("ItemSound.quest_itemget")
         st.giveItems(itemId,qty)
   return

QUEST       = Quest(136,qn,"More Than Meets The Eye")

CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(HARDIN)

QUEST.addTalkId(HARDIN)
QUEST.addTalkId(ERRICKIN)
QUEST.addTalkId(CLAYTON)
QUEST.addFirstTalkId(AVANTGARDE)

for mob in DROPLIST.keys() :
  QUEST.addKillId(mob)
