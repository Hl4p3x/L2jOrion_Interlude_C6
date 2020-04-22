# Rewritten by RayzoR
import sys
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.model.base import Race

qn = "236_SeedsOfChaos"

DROP_RATE = 20

#prerequisites:
STAR_OF_DESTINY = 5011

#Quest items
BLACK_ECHO_CRYSTAL = 9745
SHINING_MEDALLION = 9743
#How many of each do you need?
NEEDED = {
    BLACK_ECHO_CRYSTAL: 1,
    SHINING_MEDALLION: 62
    }

SCROLL_ENCHANT_WEAPON_A = 729

#NPCs
KEKROPUS,WIZARD,KATENAR,ROCK,HARKILGAMED,MAO,RODENPICULA,NORNIL = 32138,31522,32235,32238,32334,32190,32237,32239

#Mobs
NEEDLE_STAKATO_DRONE = [21516,21517]
SPLENDOR_MOBS = [21520,21521,21522,21523,21524,21525,21526,21527,21528,21529,21530,21531,21532,21533,21534,21535,21536,21537,21538,21539,21540,21541]
#Mobs, cond, Drop
DROPLIST = {
#Needle Stakato Drones
    21516: [2,BLACK_ECHO_CRYSTAL],
    21517: [2,BLACK_ECHO_CRYSTAL],
#Splendor Mobs
    21520: [12,SHINING_MEDALLION],
    21521: [12,SHINING_MEDALLION],
    21522: [12,SHINING_MEDALLION],
    21523: [12,SHINING_MEDALLION],
    21524: [12,SHINING_MEDALLION],
    21525: [12,SHINING_MEDALLION],
    21526: [12,SHINING_MEDALLION],
    21527: [12,SHINING_MEDALLION],
    21528: [12,SHINING_MEDALLION],
    21529: [12,SHINING_MEDALLION],
    21530: [12,SHINING_MEDALLION],
    21531: [12,SHINING_MEDALLION],
    21532: [12,SHINING_MEDALLION],
    21533: [12,SHINING_MEDALLION],
    21534: [12,SHINING_MEDALLION],
    21535: [12,SHINING_MEDALLION],
    21536: [12,SHINING_MEDALLION],
    21537: [12,SHINING_MEDALLION],
    21538: [12,SHINING_MEDALLION],
    21539: [12,SHINING_MEDALLION],
    21540: [12,SHINING_MEDALLION],
    21541: [12,SHINING_MEDALLION]
    }

