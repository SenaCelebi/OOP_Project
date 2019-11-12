/**
 *  Strategy Engine for Programming Intelligent Agents (SEPIA)
    Copyright (C) 2012 Case Western Reserve University

    This file is part of SEPIA.

    SEPIA is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    SEPIA is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with SEPIA.  If not, see <http://www.gnu.org/licenses/>.
 */
//package edu.cwru.sepia.agent;


import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.experiment.Configuration;
import edu.cwru.sepia.experiment.ConfigurationValues;
import edu.cwru.sepia.agent.Agent;

/**
 * This agent will first collect gold to produce a peasant,
 * then the two peasants will collect gold and wood separately until reach goal.
 * @author Feng
 *
 */
public class ResourceCollectionAgent extends Agent {
	private static final long serialVersionUID = -4047208702628325380L;
	private static final Logger logger = Logger.getLogger(ResourceCollectionAgent.class.getCanonicalName());

	private int goldRequire;
	
	private int woodRequire;
	
	private int step;
	
	public ResourceCollectionAgent(int playernum, String[] arguments) {
		super(playernum);
		
		goldRequire = Integer.parseInt(arguments[0]);
		woodRequire = Integer.parseInt(arguments[1]);
	}

	StateView currentState;
	
	@Override
	public Map<Integer, Action> initialStep(StateView newstate, History.HistoryView statehistory) {
		step = 0;
		return middleStep(newstate, statehistory);
	}

	@Override
	public Map<Integer,Action> middleStep(StateView newState, History.HistoryView statehistory) {
		step++;
		
		Map<Integer,Action> builder = new HashMap<Integer,Action>();
		currentState = newState;
		
		int currentGold = currentState.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = currentState.getResourceAmount(0, ResourceType.WOOD);
		
		
		List<Integer> myUnit_IDs = currentState.getUnitIds(playernum);
		List<Integer> peasant_Ids = new ArrayList<Integer>();
		List<Integer> townhall_Ids = new ArrayList<Integer>();
		List<Integer> farm_Ids = new ArrayList<Integer>();
		List<Integer> barracks_Ids = new ArrayList<Integer>();
		List<Integer> footman_Ids = new ArrayList<Integer>();
	
		for(int i=0; i<myUnit_IDs.size(); i++) {
			
			int id = myUnit_IDs.get(i);
			UnitView unit = currentState.getUnit(id);
			String unitType = unit.getTemplateView().getName();
			
			if(unitType.equals("TownHall"))
				townhall_Ids.add(id);
			
			if(unitType.equals("Peasant"))
				peasant_Ids.add(id);
			
			if(unitType.equals("Farm"))
				farm_Ids.add(id);
			
			if(unitType.equals("Barracks"))
				barracks_Ids.add(id);
			
			if(unitType.equals("Footman"))
				footman_Ids.add(id);
		}
		
		List<Integer> enemyUnitIds = currentState.getAllUnitIds();
		enemyUnitIds.removeAll(myUnit_IDs);
		
		if(peasant_Ids.size() >= 3) {  // Collect Resources
			
			if(farm_Ids.size() < 1 && currentGold >= 500 && currentWood >= 250) {
				
				System.out.println("Building a FARM");
				int peasant_Id = peasant_Ids.get(0);
				Action action = Action.createPrimitiveBuild(peasant_Id, currentState.getTemplate(playernum, "Farm").getID());
				
				builder.put(peasant_Id, action);
			}
			else if(barracks_Ids.size()<1 && currentGold >=700 && currentWood>=400) {
				
				System.out.println("Building a BARRACKS");
				int peasant_Id = peasant_Ids.get(0);
				Action action = Action.createPrimitiveBuild(peasant_Id, currentState.getTemplate(playernum, "Barracks").getID());
				
				builder.put(peasant_Id, action);
			}
			
			else if(barracks_Ids.size()>0 && footman_Ids.size()<2 && currentGold >=600 ) {
				
				System.out.println("Building a FOOTMAN");
				int barracks_Id = barracks_Ids.get(0);
				Action action = Action.createCompoundProduction(barracks_Id, currentState.getTemplate(playernum, "Footman").getID());
				
				builder.put(barracks_Id, action);
			}
			
			else {
				
				if(footman_Ids.size()>=2) { //Attack Enemies
					
					System.out.println("Attacking enemies");
					
					for(int i : footman_Ids) {
						
						Action action = Action.createCompoundAttack(i, enemyUnitIds.get(0));
						builder.put(i, action);
					}
				}
				
				int peasant_Id = peasant_Ids.get(1);
				int townhall_Id = townhall_Ids.get(0);
				
				Action action = null;
				
				if(currentState.getUnit(peasant_Id).getCargoAmount()>0) {
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDDEPOSIT, townhall_Id);
				}
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.TREE);
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				
				builder.put(peasant_Id, action);
				
				peasant_Id = peasant_Ids.get(0);
				
				if(currentState.getUnit(peasant_Id).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasant_Id).getCargoAmount()>2) {
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDDEPOSIT, townhall_Id);
				}
				else {
					List<Integer> resource_Ids = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDGATHER, resource_Ids.get(0));
				
				}
				
				builder.put(peasant_Id, action);
				
				peasant_Id = peasant_Ids.get(2);
				
				if(currentState.getUnit(peasant_Id).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasant_Id).getCargoAmount()>0) {
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDDEPOSIT, townhall_Id);
				}
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(peasant_Id, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				
				}
				
				builder.put(peasant_Id, action);
			}
     }
			
		
		else {  // Build Peasant
			
			if(currentGold>=400) {
				
				System.out.println("Building peasant");
				TemplateView peasanttemplate = currentState.getTemplate(playernum, "Peasant");
				
				int peasanttemplate_ID = peasanttemplate.getID();
				int townhall_ID = townhall_Ids.get(0);
				
				builder.put(townhall_ID, Action.createCompoundProduction(townhall_ID, peasanttemplate_ID));
			} 
			else {
				
				//System.out.println("Collecting Gold");
				
				int peasant_ID = peasant_Ids.get(0);
				int townhall_Id = townhall_Ids.get(0);
				
				Action action = null;
				
				if(currentState.getUnit(peasant_ID).getCargoType() == ResourceType.GOLD && currentState.getUnit(peasant_ID).getCargoAmount()>0)
					action = new TargetedAction(peasant_ID, ActionType.COMPOUNDDEPOSIT, townhall_Id);
				
				else {
					List<Integer> resourceIds = currentState.getResourceNodeIds(Type.GOLD_MINE);
					action = new TargetedAction(peasant_ID, ActionType.COMPOUNDGATHER, resourceIds.get(0));
				}
				
				builder.put(peasant_ID, action);
			}
		}
		return builder;
	}

	@Override
	public void terminalStep(StateView newstate, History.HistoryView statehistory) {
		
		step++;
		
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("=> Step: " + step);
		}
		
		int currentGold = newstate.getResourceAmount(0, ResourceType.GOLD);
		int currentWood = newstate.getResourceAmount(0, ResourceType.WOOD);
		
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Gold: " + currentGold);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Current Wood: " + currentWood);
		}
		if(logger.isLoggable(Level.FINE))
		{
			logger.fine("Congratulations! You have finished the task!");
		}
	}
	
	public static String getUsage() {
		return "Two arguments, amount of gold to gather and amount of wood to gather";
	}
	
	@Override
	public void savePlayerData(OutputStream os) {
		//this agent lacks learning and so has nothing to persist.
		
	}
	
	@Override
	public void loadPlayerData(InputStream is) {
		//this agent lacks learning and so has nothing to persist.
	}
}