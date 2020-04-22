# Maked by Mr. Have fun! Version 0.2
# Quest: Testimony Of Life
# Fixed by Artful (http://L2PLanet.ru Lineage2 C3 Server)
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "218_TestimonyOfLife"

MARK_OF_LIFE_ID = 3140
CARDIENS_LETTER_ID = 3141
CAMOMILE_CHARM_ID = 3142
HIERARCHS_LETTER_ID = 3143
MOONFLOWER_CHARM_ID = 3144
GRAIL_DIAGRAM_ID = 3145
THALIAS_LETTER1_ID = 3146
THALIAS_LETTER2_ID = 3147
THALIAS_INSTRUCTIONS_ID = 3148
PUSHKINS_LIST_ID = 3149
PURE_MITHRIL_CUP_ID = 3150
ARKENIAS_CONTRACT_ID = 3151
ARKENIAS_INSTRUCTIONS_ID = 3152
ADONIUS_LIST_ID = 3153
ANDARIEL_SCRIPTURE_COPY_ID = 3154
STARDUST_ID = 3155
ISAELS_INSTRUCTIONS_ID = 3156
ISAELS_LETTER_ID = 3157
GRAIL_OF_PURITY_ID = 3158
TEARS_OF_UNICORN_ID = 3159
WATER_OF_LIFE_ID = 3160
PURE_MITHRIL_ORE_ID = 3161
ANT_SOLDIER_ACID_ID = 3162
WYRMS_TALON1_ID = 3163
SPIDER_ICHOR_ID = 3164
HARPYS_DOWN_ID = 3165
TALINS_SPEAR_BLADE_ID = 3166
TALINS_SPEAR_SHAFT_ID = 3167
TALINS_RUBY_ID = 3168
TALINS_AQUAMARINE_ID = 3169
TALINS_AMETHYST_ID = 3170
TALINS_PERIDOT_ID = 3171
TALINS_SPEAR_ID = 3026

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "1":
        htmltext = "30460-04.htm"
        st.set("cond","1")
        st.setState(STARTED)
        st.playSound("ItemSound.quest_accept")
        st.giveItems(CARDIENS_LETTER_ID,1)
    elif event == "30154_1" :
          htmltext = "30154-02.htm"
    elif event == "30154_2" :
          htmltext = "30154-03.htm"
    elif event == "30154_3" :
          htmltext = "30154-04.htm"
    elif event == "30154_4" :
          htmltext = "30154-05.htm"
    elif event == "30154_5" :
          htmltext = "30154-06.htm"
    elif event == "30154_6" :
          htmltext = "30154-07.htm"
          st.takeItems(CARDIENS_LETTER_ID,1)
          st.giveItems(MOONFLOWER_CHARM_ID,1)
          st.giveItems(HIERARCHS_LETTER_ID,1)
    elif event == "30371_1" :
          htmltext = "30371-02.htm"
    elif event == "30371_2" :
          htmltext = "30371-03.htm"
          st.takeItems(HIERARCHS_LETTER_ID,1)
          st.giveItems(GRAIL_DIAGRAM_ID,1)
    elif event == "30371_3" :
          if st.getPlayer().getLevel() < 38 :
            htmltext = "30371-10.htm"
            st.takeItems(STARDUST_ID,1)
            st.giveItems(THALIAS_INSTRUCTIONS_ID,1)
          else:
            htmltext = "30371-11.htm"
            st.takeItems(STARDUST_ID,1)
            st.giveItems(THALIAS_LETTER2_ID,1)
    elif event == "30300_1" :
          htmltext = "30300-02.htm"
    elif event == "30300_2" :
          htmltext = "30300-03.htm"
    elif event == "30300_3" :
          htmltext = "30300-04.htm"
    elif event == "30300_4" :
          htmltext = "30300-05.htm"
    elif event == "30300_5" :
          htmltext = "30300-06.htm"
          st.takeItems(GRAIL_DIAGRAM_ID,1)
          st.giveItems(PUSHKINS_LIST_ID,1)
    elif event == "30300_6" :
          htmltext = "30300-09.htm"
    elif event == "30300_7" :
          htmltext = "30300-10.htm"
          st.takeItems(PURE_MITHRIL_ORE_ID,st.getQuestItemsCount(PURE_MITHRIL_ORE_ID))
          st.takeItems(ANT_SOLDIER_ACID_ID,st.getQuestItemsCount(ANT_SOLDIER_ACID_ID))
          st.takeItems(WYRMS_TALON1_ID,st.getQuestItemsCount(WYRMS_TALON1_ID))
          st.takeItems(PUSHKINS_LIST_ID,1)
          st.giveItems(PURE_MITHRIL_CUP_ID,1)
    elif event == "30419_1" :
          htmltext = "30419-02.htm"
    elif event == "30419_2" :
          htmltext = "30419-03.htm"
    elif event == "30419_3" :
          htmltext = "30419-04.htm"
          st.takeItems(THALIAS_LETTER1_ID,1)
          st.giveItems(ARKENIAS_CONTRACT_ID,1)
          st.giveItems(ARKENIAS_INSTRUCTIONS_ID,1)
    elif event == "30375_1" :
          htmltext = "30375-02.htm"
          st.takeItems(ARKENIAS_INSTRUCTIONS_ID,1)
          st.giveItems(ADONIUS_LIST_ID,1)
    elif event == "30655_1" :
          htmltext = "30655-02.htm"
          st.takeItems(THALIAS_LETTER2_ID,1)
          st.giveItems(ISAELS_INSTRUCTIONS_ID,1)
    return htmltext


 def onTalk (self,npc,player):
   htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
   st = player.getQuestState(qn)
   if not st : return htmltext

   npcId = npc.getNpcId()
   id = st.getState()
   if npcId != 30460 and id != STARTED : return htmltext

   if id == CREATED :
     st.setState(STARTING)
     st.set("cond","0")
     st.set("onlyone","0")
     st.set("id","0")
   if npcId == 30460 and st.getInt("cond")==0 and st.getInt("onlyone")==0 :
        if st.getInt("cond") < 15 :
          if player.getRace().ordinal() != 1 :
            htmltext = "30460-01.htm"
          else:
            if player.getLevel() < 37 :
              htmltext = "30460-02.htm"
              st.exitQuest(1)
            else:
              htmltext = "30460-03.htm"
              st.set("cond","1")
              return htmltext
        else:
          htmltext = "30460-03.htm"
          st.exitQuest(1)
   elif npcId == 30460 and st.getInt("cond")==0 and st.getInt("onlyone")==1 :
      htmltext = "<html><body>This quest has already been completed.</body></html>"
   elif npcId == 30460 and st.getInt("cond")==1 and st.getQuestItemsCount(CARDIENS_LETTER_ID)==1 :
        htmltext = "30460-05.htm"
   elif npcId == 30460 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30460-06.htm"
   elif npcId == 30460 and st.getInt("cond")==1 and st.getQuestItemsCount(CAMOMILE_CHARM_ID)==1 :
        if st.getGameTicks() != st.getInt("id") :
          st.set("id",str(st.getGameTicks()))
          st.addExpAndSp(104591,11250)
          st.giveItems(7562,16)
          st.giveItems(MARK_OF_LIFE_ID,1)
          st.takeItems(CAMOMILE_CHARM_ID,1)
          htmltext = "30460-07.htm"
          st.set("cond","0")
          st.set("onlyone","1")
          st.setState(COMPLETED)
          st.playSound("ItemSound.quest_finish")
   elif npcId == 30154 and st.getInt("cond")==1 and st.getQuestItemsCount(CARDIENS_LETTER_ID)==1 :
        htmltext = "30154-01.htm"
   elif npcId == 30154 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 and st.getQuestItemsCount(WATER_OF_LIFE_ID)==0 :
        htmltext = "30154-08.htm"
   elif npcId == 30154 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(WATER_OF_LIFE_ID) :
        htmltext = "30154-09.htm"
        st.takeItems(WATER_OF_LIFE_ID,1)
        st.takeItems(MOONFLOWER_CHARM_ID,1)
        st.giveItems(CAMOMILE_CHARM_ID,1)
   elif npcId == 30154 and st.getInt("cond")==1 and st.getQuestItemsCount(CAMOMILE_CHARM_ID)==1 :
        htmltext = "30154-10.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(HIERARCHS_LETTER_ID) :
        htmltext = "30371-01.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(GRAIL_DIAGRAM_ID) :
        htmltext = "30371-04.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(PUSHKINS_LIST_ID) :
        htmltext = "30371-05.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(PURE_MITHRIL_CUP_ID) :
        htmltext = "30371-06.htm"
        st.takeItems(PURE_MITHRIL_CUP_ID,1)
        st.giveItems(THALIAS_LETTER1_ID,1)
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(THALIAS_LETTER1_ID) :
        htmltext = "30371-07.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ARKENIAS_CONTRACT_ID) :
        htmltext = "30371-08.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(STARDUST_ID) :
        htmltext = "30371-09.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(THALIAS_INSTRUCTIONS_ID) :
        if player.getLevel() < 38 :
          htmltext = "30371-12.htm"
        else:
          htmltext = "30371-13.htm"
          st.takeItems(THALIAS_INSTRUCTIONS_ID,1)
          st.giveItems(THALIAS_LETTER2_ID,1)
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(THALIAS_LETTER2_ID) :
        htmltext = "30371-14.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ISAELS_INSTRUCTIONS_ID) :
        htmltext = "30371-15.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(TALINS_SPEAR_ID) and st.getQuestItemsCount(ISAELS_LETTER_ID) :
        htmltext = "30371-16.htm"
        st.takeItems(ISAELS_LETTER_ID,1)
        st.giveItems(GRAIL_OF_PURITY_ID,1)
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(TALINS_SPEAR_ID) and st.getQuestItemsCount(GRAIL_OF_PURITY_ID) :
        htmltext = "30371-17.htm"
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(TEARS_OF_UNICORN_ID) :
        htmltext = "30371-18.htm"
        st.takeItems(TEARS_OF_UNICORN_ID,1)
        st.giveItems(WATER_OF_LIFE_ID,1)
   elif npcId == 30371 and st.getInt("cond")==1 and st.getQuestItemsCount(CAMOMILE_CHARM_ID) or st.getQuestItemsCount(WATER_OF_LIFE_ID) and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30371-19.htm"
   elif npcId == 30300 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(GRAIL_DIAGRAM_ID) :
        htmltext = "30300-01.htm"
   elif npcId == 30300 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(PUSHKINS_LIST_ID) :
        if st.getQuestItemsCount(PURE_MITHRIL_ORE_ID) >= 10 and st.getQuestItemsCount(ANT_SOLDIER_ACID_ID) >= 20 and st.getQuestItemsCount(WYRMS_TALON1_ID) >= 20 :
          htmltext = "30300-08.htm"
        else:
          htmltext = "30300-07.htm"
   elif npcId == 30300 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(PURE_MITHRIL_CUP_ID) :
        htmltext = "30300-11.htm"
   elif npcId == 30300 and st.getInt("cond")==1 and st.getQuestItemsCount(GRAIL_DIAGRAM_ID) == 0 and st.getQuestItemsCount(PUSHKINS_LIST_ID) == 0 and st.getQuestItemsCount(PURE_MITHRIL_CUP_ID) == 0 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30300-12.htm"
   elif npcId == 30419 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(THALIAS_LETTER1_ID) :
        htmltext = "30419-01.htm"
   elif npcId == 30419 and st.getInt("cond")==1 and (st.getQuestItemsCount(ARKENIAS_INSTRUCTIONS_ID) or st.getQuestItemsCount(ADONIUS_LIST_ID)) and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30419-05.htm"
   elif npcId == 30419 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ANDARIEL_SCRIPTURE_COPY_ID) :
        htmltext = "30419-06.htm"
        st.takeItems(ARKENIAS_CONTRACT_ID,1)
        st.takeItems(ANDARIEL_SCRIPTURE_COPY_ID,1)
        st.giveItems(STARDUST_ID,1)
   elif npcId == 30419 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(STARDUST_ID) :
        htmltext = "30419-07.htm"
   elif npcId == 30419 and st.getInt("cond")==1 and st.getQuestItemsCount(THALIAS_LETTER1_ID) == 0 and st.getQuestItemsCount(ARKENIAS_CONTRACT_ID) == 0 and st.getQuestItemsCount(ANDARIEL_SCRIPTURE_COPY_ID) == 0 and st.getQuestItemsCount(STARDUST_ID) == 0 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30419-08.htm"
   elif npcId == 30375 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ARKENIAS_INSTRUCTIONS_ID) :
        htmltext = "30375-01.htm"
   elif npcId == 30375 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ADONIUS_LIST_ID) :
        if st.getQuestItemsCount(SPIDER_ICHOR_ID) >= 20 and st.getQuestItemsCount(HARPYS_DOWN_ID) >= 20 :
          htmltext = "30375-04.htm"
          st.takeItems(SPIDER_ICHOR_ID,st.getQuestItemsCount(SPIDER_ICHOR_ID))
          st.takeItems(HARPYS_DOWN_ID,st.getQuestItemsCount(HARPYS_DOWN_ID))
          st.takeItems(ADONIUS_LIST_ID,1)
          st.giveItems(ANDARIEL_SCRIPTURE_COPY_ID,1)
        else:
          htmltext = "30375-03.htm"
   elif npcId == 30375 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ANDARIEL_SCRIPTURE_COPY_ID) :
        htmltext = "30375-05.htm"
   elif npcId == 30375 and st.getInt("cond")==1 and st.getQuestItemsCount(ARKENIAS_INSTRUCTIONS_ID) == 0 and st.getQuestItemsCount(ADONIUS_LIST_ID) == 0 and st.getQuestItemsCount(ANDARIEL_SCRIPTURE_COPY_ID) == 0 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30375-06.htm"
   elif npcId == 30655 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(THALIAS_LETTER2_ID) :
        htmltext = "30655-01.htm"
   elif npcId == 30655 and st.getInt("cond")==1 and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) and st.getQuestItemsCount(ISAELS_INSTRUCTIONS_ID) :
        if st.getQuestItemsCount(TALINS_SPEAR_BLADE_ID) and st.getQuestItemsCount(TALINS_SPEAR_SHAFT_ID) and st.getQuestItemsCount(TALINS_RUBY_ID) and st.getQuestItemsCount(TALINS_AQUAMARINE_ID) and st.getQuestItemsCount(TALINS_AMETHYST_ID) and st.getQuestItemsCount(TALINS_PERIDOT_ID) :
          htmltext = "30655-04.htm"
          st.takeItems(TALINS_SPEAR_BLADE_ID,1)
          st.takeItems(TALINS_SPEAR_SHAFT_ID,1)
          st.takeItems(TALINS_RUBY_ID,1)
          st.takeItems(TALINS_AQUAMARINE_ID,1)
          st.takeItems(TALINS_AMETHYST_ID,1)
          st.takeItems(TALINS_PERIDOT_ID,1)
          st.takeItems(ISAELS_INSTRUCTIONS_ID,1)
          st.giveItems(ISAELS_LETTER_ID,1)
          st.giveItems(TALINS_SPEAR_ID,1)
        else:
          htmltext = "30655-03.htm"
   elif npcId == 30655 and st.getInt("cond")==1 and st.getQuestItemsCount(TALINS_SPEAR_ID) and st.getQuestItemsCount(ISAELS_LETTER_ID) :
        htmltext = "30655-05.htm"
   elif npcId == 30655 and st.getInt("cond")==1 and st.getQuestItemsCount(GRAIL_OF_PURITY_ID) or st.getQuestItemsCount(WATER_OF_LIFE_ID) or st.getQuestItemsCount(CAMOMILE_CHARM_ID) and st.getQuestItemsCount(MOONFLOWER_CHARM_ID)==1 :
        htmltext = "30655-06.htm"
   return htmltext

 def onKill(self,npc,player,isPet):
   st = player.getQuestState(qn)
   if not st : return
   if st.getState() != STARTED : return
   npcId = npc.getNpcId()
   if npcId == 20550 :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(PUSHKINS_LIST_ID) == 1 and st.getQuestItemsCount(PURE_MITHRIL_ORE_ID)<10 :
     if st.getRandom(100)<50 :
       st.giveItems(PURE_MITHRIL_ORE_ID,1)
       if st.getQuestItemsCount(PURE_MITHRIL_ORE_ID) < 10 :
         st.playSound("ItemSound.quest_itemget")
       else:
         st.playSound("ItemSound.quest_middle")
   elif npcId == 20176 :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(PUSHKINS_LIST_ID) == 1 and st.getQuestItemsCount(WYRMS_TALON1_ID)<20 :
     if st.getRandom(100)<50 :
      st.giveItems(WYRMS_TALON1_ID,1)
      if st.getQuestItemsCount(WYRMS_TALON1_ID) < 20 :
        st.playSound("ItemSound.quest_itemget")
      else:
        st.playSound("ItemSound.quest_middle")
   elif npcId in [ 20082,20084,20086] :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(PUSHKINS_LIST_ID) == 1 and st.getQuestItemsCount(ANT_SOLDIER_ACID_ID)<20 :
     if st.getRandom(100)<80 :
      st.giveItems(ANT_SOLDIER_ACID_ID,1)
      if st.getQuestItemsCount(ANT_SOLDIER_ACID_ID) < 20 :
        st.playSound("ItemSound.quest_itemget")
      else:
        st.playSound("ItemSound.quest_middle")
   elif npcId in [20087,20088] :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(PUSHKINS_LIST_ID) == 1 and st.getQuestItemsCount(ANT_SOLDIER_ACID_ID)<20 :
     if st.getRandom(100)<50 :
      st.giveItems(ANT_SOLDIER_ACID_ID,1)
      if st.getQuestItemsCount(ANT_SOLDIER_ACID_ID) < 20 :
        st.playSound("ItemSound.quest_itemget")
      else:
        st.playSound("ItemSound.quest_middle")
   elif npcId == 20233 :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(ADONIUS_LIST_ID) == 1 and st.getQuestItemsCount(SPIDER_ICHOR_ID)<20 :
     if st.getRandom(100)<50 :
      st.giveItems(SPIDER_ICHOR_ID,1)
      if st.getQuestItemsCount(SPIDER_ICHOR_ID) < 20 :
        st.playSound("ItemSound.quest_itemget")
      else:
        st.playSound("ItemSound.quest_middle")
   elif npcId == 20145 :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(ADONIUS_LIST_ID) == 1 and st.getQuestItemsCount(HARPYS_DOWN_ID)<20 :
     if st.getRandom(100)<50 :
      st.giveItems(HARPYS_DOWN_ID,1)
      if st.getQuestItemsCount(HARPYS_DOWN_ID) < 20 :
        st.playSound("ItemSound.quest_itemget")
      else:
        st.playSound("ItemSound.quest_middle")
   elif npcId == 27077 :
    if st.getInt("cond") and st.getQuestItemsCount(MOONFLOWER_CHARM_ID) == 1 and st.getQuestItemsCount(TALINS_SPEAR_ID) == 1 and st.getQuestItemsCount(GRAIL_OF_PURITY_ID) == 1 and st.getQuestItemsCount(TEARS_OF_UNICORN_ID) == 0 :
      if st.getQuestItemsCount(TALINS_SPEAR_ID) > 0 :
        st.takeItems(GRAIL_OF_PURITY_ID,1)
        st.takeItems(TALINS_SPEAR_ID,1)
        st.giveItems(TEARS_OF_UNICORN_ID,1)
   elif npcId in [20581,20582] :
    st.set("id","0")
    if st.getInt("cond") and st.getQuestItemsCount(ISAELS_INSTRUCTIONS_ID) == 1 and st.getRandom(100) < 50 :
     if st.getQuestItemsCount(TALINS_SPEAR_BLADE_ID) == 0 :
      st.giveItems(TALINS_SPEAR_BLADE_ID,1)
      st.playSound("ItemSound.quest_itemget")
     elif st.getQuestItemsCount(TALINS_SPEAR_SHAFT_ID) == 0 :
      st.giveItems(TALINS_SPEAR_SHAFT_ID,1)
      st.playSound("ItemSound.quest_itemget")
     elif st.getQuestItemsCount(TALINS_RUBY_ID) == 0 :
      st.giveItems(TALINS_RUBY_ID,1)
      st.playSound("ItemSound.quest_itemget")
     elif st.getQuestItemsCount(TALINS_AQUAMARINE_ID) == 0 :
      st.giveItems(TALINS_AQUAMARINE_ID,1)
      st.playSound("ItemSound.quest_itemget")
     elif st.getQuestItemsCount(TALINS_AMETHYST_ID) == 0 :
      st.giveItems(TALINS_AMETHYST_ID,1)
      st.playSound("ItemSound.quest_itemget")
     elif st.getQuestItemsCount(TALINS_PERIDOT_ID) == 0 :
      st.giveItems(TALINS_PERIDOT_ID,1)
      st.playSound("ItemSound.quest_middle")
   return

