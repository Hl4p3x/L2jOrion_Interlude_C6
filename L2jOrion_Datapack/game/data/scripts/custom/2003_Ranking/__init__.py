import sys
from l2jorion.game.model.actor.instance import L2PcInstance
from java.util import Iterator
from l2jorion.game.datatables import SkillTable
from l2jorion.util.database import L2DatabaseFactory
from l2jorion.game.model.actor.appearance import PcAppearance
from l2jorion.game.model.quest import State
from l2jorion.game.model.actor.appearance import PcAppearance
from l2jorion.game.model.quest import QuestState
from l2jorion.game.model.quest.jython import QuestJython as JQuest

qn = "2003_Ranking"

NPC=[14]
PriceID = 57
PriceCount = 0
QuestId     = 2003
QuestName   = "Ranking"
QuestDesc   = "custom"
InitialHtml = "1.htm"

print "              Ranking system                          [ Ok ]"

class Quest (JQuest) :

	def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

	def onTalk (self,npc,player):
		return InitialHtml

	def onEvent(self,event,st):
		htmltext = event
		price = st.getQuestItemsCount(PriceID)
		
		# - - - - - - - - -
		# +  PvP Ranking  +
		# - - - - - - - - -
		if event == "1" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><font color =\"0066CC\">Rank</td><td><center><font color =\"0066CC\">Name</color></center></td><td><center><font color =\"0066CC\">PvP's</color></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT char_name,pvpkills FROM characters WHERE pvpkills>0 and accesslevel=0 order by pvpkills desc limit 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				char_pkkills = rs.getString("pvpkills")
				total_asesinados = total_asesinados + int(char_pkkills)
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"LEVEL\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
			htmltext_end = "</table><center><br><font color=\"0066CC\">" + "Total:</font> " + str(total_asesinados) + "<font color=\"FFFFFF\"> PvP's</font><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "1" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
		
		# - - - - - - - - -
		# +  PK Ranking   +
		# - - - - - - - - -
		if event == "2" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><font color =\"0066CC\">Rank</td><td><center><font color =\"0066CC\">Name</color></center></td><td><center><font color =\"0066CC\">PK's</color></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT char_name,pkkills FROM characters WHERE pkkills>0 and accesslevel=0 order by pkkills desc limit 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				char_pkkills = rs.getString("pkkills")
				total_asesinados = total_asesinados + int(char_pkkills)
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
			htmltext_end = "</table><center><br><font color=\"0066CC\">" + "Total:</font> " + str(total_asesinados) + "<font color=\"FFFFFF\"> PK's</font><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "2" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext

		# - - - - - - - - - - -
		# +  Enchants +
		# - - - - - - - - - - -
		if event == "3" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=300><tr><td><font color =\"0066CC\">Rank</font></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">Enchants</font></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT characters.char_name, characters.accesslevel, characters.title, characters.obj_Id, characters.clanid, characters.base_class, char_templates.ClassId, char_templates.ClassName, items.owner_id, items.item_id, items.enchant_level, weapon.item_id, weapon.name, weapon.soulshots, weapon.crystal_type, clan_data.clan_id, clan_data.clan_name, clan_data.ally_id, clan_data.ally_name, clan_data.crest_id, clan_data.ally_crest_id FROM characters INNER JOIN items ON characters.obj_Id=items.owner_id INNER JOIN char_templates ON characters.base_class=char_templates.ClassId INNER JOIN weapon ON items.item_id=weapon.item_id LEFT JOIN clan_data ON characters.clanid=clan_data.clan_id WHERE (weapon.crystal_type!='none' AND weapon.soulshots>'0') AND characters.accesslevel='0' ORDER BY items.enchant_level DESC LIMIT 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				name = rs.getString("name")
				enchants = rs.getString("enchant_level")
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</font></td><td><center>" + color_text + char_name +"</font></center></td><td><center>" + name + " <font color=\"3399ff\">+" + enchants + "</font></center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</font></td><td><center>" + color_text + char_name +"</font></center></td><td><center>" + name + " <font color=\"3399ff\">+" + enchants + "</font></center></td></tr>"
			htmltext_end = "</table><center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "2" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
		# - - - - - - - - - - -
		# +  jewel +
		# - - - - - - - - - - -
		if event == "7" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=300><tr><td><font color =\"0066CC\">Rank</font></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">Boss Jewelry</font></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT characters.char_name, characters.accesslevel, characters.title, characters.obj_Id, characters.clanid, items.owner_id, items.item_id, items.enchant_level, char_templates.ClassId, char_templates.ClassName, armor.name, clan_data.clan_id, clan_data.clan_name, clan_data.ally_id, clan_data.ally_name, clan_data.crest_id, clan_data.ally_crest_id FROM characters INNER JOIN items ON characters.obj_Id=items.owner_id INNER JOIN char_templates ON characters.base_class=char_templates.ClassId INNER JOIN armor ON items.item_id=armor.item_id LEFT JOIN clan_data ON characters.clanid=clan_data.clan_id WHERE ((armor.item_id BETWEEN 6656 AND 6662) OR armor.item_id='8191') AND characters.accesslevel='0' ORDER BY items.enchant_level DESC LIMIT 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				name = rs.getString("name")
				enchants = rs.getString("enchant_level")
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</font></td><td><center>" + color_text + char_name +"</font></center></td><td><center>" + name + " <font color=\"3399ff\">+" + enchants + "</font></center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</font></td><td><center>" + color_text + char_name +"</font></center></td><td><center>" + name + " <font color=\"3399ff\">+" + enchants + "</font></center></td></tr>"
			htmltext_end = "</table><center><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "2" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
		# - - - - - - - - - - -
		# + level +
		# - - - - - - - - - - -
		if event == "4" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><font color =\"0066CC\">Rank</td><td><center><font color =\"0066CC\">Name</color></center></td><td><center><font color =\"0066CC\">Level</color></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT char_name,level FROM characters WHERE level>1 and accesslevel=0 order by level desc limit 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				char_onlinetime = rs.getString("level")
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_onlinetime + "</center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_onlinetime + "</center></td></tr>"
			htmltext_end = "</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "4" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
			
		# - - - - -
		# + Clan +
		# - - - - -	
		if event == "5" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=230><tr><td><font color =\"0066CC\">Rank</font></td><td><center><font color =\"0066CC\">Level</font></center></td><td><center><font color =\"0066CC\">Name</font></center></td><td><center><font color =\"0066CC\">Reputation</font></center></td></tr>"
			htmltext_info =""
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			clan = con.prepareStatement("SELECT clan_name,clan_level,reputation_score,hasCastle FROM clan_data WHERE clan_level>0 order by reputation_score desc limit 10")
			rs = clan.executeQuery()
			while (rs.next()) :
				clan_name = rs.getString("clan_name")
				clan_level = rs.getString("clan_level")
				clan_score = rs.getString("reputation_score")
				hasCastle = rs.getString("hasCastle")
				pos = pos + 1
				posstr = str(pos)
					
				htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</font></center></td><td><center>" + clan_level +"</center></td><td><center>" + clan_name + "</center></td><td><center>" + clan_score + "</center></td></tr>"
			htmltext_end = "</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "5" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
            
		 # - - - - - - - -
		 # + Castles +
	     # - - - - - - - -				
		if event == "6" :
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><center><font color =\"0066CC\">Castle</font></center></td><td><center><font color =\"0066CC\">Tax</font></center></td><td><center><font color =\"0066CC\">Owner</font></center></td></tr>"
			htmltext_info = ""
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT id,name,taxPercent FROM `castle`")
			rs = pks.executeQuery()
			while (rs.next()) :
				castle_id = rs.getString("id")
				castle_name = rs.getString("name")
				tax = rs.getString("taxPercent")
				clan = con.prepareStatement("SELECT clan_name,hasCastle FROM clan_data WHERE hasCastle = "+castle_id+"")
				rs2 = clan.executeQuery()
				while (rs2.next()) :
					cname = rs2.getString("clan_name")
					htmltext_info = htmltext_info + "<tr><td><center>" + castle_name + "</center></td><td><center>" + tax +"</center></td><td><center>"+cname+"</center></td></tr>"
			htmltext_end = "</table><center><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
			
		# - - - - - - - - -
		# +  PvP 24h Ranking  +
		# - - - - - - - - -
		if event == "8" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><font color =\"0066CC\">Rank</td><td><center><font color =\"0066CC\">Name</color></center></td><td><center><font color =\"0066CC\">PvP's</color></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT char_name,pvpkills FROM 24hPvpPk WHERE pvpkills>0 order by pvpkills desc limit 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				char_pkkills = rs.getString("pvpkills")
				total_asesinados = total_asesinados + int(char_pkkills)
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"LEVEL\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
			htmltext_end = "</table><center><br><font color=\"0066CC\">" + "Total:</font> " + str(total_asesinados) + "<font color=\"FFFFFF\"> PvP's</font><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pvplist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pvplist
		elif event == "7" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext
		
		# - - - - - - - - -
		# +  PK 24h Ranking   +
		# - - - - - - - - -
		if event == "9" and price >= PriceCount :
			st.takeItems(PriceID,PriceCount)
			total_asesinados = 0
			htmltext_ini = "<html><head><center><title>TOP 10</title></head><body><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><table width=200><tr><td><font color =\"0066CC\">Rank</td><td><center><font color =\"0066CC\">Name</color></center></td><td><center><font color =\"0066CC\">PK's</color></center></td></tr><tr></tr>"
			htmltext_info =""			
			color = 1
			pos = 0
			con = L2DatabaseFactory.getInstance().getConnection(False)
			pks = con.prepareStatement("SELECT char_name,pkkills FROM 24hPvpPk WHERE pkkills>0 order by pkkills desc limit 10")
			rs = pks.executeQuery()
			while (rs.next()) :
				char_name = rs.getString("char_name")
				char_pkkills = rs.getString("pkkills")
				total_asesinados = total_asesinados + int(char_pkkills)
				pos = pos + 1
				posstr = str(pos)
				if color == 1:
					color_text = "<font color =\"FFFFFF\">"
					color = 2
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
				elif color == 2:
					color_text = "<font color =\"FFFFFF\">"
					color = 1
					htmltext_info = htmltext_info + "<tr><td><center><font color =\"FFFF00\">" + posstr + "</td><td><center>" + color_text + char_name +"</center></td><td><center>" + char_pkkills + "</center></td></tr>"
			htmltext_end = "</table><center><br><font color=\"0066CC\">" + "Total:</font> " + str(total_asesinados) + "<font color=\"FFFFFF\"> PK's</font><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32></center></body></html>"
			htmltext_pklist = htmltext_ini + htmltext_info + htmltext_end
			con.close()
			return htmltext_pklist
		elif event == "8" and price < PriceCount :
			htmltext = "<html><body><center><title>TOP 10</title><br><br><img src=\"l2font-e.replay_logo-e\" width=255 height=60><br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_2\" width=300 height=32><br><br>Sorry!<br><br>You do not have the necessary items.<br><br><img src=\"L2UI_CH3.onscrmsg_pattern01_1\" width=300 height=32><font color=\"222222\"></center></body></html>"
			return htmltext

QUEST       = Quest(QuestId,str(QuestId) + "_" + QuestName,QuestDesc)
CREATED=State('Start',QUEST)
STARTED=State('Started',QUEST)
COMPLETED=State('Completed',QUEST)

QUEST.setInitialState(CREATED)

for npcId in NPC:
 QUEST.addStartNpc(npcId)
 QUEST.addTalkId(npcId)