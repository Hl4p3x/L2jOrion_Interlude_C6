import sys
from l2jorion import Config
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "126_TheNameofEvilPart2"

#NPCs
ASAMAH = 32115
MUSHIKA = 32114
STATUE = 32119
STATUEE = 32120
STATUEEE = 32121
SHSTATUE = 32109
WARGRAVE = 32122

#Items
BONEPOWDER = 8783
EPITAPH = 8781
EWA = 729
ADENA = 57

class Quest (JQuest) :
 def __init__(self,id,name,descr):
    JQuest.__init__(self,id,name,descr)

 def onEvent (self,event,st) :
    htmltext = event
    if event == "proklatie.htm" :
       st.setState(STARTED)
       st.playSound("ItemSound.quest_accept")
       st.set("cond","1")
    elif event == "statui.htm" :
       st.set("cond","2")
    elif event == "statuesvedenia.htm" :
       st.set("cond","3")
    elif event == "statuemelodia.htm" :
       st.set("cond","4")
    elif event == "statuepoka.htm" :
       st.set("cond","5")
    elif event == "statueevoprosss.htm" :
       st.set("cond","6")
    elif event == "statueemelodia.htm" :
       st.set("cond","7")    
    elif event == "statueepoka.htm" :
       st.set("cond","8")    
    elif event == "statueeevopross.htm" :
       st.set("cond","9")      
    elif event == "statueeemelodia.htm" :
       st.set("cond","10")      
    elif event == "statueeepoka.htm" :
       st.set("cond","11")      
    elif event == "wargravevopross.htm" :
       st.set("cond","13")      
    elif event == "wargravevoprosss.htm" :
       st.set("cond","14")       
    elif event == "wargravertodinok.htm" :
       st.set("cond","15")        
    elif event == "wargravertdwaok.htm" :
       st.set("cond","16")        
    elif event == "wargraverttriok.htm" :
       st.set("cond","17")        
    elif event == "wargraveprah.htm" :
       st.set("cond","18")
    elif event == "wargraveritualok.htm" :
       st.giveItems(BONEPOWDER,1)  
    elif event == "shstatuee.htm" :
       st.set("cond","19")        
    elif event == "shstatueritual.htm" :
       st.set("cond","20")
       st.takeItems(BONEPOWDER,1)
    elif event == "vernulsaaa.htm" :
       st.set("cond","21")        
    elif event == "gazkh.htm" :
       st.set("cond","22")        
    elif event == "theend.htm" :
       st.playSound("ItemSound.quest_finish")
       st.giveItems(EWA,1)
       st.giveItems(ADENA,298496)
       st.unset("cond")
       st.setState(COMPLETED) 
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    epith = st.getQuestItemsCount(EPITAPH)
    if npcId == ASAMAH :
       if id == CREATED :
          st2 = player.getQuestState("125_TheNameofEvilPart1")
          if st2 :
             if st2.getState().getName() == 'Completed' :
                if player.getLevel() >= 77 :
                   htmltext = "privetstvie.htm"
                else :
                   htmltext = "lvl.htm"
                   st.exitQuest(1)
          else :
             htmltext = "no.htm"
             st.exitQuest(1)
       elif cond == 2:
          htmltext = "statui.htm"
       elif cond == 20:
          htmltext = "vernulsa.htm"
       elif cond == 21:
          htmltext = "gazkh.htm"
    elif npcId == STATUE :
       if cond == 2:
          htmltext = "statue.htm"
       if cond == 3:
          htmltext = "statuesvedenia.htm"
       if cond == 4:
          htmltext = "statuemelodia.htm"
       if cond == 5:
          htmltext = "statuepoka.htm"
    elif npcId == STATUEE :
       if cond == 5:
          htmltext = "statuee.htm"
       if cond == 6:
          htmltext = "statueevoprosss.htm"
       if cond == 7:
          htmltext = "statueemelodia.htm"
       if cond == 8:
          htmltext = "statueepoka.htm"
    elif npcId == STATUEEE :
       if cond == 8:
          htmltext = "statueee.htm"
       if cond == 9:
          htmltext = "statueeevopross.htm"
       if cond == 10:
          htmltext = "statueeemelodia.htm"
       if cond == 11:
          htmltext = "statueeepoka.htm"
    elif npcId == WARGRAVE :
       if cond == 11:
          htmltext = "wargrave.htm"
          st.set("cond","12")
       if cond == 12:
          htmltext = "wargravemolitva.htm"
       if cond == 13:
          htmltext = "wargravevopross.htm"
       if cond == 14:
          htmltext = "wargraveritual.htm"
       if cond == 15:
          htmltext = "wargravertdwa.htm"
       if cond == 16:
          htmltext = "wargraverttri.htm"
       if cond == 17:
          htmltext = "wargraveritualok.htm"
       if cond == 18:
          htmltext = "wargraveprah.htm"
    elif npcId == SHSTATUE :
       if cond == 18:
          htmltext = "shstatue.htm"
       if cond == 19:
          htmltext = "shstatuee.htm"
       if cond == 20:
          htmltext = "shstatueritual.htm"
    elif npcId == MUSHIKA :
       if cond == 22:
          htmltext = "mushika.htm"
          st.set("cond","23")
       if cond == 23:
          htmltext = "theend.htm"
    return htmltext

QUEST = Quest(126,qn,"The Name Of Evil - 2")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(ASAMAH)
QUEST.addTalkId(ASAMAH)
QUEST.addTalkId(WARGRAVE)
QUEST.addTalkId(STATUE)
QUEST.addTalkId(STATUEE)
QUEST.addTalkId(STATUEEE)
QUEST.addTalkId(SHSTATUE)
QUEST.addTalkId(MUSHIKA)