QUEST       = Quest(218,qn,"Testimony of Life")
CREATED     = State('Start', QUEST)
STARTING     = State('Starting', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)


QUEST.setInitialState(CREATED)
QUEST.addStartNpc(30460)

QUEST.addTalkId(30460)

QUEST.addTalkId(30154)
QUEST.addTalkId(30300)
QUEST.addTalkId(30371)
QUEST.addTalkId(30375)
QUEST.addTalkId(30419)
QUEST.addTalkId(30655)

QUEST.addKillId(20145)
QUEST.addKillId(20176)
QUEST.addKillId(20233)
QUEST.addKillId(27077)
QUEST.addKillId(20550)
QUEST.addKillId(20581)
QUEST.addKillId(20582)
QUEST.addKillId(20082)
QUEST.addKillId(20084)
QUEST.addKillId(20086)
QUEST.addKillId(20087)
QUEST.addKillId(20088)

STARTED.addQuestDrop(30154,CAMOMILE_CHARM_ID,1)
STARTED.addQuestDrop(30460,CARDIENS_LETTER_ID,1)
STARTED.addQuestDrop(30371,WATER_OF_LIFE_ID,1)
STARTED.addQuestDrop(30154,MOONFLOWER_CHARM_ID,1)
STARTED.addQuestDrop(30154,HIERARCHS_LETTER_ID,1)
STARTED.addQuestDrop(30419,STARDUST_ID,1)
STARTED.addQuestDrop(30300,PURE_MITHRIL_CUP_ID,1)
STARTED.addQuestDrop(30371,THALIAS_INSTRUCTIONS_ID,1)
STARTED.addQuestDrop(30655,ISAELS_LETTER_ID,1)
STARTED.addQuestDrop(27077,TEARS_OF_UNICORN_ID,1)
STARTED.addQuestDrop(30371,GRAIL_DIAGRAM_ID,1)
STARTED.addQuestDrop(30300,PUSHKINS_LIST_ID,1)
STARTED.addQuestDrop(30371,THALIAS_LETTER1_ID,1)
STARTED.addQuestDrop(30419,ARKENIAS_CONTRACT_ID,1)
STARTED.addQuestDrop(30375,ANDARIEL_SCRIPTURE_COPY_ID,1)
STARTED.addQuestDrop(30419,ARKENIAS_INSTRUCTIONS_ID,1)
STARTED.addQuestDrop(30375,ADONIUS_LIST_ID,1)
STARTED.addQuestDrop(30371,THALIAS_LETTER2_ID,1)
STARTED.addQuestDrop(20581,TALINS_SPEAR_BLADE_ID,1)
STARTED.addQuestDrop(20581,TALINS_SPEAR_SHAFT_ID,1)
STARTED.addQuestDrop(20581,TALINS_RUBY_ID,1)
STARTED.addQuestDrop(20581,TALINS_AQUAMARINE_ID,1)
STARTED.addQuestDrop(20581,TALINS_AMETHYST_ID,1)
STARTED.addQuestDrop(20581,TALINS_PERIDOT_ID,1)
STARTED.addQuestDrop(30655,ISAELS_INSTRUCTIONS_ID,1)
STARTED.addQuestDrop(30371,GRAIL_OF_PURITY_ID,1)