class Quest (JQuest) :
 
 def __init__(self,id,name,descr): 
    JQuest.__init__(self,id,name,descr)
    self.katenar = self.harkil = 0
    self.questItemId = [BLACK_ECHO_CRYSTAL, SHINING_MEDALLION]
 
 def onEvent (self,event,st) :
    if event == "1" : #Go talk to the wizard!
        st.setState(STARTED)
        st.set("cond","1")
        st.playSound("ItemSound.quest_accept")
        htmltext = "32138_02b.htm"
    elif event == "1_yes" : #Ok, know about those Stakato Drones?
        htmltext = "31522_01c.htm"
    elif event == "1_no" : #You suck.  Come back when you want to talk
        htmltext = "31522_01no.htm"
    elif event == "2" : #Get me the crystal
        st.set("cond","2")
        htmltext = "31522_02.htm"
    elif event == "31522_03b" :
        st.takeItems(BLACK_ECHO_CRYSTAL,-1)
        htmltext = event + ".htm"
    elif event == "4" : #Time to summon this bad boy
        st.set("cond","4")
        if not self.katenar :
            st.addSpawn(KATENAR,120000)
            self.katenar = 1
            st.startQuestTimer("Despawn_Katenar",120000)
        return
    elif event == "5" : #gotta go.  talk to Harkilgamed
        st.set("cond","5")
        htmltext = "32235_02.htm"
    elif event == "spawn_harkil" : #talk to the rock, this spawns Harkilgamed
        if not self.harkil :
            st.addSpawn(HARKILGAMED,120000)
            self.hark = 1
            st.startQuestTimer("Despawn_Harkil",120000)
        return
    elif event == "6" : #now go hunt splendor mobs
        st.set("cond","12")
        htmltext = "32236_06.htm"
    elif event == "8" : #go back to Kekropus
        st.set("cond","14")
        htmltext = "32236_08.htm"
    elif event == "9" : #Go talk to Mao, no not the dictator Mao... the Vice Hierarch Mao.  <_<
        st.set("cond","15")
        htmltext = "32138_09.htm"
    elif event == "10" : #This is where you can find Rodenpicula.  
        st.set("cond","16")
        st.getPlayer().teleToLocation(-119534,87176,-12593)
        htmltext = "32190_02.htm"
    elif event == "11" : #Go talk to Mother Nornil now
        st.set("cond","17")
        htmltext = "32237_11.htm"
    elif event == "12" : #Get Rodenpicula's permission
        st.set("cond","18")
        htmltext = "32239_12.htm"
    elif event == "13" : #LETS DO THIS!!
        st.set("cond","19")
        htmltext = "32237_13.htm"
    elif event == "14" : #LEROOY JENKINS!!!!   Finish the quest at Rodenpicula
        st.set("cond","20")
        htmltext = "32239_14.htm"
    elif event == "15" : #done done done!!!
        st.giveItems(SCROLL_ENCHANT_WEAPON_A,1)
        st.setState(COMPLETED)
        htmltext = "32237_15.htm"
    elif event == "Despawn_Katenar" :
        self.katenar = 0
        return
    elif event == "Despawn_Harkil" :
        self.harkil = 0
        return
    else :
        htmltext = event + ".htm" #this is for having to go through pages upon pages of html text... <_<
    return htmltext
 
 def onTalk (self,npc,player):
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>"
    st = player.getQuestState(qn)
    if not st : return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if npcId == KEKROPUS :
        if id == CREATED :
            st.set("cond","0")
            if player.getRace() != Race.Kamael :
                st.exitQuest(1)
                htmltext = "<html><body>I'm sorry, but I can only give this quest to Kamael. Talk to Magister Ladd.</body></html>"
            elif player.getLevel() < 75 :
                st.exitQuest(1)
                htmltext = "32138_01.htm"     #not qualified
            elif not st.getQuestItemsCount(STAR_OF_DESTINY) :
                st.exitQuest(1)
                htmltext = "32138_01a.htm"   #not qualified
            else :
                htmltext = "32138_02.htm"    # Successful start: Talk to me a few times,
        elif id == STARTED :
            if cond < 14 :
                htmltext = "32138_02c.htm"
            elif cond == 14:
                htmltext = "32138_08.htm"
            else :
                htmltext = "<html><body>Kekropus:<br>Go talk to Rodenpicula. Mao can help you get to her.</body></html>"
        elif id == COMPLETED :
            htmltext = "<html><body>You have already completed this quest.</body></html>"
    elif npcId == WIZARD and id == STARTED:
       # first time talking to Wizard. Talk a bit
        if cond==1 :
            htmltext = "31522_01.htm"      
        # Why are you back alraedy?  You don't have the echo crystal
        elif cond==2 :
            htmltext = "31522_02a.htm"     # you haven't gotten the crystal yet?
        # aha!  Here is the black echo crystal!  Now where's that one chap?
        elif cond == 3 or (cond == 4 and not self.katenar) :
            htmltext = "31522_03.htm"     # ah yes.  Now you get to talk to this guy that I will soon summon
        else :
            htmltext = "31522_04.htm"     #shouldn't you be talking to Katenar?
    elif npcId == KATENAR and id == STARTED:
        if cond == 4:
            htmltext = "32235_01.htm"
        elif cond >= 5:
            htmltext = "32235_02.htm"
    elif npcId == ROCK and id == STARTED:
        if cond == 5 or cond == 13:
            htmltext = "32238.htm" #click the link to spawn Harkilgamed
        else:
            htmltext = "<html><body>A strange rock...</body></html>"
    elif npcId == HARKILGAMED and id == STARTED:
        if cond == 5:
            htmltext = "32236_05.htm" #First time talking to Harkilgamed
        elif cond == 12:
            htmltext = "32236_06.htm" #Kill the Splendor mobs, bring back 62 Shining Medallions
        elif cond == 13:
            st.takeItems(SHINING_MEDALLION,-1)
            htmltext = "32236_07.htm"
        elif cond > 13:
            htmltext = "<html><body>Harkilgamed:<br><br>Go talk to Kekropus already.</body></html>"
    elif npcId == MAO and id == STARTED: #Ok.  The deal with Mao is that he's supposed to port you to Mother Nornil, but since she's not yet in the spawnlist, he's just gonna tell ya where to find her.
        #THIS MEANS: WHOEVER SPAWNS NORNIL AND RODENPICULA MUST WRITE THE FOLLOWING .htm FILE ACCORDINGLY
        if cond == 15 or cond == 16:
            htmltext = "32190_01.htm"
    elif npcId == RODENPICULA and id==STARTED:
        if cond == 16:
            htmltext = "32237_10.htm" #heys.  long talk, figure stuff out
        elif cond == 17:
            htmltext = "32237_11.htm" #talk to nornil already
        elif cond == 18:
            htmltext = "32237_12.htm" #you want approval
        elif cond == 19:
            htmltext = "32237_13.htm" #here's approval, talk to her
        elif cond == 20:
            htmltext = "32237_14.htm" #congrats.  here's a scroll
    elif npcId == NORNIL and id==STARTED:
        if cond == 17:
            htmltext = "32239_11.htm" #yo.  get rodenpicula's approval
        elif cond == 18:
            htmltext = "32239_12.htm" #i need rodenpicula's approval
        elif cond == 19:
            htmltext = "32239_13.htm" #lets get it over with
        elif cond == 20:
            htmltext = "32239_14.htm" #you're good.  talk to roden one more time
    return htmltext
 
 def onKill(self,npc,player,isPet):
    st = player.getQuestState(qn)
    if not st : return 
    if st.getState() != STARTED : return 
    #The following algorithm should work for both quest mobs and drops for this quest.... hopefully.
    npcId = npc.getNpcId()
    dropcond, item = DROPLIST[npcId]
    drop = st.getRandom(100)
    cond = st.getInt("cond")
    if drop < DROP_RATE and cond == dropcond :
        if st.getQuestItemsCount(item) < NEEDED[item] :
            st.giveItems(item,1)
            st.playSound("ItemSound.quest_itemget")
            if st.getQuestItemsCount(item) == NEEDED[item]:
                st.set("cond",str(cond+1))
    return

QUEST = Quest(236,qn,"Seeds of Chaos")
CREATED     = State('Start', QUEST)
STARTED     = State('Started', QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(KEKROPUS)

QUEST.addTalkId(KEKROPUS)
QUEST.addTalkId(WIZARD)
QUEST.addTalkId(KATENAR)
QUEST.addTalkId(ROCK)
QUEST.addTalkId(HARKILGAMED)
QUEST.addTalkId(MAO)
QUEST.addTalkId(RODENPICULA)
QUEST.addTalkId(NORNIL)

for i in DROPLIST.keys():
  QUEST.addKillId(i)
