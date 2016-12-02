package sqdance.g5;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import sqdance.sim.Point;

public class LoveBirdStrategy implements ShapeStrategy {

	public int d;

	// map dancer id to bar
	public Map<Integer, Integer> idToBar = new HashMap<>();;

	// store population of each group
	public Map<Integer, Integer> groupToNum;

	// store bars
	public List<Bar> bars;
	
	// store dance period
	int count;
	int noNewSoulMates;
	// store known soulmates
	public List<Pair> honeyMooners;


	// Define intervals
	public static final int MOVE_INTERVAL = 5 + 1;
	public static int SWAP_INTERVAL;

	
	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.0001;
	public static final double VERTICAL_GAP = 0.5 + 0.001;
	public static final double BAR_GAP = 0.5 + 0.01;


	//Singleton: Ensure there is only one reference
	private static LoveBirdStrategy instance=null;
	
	private LoveBirdStrategy() {
		// System.out.println("Bar strategy is chosen");
		this.d = Player.d;
		this.groupToNum = new HashMap<>();
		this.idToBar = new HashMap<>();
		this.bars = new ArrayList<>();

		this.honeyMooners = new ArrayList<>();
		count = 1;
		noNewSoulMates = 0;
	}

	public static LoveBirdStrategy getInstance(){
		if(instance==null)
			instance=new LoveBirdStrategy();
		return instance;
	}

	@Override
	public Point[] generateStartLocations(int number) {
		// decide the center of each bar
		double firstX = 0.25 + 0.001;
		double firstY = 10.0;

		// decide how many bars are needed
		int barNum = (int) Math.ceil((number + 0.0) / Player.BAR_MAX_VOLUME);

		// calculate the people assigned to each bar
		int headCount = (int) Math.round((number + 0.0) / barNum);

		// rewrite population distribution
		groupToNum = distributePeopleToBars(number);
		int groupPop = ToolBox.findSmallest(groupToNum);

		if (groupPop == 0) {
			// System.out.println("Error: 0 people in the group!");
		} else {
			/*
			 * Decide the interval of swapping
			 */
			// SWAP_INTERVAL = groupPop * MOVE_INTERVAL - 1;
			// SWAP_INTERVAL = 20;
			SWAP_INTERVAL = 1801;
			/* Disabled swapping for now */

			// System.out.println("The small group decides when to swap: " +
			// SWAP_INTERVAL);
		}

		// Put people to group
		int pid = 0;
		int contained = 0;
		for (int i = 0; i < barNum; i++) {
			// System.out.println("Index " + i);
			// Find center point
			Point centerPoint = new Point(firstX + i * (BAR_GAP + HORIZONTAL_GAP), firstY);
			// System.out.format("Center point is: (%f, %f)", centerPoint.x,
			// centerPoint.y);

			// decide head count
			int pop = groupToNum.get(i);

			// System.out.format("The bar is to have %d people\n", pop);

			Bar newBar = new Bar(pop, centerPoint, i);
			this.bars.add(newBar);

			// Set bar flags
			newBar.setBottomConnected(true);
			newBar.setUpConnected(true);

			if (i == 0)
				newBar.setBottomConnected(false);

			if (i == barNum - 1) {
				if (i % 2 == 0) {
					newBar.setUpConnected(false);
				} else {
					newBar.setBottomConnected(false);
				}
			}

			if (i % 2 == 0)
				newBar.setEven(true);
			else
				newBar.setEven(false);

			// store the mapping
			int idEnd = contained + pop;
			for (int j = contained; j < idEnd; j++) {
				idToBar.put(pid++, i);
			}
			contained = idEnd;
		}

		// debug
		for (int i = 0; i < barNum; i++) {
			Bar b = bars.get(i);
			b.debrief();
		}

		// generate return values
		List<Point> result = new LinkedList<>();
		for (int i = 0; i < barNum; i++) {
			Bar theBar = bars.get(i);
			List<Point> thePoints = theBar.getPoints();
			result.addAll(thePoints);
		}

		return result.toArray(new Point[this.d]);
	}
	int turn = 0;
	@Override
	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		System.out.println("Turn: "+turn++);
		Point[] result = new Point[dancers.length];

