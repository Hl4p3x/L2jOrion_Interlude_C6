import sys
from l2jorion.game.ai import CtrlIntention
from l2jorion.game.managers import GrandBossManager
from l2jorion.game.model.quest import State
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest
from l2jorion.game.network.serverpackets import SocialAction
from l2jorion.game.network.serverpackets import SpecialCamera
from l2jorion.util.random import Rnd
from java.lang import System

STATUE = 32109
SAILREN = 29065
VELO = 22196
PTERO = 22199
TREX = 22215

STONE = 8784

class Sailren(JQuest) :

 def __init__(self,id,name,descr):
   self.vlc = []
   self.ptr = []
   self.trx = []
   self.slrn = []
   JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player):
   if event == "start" :
     self.vlc = self.addSpawn(VELO,27845,-5567,-1982,45000,False,0)
     self.startQuestTimer("camera",2000, self.vlc, player)
     self.cancelQuestTimer("start",npc,None)
   if event == "round2" :
     self.ptr = self.addSpawn(PTERO,27838,-5578,-1982,45000,False,0)
     self.startQuestTimer("camera",2000, self.ptr, player)
     self.cancelQuestTimer("round2",npc,None)
   if event == "round3" :
     self.trx = self.addSpawn(TREX,27838,-5578,-1982,45000,False,0)
     self.startQuestTimer("camera",2000, self.trx, player)
     self.cancelQuestTimer("round3",npc,None)
   if event == "sailren" :
     self.slrn = self.addSpawn(SAILREN,27489,-6223,-1982,45000,False,0)
     self.startQuestTimer("camera",2000, self.slrn, player)
     self.startQuestTimer("vkrovatku",1200000, self.slrn, None)
     self.cancelQuestTimer("round4",npc,None)
   elif event == "camera" :
     player.broadcastPacket(SpecialCamera(npc.getObjectId(),400,-75,3,-150,5000))
     npc.broadcastPacket(SocialAction(npc.getObjectId(),1))
   elif event == "open" :
     self.deleteGlobalQuestVar("close")  
     self.cancelQuestTimer("open",npc,None)
   elif event == "vkrovatku" :
     npc.deleteMe()
     self.deleteGlobalQuestVar("close")
     self.cancelQuestTimer("open",npc,None)
     self.cancelQuestTimer("vkrovatku",npc,None)
   return
        
 def onTalk (self,npc,player):
   st = player.getQuestState("sailren")  
   npcId = npc.getNpcId()
   close = self.loadGlobalQuestVar("close")
   time = self.loadGlobalQuestVar("time")
   party = player.getParty()
   if npcId == STATUE :
     if st.getQuestItemsCount(STONE) >= 1:
       if close == "" :
         if party:
           for player in party.getPartyMembers() :
             st.takeItems(STONE,1)
             self.saveGlobalQuestVar("close", "1")
             zone = GrandBossManager.getInstance().getZone(27244,-7026,-1974)
             if zone:
               zone.allowPlayerEntry(player, 3600)
             player.teleToLocation(27244,-7026,-1974)
             self.startQuestTimer("start",30000,npc,player)
             self.startQuestTimer("open",1800000,npc,None)
         else :
           return "<html><body><font color=LEVEL>Only with party...</font></body></html>"
       else :
         return "<html><body><font color=LEVEL>Some one else is inside...</font></body></html>"
     else :
       return "<html><body>You need quest item: <font color=LEVEL>Gazkh...</font></body></html>"
   return

 def onKill(self,npc,player,isPet):
   if npc == self.vlc :
     self.startQuestTimer("round2",30000,npc,player)
   if npc == self.ptr :
     self.startQuestTimer("round3",60000,npc,player)
   if npc == self.trx :
     self.startQuestTimer("sailren",180000,npc,player)
   if npc == self.slrn :
     self.deleteGlobalQuestVar("close")
     self.cancelQuestTimer("open",npc,None)
   return

QUEST = Sailren(-1,"sailren","ai")
CREATED = State('Start',QUEST)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(STATUE)
QUEST.addTalkId(STATUE)
QUEST.addKillId(VELO)
QUEST.addKillId(PTERO)
QUEST.addKillId(TREX)
QUEST.addKillId(SAILREN)