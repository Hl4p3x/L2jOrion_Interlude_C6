import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "999_C3Tutorial"

RECOMMENDATION_01 = 1067
RECOMMENDATION_02 = 1068
LEAF_OF_MOTHERTREE = 1069
BLOOD_OF_JUNDIN = 1070
LICENSE_OF_MINER = 1498
VOUCHER_OF_FLAME = 1496
SOULSHOT_NOVICE = 5789
SPIRITSHOT_NOVICE = 5790
BLUE_GEM=6353
TOKEN = 8542
SCROLL= 8594

# event:[htmlfile,radarX,radarY,radarZ,item,classId1,gift1,count1,classId2,gift2,count2]
EVENTS={
"30008_02":["30008-03.htm",-84058, 243239,-3730,RECOMMENDATION_01 ,0x00,SOULSHOT_NOVICE  ,200,0x00,              0,  0],
"30017_02":["30017-03.htm",-84058, 243239,-3730,RECOMMENDATION_02 ,0x0a,SPIRITSHOT_NOVICE,100,0x00,              0,  0],
"30370_02":["30370-03.htm", 45491,  48359,-3086,LEAF_OF_MOTHERTREE,0x19,SPIRITSHOT_NOVICE,100,0x12,SOULSHOT_NOVICE,200],
"30129_02":["30129-03.htm", 12116,  16666,-4610,BLOOD_OF_JUNDIN   ,0x26,SPIRITSHOT_NOVICE,100,0x1f,SOULSHOT_NOVICE,200],
"30528_02":["30528-03.htm",115642,-178046, -941,LICENSE_OF_MINER  ,0x35,SOULSHOT_NOVICE  ,200,0x00,              0,  0],
"30573_02":["30573-03.htm",-45067,-113549, -235,VOUCHER_OF_FLAME  ,0x31,SPIRITSHOT_NOVICE,100,0x2c,SOULSHOT_NOVICE,200]
}