		if (count%MOVE_INTERVAL==0) {
			Map <Integer, Point> soulmateMoves = getSoulmateMoves(dancers, partner_ids, enjoyment_gained);
			// System.out.println("Soulmate move counts: " + soulmateMoves.size());
			for (int i =0; i < result.length; i++){
				if (soulmateMoves.containsKey(i))
					result[i] = soulmateMoves.get(i);
				else
					result[i] = new Point(0,0);
			}
			for (int j = 0; j < result.length; j++) {
				System.out.println(j + ": " + result[j].x + "," + result[j].y);
			}
		} 
		else {
			result = new Point[dancers.length];
			for (int i = 0; i < dancers.length; i++) {
				result[i] = new Point(0, 0);
			}
		}
		count++;

		return result;
	}

	public Map<Integer, Integer> distributePeopleToBars(int total) {
		// System.out.format("Distributing %d people into bars\n", total);

		Map<Integer, Integer> popMap = new HashMap<>();

		// Put baseline population into bars
		int barNum = (int) Math.ceil((total + 0.0) / Player.BAR_MAX_VOLUME);
		int basePop = total / barNum;
		if (basePop % 2 == 1) {
			// System.out.format("Cannot put %d people in the bar, put %d instead\n", basePop, basePop - 1);
			basePop--;
		}

		for (int i = 0; i < barNum; i++) {
			popMap.put(i, basePop);
		}

		// Deal with the residue
		int residue = total - basePop * barNum;
		// System.out.format("%d people left to distribute to %d bars\n", residue, barNum);
		if (residue % 2 == 1) {
			// System.out.println("Error: Odd number people are left!");
		}
		int pairNum = residue / 2;

		// 1st cycle, distribute people to even bars
		for (int i = 0; i < barNum && pairNum > 0; i += 2) {
			int popNow = popMap.get(i);
			int targetPop = popNow + 2;
			if (targetPop > 80)
				// System.out.format("Error: %d people in bar %d\n", targetPop, i);
			popMap.put(i, popNow + 2);
			pairNum--;
		}

		// 2nd cycle, distribute people to odd bars
		for (int i = 1; i < barNum && pairNum > 0; i += 2) {
			int popNow = popMap.get(i);
			int targetPop = popNow + 2;
			if (targetPop > 80)
				// System.out.format("Error: %d people in bar %d\n", targetPop, i);
			popMap.put(i, popNow + 2);
			pairNum--;
		}

		// System.out.println("Distributed people:" + popMap);

		return popMap;
	}


	// gets the soulmate moves
	private Map<Integer, Point> getSoulmateMoves(Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {

		Map<Integer, Point> allMoves = new HashMap<Integer, Point>();

		//this block enumerates all the pairs in order and saves to a list the integer location for and soulmate list those soulmate rows
		List<Pair> orderedDancers = new ArrayList<Pair>();
		List<Integer> soulmatePairs = new ArrayList<Integer>();
		for (int i=0; i< dancers.length; i++){
			Pair couple = new Pair(dancers[i], dancers[partner_ids[i]], 0, i, partner_ids[i]);
			if (partner_ids[i] == i){
				couple = findMatch(couple, dancers);
				System.out.println("find match for "+couple.leftid+"and "+couple.rightid);
			}
			if (!orderedDancers.contains(couple))
				orderedDancers.add(couple);
		}

		Collections.sort(orderedDancers, new Comparator<Pair>() {
			public int compare(Pair a, Pair b) {
				if (idToBar.get(a.leftid) < idToBar.get(b.leftid))
					return -1;
				else if (idToBar.get(a.leftid) > idToBar.get(b.leftid))
					return 1;
				else if (ToolBox.comparePoints(a.leftdancer, b.leftdancer))
					return 0;
				else if (idToBar.get(a.leftid) % 2 == 0)
					return -1*Double.compare(a.leftdancer.y, b.leftdancer.y);
				else
					return Double.compare(a.leftdancer.y, b.leftdancer.y);
			}
		});
		Map<Pair,Integer> directions = new HashMap<>();
		for (int i =0; i<orderedDancers.size(); i++){
			int direction = 1;
			if (i<orderedDancers.size()/2)
				direction = -1;
			directions.put(orderedDancers.get(i), direction);
		}

		//updates the honneymoon list
		for (int i =0; i< bars.size(); i++){
			Bar b = bars.get(i);
			List<Integer> newlyweds = b.updateHoneymoonSuite(orderedDancers, enjoyment_gained, idToBar);
			for (int leftID: newlyweds){
				Pair p = new Pair(dancers[leftID], dancers[partner_ids[leftID]], 0, leftID, partner_ids[leftID]);
				if (!honeyMooners.contains(p))
					honeyMooners.add(p);
			}
		}

		// System.out.println("Honeymooner size: "+honeyMooners.size());
		for (Pair p: honeyMooners)
			orderedDancers.remove(p);


		//gets the row of soulmate pairs
		for (int i =0; i < orderedDancers.size(); i++) {
			Pair p = orderedDancers.get(i);
			if (enjoyment_gained[p.leftid] ==6 && !inHoneyMoon(p))
				soulmatePairs.add(i); //which rows are soulmates
		}

		if (soulmatePairs.size() == 0)
			noNewSoulMates++;
		else 
			noNewSoulMates = 0;

		// System.out.println("turns without soulmates = "+noNewSoulMates);
		if (noNewSoulMates >= orderedDancers.size() / 2){
			noNewSoulMates = 0;
			return rearrange(orderedDancers);
		}

		Map<Integer, Point> clumpMoves =solveClumping(orderedDancers, soulmatePairs, directions);
		if (clumpMoves.size() > 0){ //at least 1 clump to address{
			noNewSoulMates = 0;
			return clumpMoves;
		}



		int numOfPairs = dancers.length/2;
		for (int row = 0; row < orderedDancers.size(); row++){
			Pair curr = orderedDancers.get(row);
			Bar theBar = bars.get(idToBar.get(curr.leftid));

			int direction = directions.get(curr);

			if (soulmatePairs.contains(row)){
				allMoves.putAll(theBar.doSoulmateMove(direction, curr, idToBar));
				// System.out.println("Row "+row+" direction is"+direction);
			} else {

				boolean spot1 = false;
				boolean spot2 = false;
				boolean spot3 = false;//fix this
				if (direction == -1){
					if (row < orderedDancers.size() - 1 && directions.get(orderedDancers.get(row+1)) == -1 && soulmatePairs.contains(row+1))
						spot1 = true;
					if (row < orderedDancers.size() - 2 && directions.get(orderedDancers.get(row+2)) == -1 && soulmatePairs.contains(row+2))
						spot2 = true;
					if (row < orderedDancers.size() - 3 && directions.get(orderedDancers.get(row+3)) == -1 && soulmatePairs.contains(row+3))
						spot3 = true;					
				} else{
					if (row > 0 && directions.get(orderedDancers.get(row-1)) == 1 && soulmatePairs.contains(row-1))
						spot1 = true;
					if (row > 1 && directions.get(orderedDancers.get(row-2)) == 1 && soulmatePairs.contains(row-2))
						spot2 = true;
					if (row > 2 && directions.get(orderedDancers.get(row-3)) == 1 && soulmatePairs.contains(row-3))
						spot3 = true;						
				}

				int moves = 1;
				if (spot1 && spot2 || spot2 && spot3 || spot1 && spot3){
					moves = 3;
						//triple move
				} else if (spot1 || spot2){
						//double move
					moves = 2;
				}

				int skipperid = curr.leftid;
				int normalid = curr.rightid;
				if (direction == 1){
					skipperid = curr.rightid;
					normalid = curr.leftid;
				}

				Point skipper = dancers[skipperid];
				Point normal = dancers[normalid];

				Point skipperFinal = new Point (0,0);
				Point normalFinal = new Point (0,0);
				Point temp;

				//move skipper the number of moves
				for (int i=0; i<moves; i++){
					temp = theBar.moveSoul(skipper, skipperid, idToBar);
					skipperFinal = skipperFinal.add(temp);
					skipper = skipper.add(temp);
				}
				//move normal the proper number
				int normalMoves = 1;
				if (row==0&&spot1 || row == orderedDancers.size()-1 && spot1){
					normalMoves = moves;
					if (spot3)
						normalMoves--;
					
				}
				for (int i=0; i<normalMoves; i++){
					temp = theBar.moveSoul(normal, normalid, idToBar);
					normalFinal = normalFinal.add(temp);
					normal = normal.add(temp);
					
				}
				// System.out.println("Moves for row "+row+" with "+curr.leftid+", "+curr.rightid+": ("+skipperFinal.x+","+skipperFinal.y+") and ("+normalFinal.x+","+normalFinal.y+")");

				allMoves.put(skipperid, skipperFinal);
				allMoves.put(normalid, normalFinal);
				

			}//dealing with a non soulmate row else statement
		}//looping through the rows
		return allMoves;

	} //closes getsoulmate moves method



	public Point checkPoint(Point p){
		int multiplier = 1;
		if (p.y < 0)
			multiplier = -1;
		if (p.y*multiplier > 1.4){
			p = new Point(p.x, 1.503*multiplier);
		} else if (p.y*multiplier > .9){
			p = new Point(p.x, 1.002*multiplier);
		} else if (p.y*multiplier > 0) {
			p = new Point(p.x, .501*multiplier);
		}
		return p;
	}

	public boolean inHoneyMoon(Pair p){
		return (honeyMooners.contains(p));
	}

	public Pair findMatch (Pair p, Point[] dancers){
		Bar theBar = bars.get(idToBar.get(p.leftid));
		Point left = p.leftdancer.add(theBar.goLeft(p.leftdancer));
		Point right = p.leftdancer.add(theBar.goRight(p.leftdancer));
		boolean found = false;
		int min = -1;
		for (int i=0; i<dancers.length; i++){
			if (idToBar.get(i)==theBar.id && i!=p.leftid && (ToolBox.comparePoints(left, dancers[i]) || ToolBox.comparePoints(right, dancers[i]))){
				p = new Pair(p.leftdancer, dancers[i], 0, p.leftid, i);
				found = true;
			}
			if (i!=p.leftid && (min==-1 || ToolBox.distance(p.leftdancer, dancers[min]) > ToolBox.distance(p.leftdancer, dancers[i])))
				min = i;
		}
		if (!found){
			p = new Pair(p.leftdancer, dancers[min], 0, p.leftid, min);			
			System.out.println("MISSING MATCHES");

		}
		System.out.println("result: "+p.leftid+", "+p.rightid);
		return p;
	}

	public Map<Integer, Point> rearrange (List<Pair> dancers){
		// System.out.println("calling rearrange, dancers = "+dancers.size());
		Map<Integer, Point> switches = new HashMap<>();
		for (int i=0; i< dancers.size() - 1; i+=2){
			//for every 2 pairs of 2, switch the right 2
			Pair p1 = dancers.get(i);
			Pair p2 = dancers.get(i+1);
			switches.put(p1.rightid, checkPoint(ToolBox.pointsDifferencer(p1.rightdancer, p2.rightdancer)));
			switches.put(p2.rightid, checkPoint(ToolBox.pointsDifferencer(p2.rightdancer, p1.rightdancer)));
			idToBar.put(p1.rightid, idToBar.get(p2.rightid));
			idToBar.put(p2.rightid, idToBar.get(p1.rightid));

		}
		return switches;
	}

	public List<Integer> checkForClumping(List<Pair> dancers, List<Integer> soulmateRows, Map<Pair, Integer> directions){
		List<Integer> clumps = new ArrayList<>();
		for (int i =0; i< dancers.size(); i++){
			int count = 0;
			if (soulmateRows.contains(i) && directions.get(dancers.get(i)) == directions.get(dancers.get(i)))
				count++;
			if (i < dancers.size() - 1 && soulmateRows.contains(i + 1) && directions.get(dancers.get(i)) == directions.get(dancers.get(i + 1)))
				count++;
			if (i < dancers.size() - 2 && soulmateRows.contains(i + 2) && directions.get(dancers.get(i)) == directions.get(dancers.get(i + 2)))
				count++;
			if (i < dancers.size() - 3 && soulmateRows.contains(i + 3) && directions.get(dancers.get(i)) == directions.get(dancers.get(i + 3)))
				count++;
			if (count > 2) 
				clumps.add(i);
		}
		return clumps;
	}

	public Map<Integer, Point> solveClumping(List<Pair> dancers, List<Integer> soulmateRows, Map<Pair, Integer> directions){
		List<Integer> clumps = checkForClumping(dancers, soulmateRows, directions);
		Map<Integer, Integer> switchPairs = new HashMap<>();
		Map<Integer, Point> clumpMoves = new HashMap<>();
		if (clumps.size() > 0){		
			System.out.println("SOLVING CLUMPING!!");
			List<Integer> modified = new ArrayList<>();
			Map<Integer, Integer> freeness = calculateFreeness(dancers, soulmateRows, directions, modified);

		//if in the list of clumps
		//look at all neighbors, pick one with most freeness, modify their neighbors


			List<Integer> souls = new ArrayList<>();
			souls.addAll(clumps);
			for(int i=0; i<soulmateRows.size();i++)
				if (!souls.contains(soulmateRows.get(i)))
					souls.add(soulmateRows.get(i));
			for (int i=0; i<souls.size(); i++){
				List<Integer> neighborIDs= getNeighbors(dancers, souls.get(i));
				int switchid = -1;
				int max = -2;
				for (int j=0; j<neighborIDs.size(); j++){
					int temp = freeness.get(neighborIDs.get(j));
					if (!souls.contains(neighborIDs.get(j)) && !soulmateRows.contains(neighborIDs.get(j)) && !modified.contains(neighborIDs.get(j)) && temp > max){
						switchid = neighborIDs.get(j);
						max = temp;
					}
				}
			//get neighbors
				if(max>=0){
					//switch
					switchPairs.put(souls.get(i), switchid);
					modified.add(switchid);
					//updateall neighbors of switch
					freeness = calculateFreeness(dancers, soulmateRows, directions, modified);
					// System.out.print("Modified");
					// for (int m = 0; i<modified.size(); m++)
					// 	System.out.print(modified.get(m)+" ");

				}
			}

			for (int i: switchPairs.keySet()){
				Pair p1 = dancers.get(i);
				Pair p2 = dancers.get(switchPairs.get(i));
				System.out.println("Switching: "+p1.leftid+","+p1.rightid+" and  "+p2.leftid+","+p2.rightid);
				clumpMoves.put(p1.leftid, checkPoint(ToolBox.pointsDifferencer(p1.leftdancer, p2.leftdancer)));
				clumpMoves.put(p1.rightid, checkPoint(ToolBox.pointsDifferencer(p1.rightdancer, p2.rightdancer)));
				clumpMoves.put(p2.leftid, checkPoint(ToolBox.pointsDifferencer(p2.leftdancer, p1.leftdancer)));
				clumpMoves.put(p2.rightid, checkPoint(ToolBox.pointsDifferencer(p2.rightdancer, p1.rightdancer)));
				
				int id = idToBar.get(p1.leftid);
				idToBar.put(p1.leftid, idToBar.get(p2.leftid));
				idToBar.put(p1.rightid, idToBar.get(p2.rightid));
				idToBar.put(p2.leftid, id);
				idToBar.put(p2.rightid, id);
			}
		}
		return clumpMoves;
	}


	public List<Integer> getNeighbors(List<Pair> dancers, int id){
		// System.out.print("called neighbors");
		List<Integer> neighbors = new ArrayList<>();
		Pair curr = dancers.get(id);
		for (int i=0; i<dancers.size(); i++){
			if (i!=id && ToolBox.distance(curr.leftdancer, dancers.get(i).leftdancer) < 2){
				neighbors.add(i);
				// System.out.println("Distance for"+dancers.get(i).leftid+": "+ToolBox.distance(curr.leftdancer, dancers.get(i).leftdancer));
			}
		}
		// System.out.println("So neighbor size is... "+neighbors.size());
		return neighbors;

	}


	public Map<Integer, Integer> calculateFreeness(List<Pair> dancers, List<Integer> soulmateRows, Map<Pair, Integer> directions, List<Integer> modified){
		Map<Integer, Integer> freeness = new HashMap<>();
		for (int i=0; i<dancers.size(); i++){
			int left = 0;
			int right = 0;
			while (i + left < dancers.size() - 1 && (soulmateRows.contains(i+left+1) || modified.contains(i+left+1)) && directions.get(dancers.get(i+left+1)) == directions.get(dancers.get(i)))
				left++;
			while (i > right && (soulmateRows.contains(i-right-1) || modified.contains(i-right-1)) && directions.get(dancers.get(i-right-1)) == directions.get(dancers.get(i)))
				right++;
			if (left+right == 0){
				while (i + left < dancers.size() - 1 && soulmateRows.contains(i+left+1) && directions.get(dancers.get(i+left+1)) == directions.get(dancers.get(i)))
					left++;
				while (i > right && soulmateRows.contains(i-right-1) && directions.get(dancers.get(i-right-1)) == directions.get(dancers.get(i)))
					right++;
				left = -1*left;
				right = -1*right;
			}
			if (i == 0 && directions.get(dancers.get(i)) == -1 || i == dancers.size()-1 && directions.get(dancers.get(i)) == 1 ){
				left = -100;
				right = -100;
			}
			freeness.put(i, (left+right)*-1);
		}
		// System.out.println("did not break on freedom");
		return freeness;
	}

}
