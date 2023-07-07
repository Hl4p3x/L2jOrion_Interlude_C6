/*
 * L2jOrion Project - www.l2jorion.com 
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package l2jorion.game.model.actor.instance;

import java.util.Calendar;
import java.util.List;

import javolution.text.TextBuilder;
import l2jorion.Config;
import l2jorion.game.model.L2Party;
import l2jorion.game.model.entity.sevensigns.SevenSigns;
import l2jorion.game.model.entity.sevensigns.SevenSignsFestival;
import l2jorion.game.network.SystemMessageId;
import l2jorion.game.network.serverpackets.ActionFailed;
import l2jorion.game.network.serverpackets.NpcHtmlMessage;
import l2jorion.game.network.serverpackets.SystemMessage;
import l2jorion.game.templates.L2NpcTemplate;
import l2jorion.game.templates.StatsSet;

public final class L2FestivalGuideInstance extends L2FolkInstance
{
	/** The _festival type. */
	protected int _festivalType;
	
	/** The _festival oracle. */
	protected int _festivalOracle;
	
	/** The _blue stones needed. */
	protected int _blueStonesNeeded;
	
	/** The _green stones needed. */
	protected int _greenStonesNeeded;
	
	/** The _red stones needed. */
	protected int _redStonesNeeded;
	
	/**
	 * Instantiates a new l2 festival guide instance.
	 * @param objectId the object id
	 * @param template the template
	 */
	public L2FestivalGuideInstance(final int objectId, final L2NpcTemplate template)
	{
		super(objectId, template);
		
		switch (getNpcId())
		{
			case 31127:
			case 31132:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_31;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 900;
				_greenStonesNeeded = 540;
				_redStonesNeeded = 270;
				break;
			case 31128:
			case 31133:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_42;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 1500;
				_greenStonesNeeded = 900;
				_redStonesNeeded = 450;
				break;
			case 31129:
			case 31134:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_53;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 3000;
				_greenStonesNeeded = 1800;
				_redStonesNeeded = 900;
				break;
			case 31130:
			case 31135:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_64;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 4500;
				_greenStonesNeeded = 2700;
				_redStonesNeeded = 1350;
				break;
			case 31131:
			case 31136:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_NONE;
				_festivalOracle = SevenSigns.CABAL_DAWN;
				_blueStonesNeeded = 6000;
				_greenStonesNeeded = 3600;
				_redStonesNeeded = 1800;
				break;
			
			case 31137:
			case 31142:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_31;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 900;
				_greenStonesNeeded = 540;
				_redStonesNeeded = 270;
				break;
			case 31138:
			case 31143:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_42;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 1500;
				_greenStonesNeeded = 900;
				_redStonesNeeded = 450;
				break;
			case 31139:
			case 31144:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_53;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 3000;
				_greenStonesNeeded = 1800;
				_redStonesNeeded = 900;
				break;
			case 31140:
			case 31145:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_64;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 4500;
				_greenStonesNeeded = 2700;
				_redStonesNeeded = 1350;
				break;
			case 31141:
			case 31146:
				_festivalType = SevenSignsFestival.FESTIVAL_LEVEL_MAX_FOR_NONE;
				_festivalOracle = SevenSigns.CABAL_DUSK;
				_blueStonesNeeded = 6000;
				_greenStonesNeeded = 3600;
				_redStonesNeeded = 1800;
				break;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see l2jorion.game.model.actor.instance.L2FolkInstance#onBypassFeedback(l2jorion.game.model.actor.instance.L2PcInstance, java.lang.String)
	 */
	@Override
	public void onBypassFeedback(final L2PcInstance player, final String command)
	{
		if (command.startsWith("FestivalDesc"))
		{
			final int val = Integer.parseInt(command.substring(13));
			
			showChatWindow(player, val, null, true);
		}
		else if (command.startsWith("Festival"))
		{
			L2Party playerParty = player.getParty();
			final int val = Integer.parseInt(command.substring(9, 10));
			
			switch (val)
			{
				case 1: // Become a Participant
					// Check if the festival period is active, if not then don't allow registration.
					if (SevenSigns.getInstance().isSealValidationPeriod())
					{
						showChatWindow(player, 2, "a", false);
						return;
					}
					
					// Check if a festival is in progress, then don't allow registration yet.
					if (SevenSignsFestival.getInstance().isFestivalInitialized())
					{
						player.sendMessage("You cannot sign up while a festival is in progress.");
						return;
					}
					
					// Check if the player is in a formed party already.
					if (playerParty == null)
					{
						showChatWindow(player, 2, "b", false);
						return;
					}
					
					// Check if the player is the party leader.
					if (!playerParty.isLeader(player))
					{
						showChatWindow(player, 2, "c", false);
						return;
					}
					
					// Check to see if the party has at least 5 members.
					if (playerParty.getMemberCount() < Config.ALT_FESTIVAL_MIN_PLAYER)
					{
						showChatWindow(player, 2, "b", false);
						return;
					}
					
					// Check if all the party members are in the required level range.
					if (playerParty.getLevel() > SevenSignsFestival.getMaxLevelForFestival(_festivalType))
					{
						showChatWindow(player, 2, "d", false);
						return;
					}
					
					/*
					 * Check to see if the player has already signed up, if they are then update the participant list providing all the required criteria has been met.
					 */
					if (player.isFestivalParticipant())
					{
						SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
						showChatWindow(player, 2, "f", false);
						return;
					}
					
					showChatWindow(player, 1, null, false);
					break;
				case 2: // Festival 2 xxxx
					final int stoneType = Integer.parseInt(command.substring(11));
					int stonesNeeded = 0;
					
					switch (stoneType)
					{
						case SevenSigns.SEAL_STONE_BLUE_ID:
							stonesNeeded = _blueStonesNeeded;
							break;
						case SevenSigns.SEAL_STONE_GREEN_ID:
							stonesNeeded = _greenStonesNeeded;
							break;
						case SevenSigns.SEAL_STONE_RED_ID:
							stonesNeeded = _redStonesNeeded;
							break;
					}
					
					if (!player.destroyItemByItemId("SevenSigns", stoneType, stonesNeeded, this, true))
					{
						return;
					}
					
					SevenSignsFestival.getInstance().setParticipants(_festivalOracle, _festivalType, playerParty);
					SevenSignsFestival.getInstance().addAccumulatedBonus(_festivalType, stoneType, stonesNeeded);
					
					showChatWindow(player, 2, "e", false);
					break;
				case 3: // Score Registration
					// Check if the festival period is active, if not then don't register the score.
					if (SevenSigns.getInstance().isSealValidationPeriod())
					{
						showChatWindow(player, 3, "a", false);
						return;
					}
					
					// Check if a festival is in progress, if it is don't register the score.
					if (SevenSignsFestival.getInstance().isFestivalInProgress())
					{
						player.sendMessage("You cannot register a score while a festival is in progress.");
						return;
					}
					
					// Check if the player is in a party.
					if (playerParty == null)
					{
						showChatWindow(player, 3, "b", false);
						return;
					}
					
					List<L2PcInstance> prevParticipants = SevenSignsFestival.getInstance().getPreviousParticipants(_festivalOracle, _festivalType);
					
					// Check if there are any past participants.
					if (prevParticipants == null)
					{
						return;
					}
					
					// Check if this player was among the past set of participants for this festival.
					if (!prevParticipants.contains(player))
					{
						showChatWindow(player, 3, "b", false);
						return;
					}
					
					// Check if this player was the party leader in the festival.
					if (player.getObjectId() != prevParticipants.get(0).getObjectId())
					{
						showChatWindow(player, 3, "b", false);
						return;
					}
					
					L2ItemInstance bloodOfferings = player.getInventory().getItemByItemId(SevenSignsFestival.FESTIVAL_OFFERING_ID);
					int offeringCount = 0;
					
					// Check if the player collected any blood offerings during the festival.
					if (bloodOfferings == null)
					{
						player.sendMessage("You do not have any blood offerings to contribute.");
						return;
					}
					
					offeringCount = bloodOfferings.getCount();
					
					final int offeringScore = offeringCount * SevenSignsFestival.FESTIVAL_OFFERING_VALUE;
					final boolean isHighestScore = SevenSignsFestival.getInstance().setFinalScore(player, _festivalOracle, _festivalType, offeringScore);
					
					player.destroyItem("SevenSigns", bloodOfferings, this, false);
					
					// Send message that the contribution score has increased.
					SystemMessage sm = new SystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED);
					sm.addNumber(offeringScore);
					player.sendPacket(sm);
					sm = null;
					
					if (isHighestScore)
					{
						showChatWindow(player, 3, "c", false);
					}
					else
					{
						showChatWindow(player, 3, "d", false);
					}
					
					prevParticipants = null;
					bloodOfferings = null;
					break;
				case 4: // Current High Scores
					TextBuilder strBuffer = new TextBuilder("<html><body>Festival Guide:<br>These are the top scores of the week, for the ");
					
					final StatsSet dawnData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, _festivalType);
					final StatsSet duskData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, _festivalType);
					final StatsSet overallData = SevenSignsFestival.getInstance().getOverallHighestScoreData(_festivalType);
					
					final int dawnScore = dawnData.getInteger("score");
					final int duskScore = duskData.getInteger("score");
					int overallScore = 0;
					
					// If no data is returned, assume there is no record, or all scores are 0.
					if (overallData != null)
					{
						overallScore = overallData.getInteger("score");
					}
					
					strBuffer.append(SevenSignsFestival.getFestivalName(_festivalType) + " festival.<br>");
					
					if (dawnScore > 0)
					{
						strBuffer.append("Dawn: " + calculateDate(dawnData.getString("date")) + ". Score " + dawnScore + "<br>" + dawnData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Dawn: No record exists. Score 0<br>");
					}
					
					if (duskScore > 0)
					{
						strBuffer.append("Dusk: " + calculateDate(duskData.getString("date")) + ". Score " + duskScore + "<br>" + duskData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Dusk: No record exists. Score 0<br>");
					}
					
					if ((overallScore > 0) && (overallData != null))
					{
						String cabalStr = "Children of Dusk";
						
						if (overallData.getString("cabal").equals("dawn"))
						{
							cabalStr = "Children of Dawn";
						}
						
						strBuffer.append("Consecutive top scores: " + calculateDate(overallData.getString("date")) + ". Score " + overallScore + "<br>Affilated side: " + cabalStr + "<br>" + overallData.getString("members") + "<br>");
					}
					else
					{
						strBuffer.append("Consecutive top scores: No record exists. Score 0<br>");
					}
					
					strBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");
					
					NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setHtml(strBuffer.toString());
					player.sendPacket(html);
					strBuffer = null;
					html = null;
					break;
				case 8: // Increase the Festival Challenge
					if (playerParty == null)
					{
						return;
					}
					
					if (!SevenSignsFestival.getInstance().isFestivalInProgress())
					{
						return;
					}
					
					if (!playerParty.isLeader(player))
					{
						showChatWindow(player, 8, "a", false);
						break;
					}
					
					if (SevenSignsFestival.getInstance().increaseChallenge(_festivalOracle, _festivalType))
					{
						showChatWindow(player, 8, "b", false);
					}
					else
					{
						showChatWindow(player, 8, "c", false);
					}
					break;
				case 9: // Leave the Festival
					if (playerParty == null)
					{
						return;
					}
					
					/**
					 * If the player is the party leader, remove all participants from the festival (i.e. set the party to null, when updating the participant list) otherwise just remove this player from the "arena", and also remove them from the party.
					 */
					final boolean isLeader = playerParty.isLeader(player);
					
					if (isLeader)
					{
						SevenSignsFestival.getInstance().updateParticipants(player, null);
					}
					else
					{
						SevenSignsFestival.getInstance().updateParticipants(player, playerParty);
						playerParty.removePartyMember(player);
					}
					break;
				case 0: // Distribute Accumulated Bonus
					if (!SevenSigns.getInstance().isSealValidationPeriod())
					{
						player.sendMessage("Bonuses cannot be paid during the competition period.");
						return;
					}
					
					if (SevenSignsFestival.getInstance().distribAccumulatedBonus(player) > 0)
					{
						showChatWindow(player, 0, "a", false);
					}
					else
					{
						showChatWindow(player, 0, "b", false);
					}
					break;
				default:
					showChatWindow(player, val, null, false);
			}
			playerParty = null;
		}
		else
		{
			// this class dont know any other commands, let forward
			// the command to the parent class
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Show chat window.
	 * @param player the player
	 * @param val the val
	 * @param suffix the suffix
	 * @param isDescription the is description
	 */
	private void showChatWindow(final L2PcInstance player, final int val, final String suffix, final boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH + "festival/";
		filename += isDescription ? "desc_" : "festival_";
		filename += suffix != null ? val + suffix + ".htm" : val + ".htm";
		
		// Send a Server->Client NpcHtmlMessage containing the text of the L2NpcInstance to the L2PcInstance
		NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		html.setFile(filename);
		html.replace("%objectId%", String.valueOf(getObjectId()));
		html.replace("%festivalType%", SevenSignsFestival.getFestivalName(_festivalType));
		html.replace("%cycleMins%", String.valueOf(SevenSignsFestival.getInstance().getMinsToNextCycle()));
		if (!isDescription && "2b".equals(val + suffix))
		{
			html.replace("%minFestivalPartyMembers%", String.valueOf(Config.ALT_FESTIVAL_MIN_PLAYER));
		}
		
		// If the stats or bonus table is required, construct them.
		if (val == 5)
		{
			html.replace("%statsTable%", getStatsTable());
		}
		if (val == 6)
		{
			html.replace("%bonusTable%", getBonusTable());
		}
		
		// festival's fee
		if (val == 1)
		{
			html.replace("%blueStoneNeeded%", String.valueOf(_blueStonesNeeded));
			html.replace("%greenStoneNeeded%", String.valueOf(_greenStonesNeeded));
			html.replace("%redStoneNeeded%", String.valueOf(_redStonesNeeded));
		}
		
		player.sendPacket(html);
		filename = null;
		html = null;
		
		// Send a Server->Client ActionFailed to the L2PcInstance in order to avoid that the client wait another packet
		player.sendPacket(ActionFailed.STATIC_PACKET);
	}
	
	/**
	 * Gets the stats table.
	 * @return the stats table
	 */
	private final String getStatsTable()
	{
		final TextBuilder tableHtml = new TextBuilder();
		
		// Get the scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			final int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
			final int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);
			String winningCabal = "Children of Dusk";
			
			if (dawnScore > duskScore)
			{
				winningCabal = "Children of Dawn";
			}
			else if (dawnScore == duskScore)
			{
				winningCabal = "None";
			}
			
			tableHtml.append("<tr><td width=\"100\" align=\"center\">" + festivalName + "</td><td align=\"center\" width=\"35\">" + duskScore + "</td><td align=\"center\" width=\"35\">" + dawnScore + "</td><td align=\"center\" width=\"130\">" + winningCabal + "</td></tr>");
		}
		
		return tableHtml.toString();
	}
	
	/**
	 * Gets the bonus table.
	 * @return the bonus table
	 */
	private final String getBonusTable()
	{
		final TextBuilder tableHtml = new TextBuilder();
		
		// Get the accumulated scores for each of the festival level ranges (types).
		for (int i = 0; i < 5; i++)
		{
			final int accumScore = SevenSignsFestival.getInstance().getAccumulatedBonus(i);
			final String festivalName = SevenSignsFestival.getFestivalName(i);
			
			tableHtml.append("<tr><td align=\"center\" width=\"150\">" + festivalName + "</td><td align=\"center\" width=\"150\">" + accumScore + "</td></tr>");
		}
		
		return tableHtml.toString();
	}
	
	/**
	 * Calculate date.
	 * @param milliFromEpoch the milli from epoch
	 * @return the string
	 */
	private final String calculateDate(final String milliFromEpoch)
	{
		final long numMillis = Long.valueOf(milliFromEpoch);
		final Calendar calCalc = Calendar.getInstance();
		
		calCalc.setTimeInMillis(numMillis);
		
		return calCalc.get(Calendar.YEAR) + "/" + calCalc.get(Calendar.MONTH) + "/" + calCalc.get(Calendar.DAY_OF_MONTH);
	}
}