# npcId:[raceId,[htmlfiles],npcTyp,item]
TALKS={
30017:[0,["30017-01.htm","30017-02.htm","30017-04.htm"],0,0],
30008:[0,["30008-01.htm","30008-02.htm","30008-04.htm"],0,0],
30370:[1,["30370-01.htm","30370-02.htm","30370-04.htm"],0,0],
30129:[2,["30129-01.htm","30129-02.htm","30129-04.htm"],0,0],
30573:[3,["30573-01.htm","30573-02.htm","30573-04.htm"],0,0],
30528:[4,["30528-01.htm","30528-02.htm","30528-04.htm"],0,0],
30018:[0,["30131-01.htm",0,"30019-03a.htm","30019-04.htm",],1,RECOMMENDATION_02],
30019:[0,["30131-01.htm",0,"30019-03a.htm","30019-04.htm",],1,RECOMMENDATION_02],
30020:[0,["30131-01.htm",0,"30019-03a.htm","30019-04.htm",],1,RECOMMENDATION_02],
30021:[0,["30131-01.htm",0,"30019-03a.htm","30019-04.htm",],1,RECOMMENDATION_02],
30009:[0,["30530-01.htm","30009-03.htm",0,"30009-04.htm",],1,RECOMMENDATION_01],
30011:[0,["30530-01.htm","30009-03.htm",0,"30009-04.htm",],1,RECOMMENDATION_01],
30012:[0,["30530-01.htm","30009-03.htm",0,"30009-04.htm",],1,RECOMMENDATION_01],
30056:[0,["30530-01.htm","30009-03.htm",0,"30009-04.htm",],1,RECOMMENDATION_01],
30400:[1,["30131-01.htm","30400-03.htm","30400-03a.htm","30400-04.htm",],1,LEAF_OF_MOTHERTREE],
30401:[1,["30131-01.htm","30400-03.htm","30400-03a.htm","30400-04.htm",],1,LEAF_OF_MOTHERTREE],
30402:[1,["30131-01.htm","30400-03.htm","30400-03a.htm","30400-04.htm",],1,LEAF_OF_MOTHERTREE],
30403:[1,["30131-01.htm","30400-03.htm","30400-03a.htm","30400-04.htm",],1,LEAF_OF_MOTHERTREE],
30131:[2,["30131-01.htm","30131-03.htm","30131-03a.htm","30131-04.htm",],1,BLOOD_OF_JUNDIN],
30404:[2,["30131-01.htm","30131-03.htm","30131-03a.htm","30131-04.htm",],1,BLOOD_OF_JUNDIN],
30574:[3,["30575-01.htm","30575-03.htm","30575-03a.htm","30575-04.htm",],1,VOUCHER_OF_FLAME],
30575:[3,["30575-01.htm","30575-03.htm","30575-03a.htm","30575-04.htm",],1,VOUCHER_OF_FLAME],
30530:[4,["30530-01.htm","30530-03.htm",0,"30530-04.htm",],1,LICENSE_OF_MINER]
}

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player) :
    st = player.getQuestState(qn)
    htmltext = event
    htmlfile,radarX,radarY,radarZ,item,classId1,gift1,count1,classId2,gift2,count2 = EVENTS[event]
    st.addRadar(radarX,radarY,radarZ)
    htmltext = htmlfile
    isMage = player.getClassId().isMage()
    if st.getQuestItemsCount(item) and st.getInt("onlyone") == 0:
      st.addExpAndSp(0,50)
      st.takeItems(item,1)
      if player.getClassId().getId() == classId1 :
        st.giveItems(gift1,count1)
      elif player.getClassId().getId() == classId2 :
        if gift2:
           st.giveItems(gift2,count2)
      st.unset("cond")
      st.set("onlyone","1")
      st.setState(COMPLETED)
      st.playSound("ItemSound.quest_finish")
    return htmltext

 def onFirstTalk (self,npc,player):
   htmltext = None
   npcId = npc.getNpcId()
   st = player.getQuestState(qn)
   if not st : st = self.newQuestState(player)
   id = st.getState()
   cond = st.getInt("cond")
   onlyone = st.getInt("onlyone")
   level = player.getLevel()
   isMage = player.getClassId().isMage()
   npcTyp = 0
   if id == CREATED :
     st.setState(STARTING)
     st.set("onlyone","0")
   raceId,htmlfiles,npcTyp,item = TALKS[npcId] 
   if (level >= 10 or onlyone) and npcTyp == 1:
       htmltext = "30575-05.htm"
   elif onlyone == 0 and level < 10 :
    if player.getRace().ordinal() == raceId :
      htmltext=htmlfiles[0]
      if npcTyp == 1 :
       if cond == 0 :
        if isMage :
         st.set("cond","1")
         st.setState(STARTED)
         st.playSound("ItemSound.quest_tutorial")
        else:
         htmltext="30530-01.htm"
         st.set("cond","1")
         st.setState(STARTED)
         st.playSound("ItemSound.quest_tutorial")
       elif cond==1 and st.getQuestItemsCount(item)==0 :
         if st.getQuestItemsCount(BLUE_GEM) :
           st.takeItems(BLUE_GEM,st.getQuestItemsCount(BLUE_GEM))
           st.giveItems(item,1)
           st.set("cond","2")
           st.playSound("ItemSound.quest_middle")
           if isMage :
             st.giveItems(SPIRITSHOT_NOVICE,100)
             htmltext = htmlfiles[2]
             if htmltext == 0 :
                 htmltext = "<html><body>I am sorry.  I only help warriors.  Please go to another Newbie Helper who may assist you.</body></html>"
           else:
             st.giveItems(SOULSHOT_NOVICE,200)
             htmltext = htmlfiles[1]
             if htmltext == 0 :
                 htmltext = "<html><body>I am sorry.  I only help mystics.  Please go to another Newbie Helper who may assist you.</body></html>"
         else:
           if isMage :
             htmltext = "30131-02.htm"
             if player.getRace().ordinal() == 3 :
              htmltext = "30575-02.htm"
           else:
             htmltext = "30530-02.htm"
       elif cond==2 :
        htmltext = htmlfiles[3]
      elif npcTyp == 0 :
        if cond==1 :
          htmltext = htmlfiles[0]
        elif cond==2 :
          htmltext = htmlfiles[1]
        elif cond==3 :
          htmltext = htmlfiles[2] 
   #else:
       #htmltext = "<html><body>You are too experienced now.</body></html>"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return
   if st.getState() != STARTED : return 
   if st.getInt("cond")==1 and st.getRandom(100) < 25 and st.getQuestItemsCount(BLUE_GEM) == 0 :
      st.giveItems(BLUE_GEM,1)
      st.playSound("ItemSound.quest_itemget")
      st.playSound("ItemSound.quest_tutorial")
   return

QUEST       = Quest(999,qn,"Interlude Tutorial")
CREATED     = State('Start', QUEST)
STARTING    = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

for startNpc in [30008,30009,30017,30019,30129,30131,30573,30575,30370,30528,30530,30400,30401,30402,30403,30404]:
  QUEST.addStartNpc(startNpc)
  QUEST.addFirstTalkId(startNpc)
  QUEST.addTalkId(startNpc)

QUEST.addKillId(18342)
QUEST.addKillId(20001)