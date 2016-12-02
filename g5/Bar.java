package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class Bar extends Shape {
	/* Bar specific records */
	

	// Anchor points
	public Point center;
	public Point topLeft;
	public Point topRight;
	public Point bottomRight;
	public Point bottomLeft;

	// Interactions
	boolean upConnected;

	public boolean isUpConnected() {
		return upConnected;
	}

	public void setUpConnected(boolean upConnected) {
		this.upConnected = upConnected;
	}

	public boolean isBottomConnected() {
		return bottomConnected;
	}

	public void setBottomConnected(boolean bottomConnected) {
		this.bottomConnected = bottomConnected;
	}

	public boolean isEven() {
		return isEven;
	}

	public void setEven(boolean isEven) {
		this.isEven = isEven;
	}

	boolean bottomConnected;
	boolean isEven;

	public void debrief() {
// 		System.out.println("Bar id: " + id);
// //
// 		// System.out
// 		// .println("Upconnected: " + upConnected + " Bottomconnected: " +
// 		// bottomConnected + " IsEven: " + isEven);
// 		// System.out.println("Topleft: " + topLeft);
// 		// System.out.println("TopRight: " + topRight);
// 		// System.out.println("BottomLeft: " + bottomLeft);
// 		// System.out.println("BottomRight: " + bottomRight);

// 		/* Detailed maps */
// 		System.out.println(dancerId);
// 		System.out.println(idToPosition);
// 		System.out.println(idToRow);

	}

	int number;

	// List<Point> spots = new ArrayList<>();
	List<Point> leftColumn = new ArrayList<>();
	List<Point> rightColumn = new ArrayList<>();

	public Bar(int number, Point center, int id) {
		// store params
		this.number = number;
		this.center = center;
		this.id = id;

		// calculate starting points of the two rows
		int column = number / 2;
		int halfRow;
		Point startLeft;
		Point startRight;
		if (column % 2 == 0) {
			// System.out.println("Even number in a column");
			halfRow = column / 2;
			startLeft = new Point(center.x - 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP + 0.5 * OneMoreTimeStrategy.VERTICAL_GAP);
			startRight = new Point(center.x + 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP + 0.5 * OneMoreTimeStrategy.VERTICAL_GAP);

		} else {
			// System.out.println("Odd number in a column");
			halfRow = (column - 1) / 2;
			startLeft = new Point(center.x - 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
			startRight = new Point(center.x + 0.5 * OneMoreTimeStrategy.HORIZONTAL_GAP,
					center.y - halfRow * OneMoreTimeStrategy.VERTICAL_GAP);
		}
		// System.out.println("Starting points:");
		// System.out.format("Left start: (%f, %f)", startLeft.x, startLeft.y);
		// System.out.format("Right start: (%f, %f)", startRight.x, startRight.y);

		// store topleft and bottomright
		topLeft = startLeft;
		topRight = startRight;
		bottomLeft = new Point(startLeft.x, startLeft.y + (column - 1) * OneMoreTimeStrategy.VERTICAL_GAP);
		bottomRight = new Point(startRight.x, startRight.y + (column - 1) * OneMoreTimeStrategy.VERTICAL_GAP);

		// System.out.format("Top left: (%f, %f)", topLeft.x, topLeft.y);
		// System.out.format("Bottom right: (%f, %f)", bottomRight.x, bottomRight.y);

		// assign people to points
		for (int i = 0; i < column; i++) {
			Point leftPlayer = new Point(startLeft.x, startLeft.y + i * OneMoreTimeStrategy.VERTICAL_GAP);
			Point rightPlayer = new Point(startLeft.x + OneMoreTimeStrategy.HORIZONTAL_GAP,
					startLeft.y + i * OneMoreTimeStrategy.VERTICAL_GAP);
			// spots.add(leftPlayer);
			// spots.add(rightPlayer);
			leftColumn.add(leftPlayer);
			rightColumn.add(rightPlayer);
		}
		// System.out.println("Now " + (leftColumn.size() + rightColumn.size()) + " players are assigned.");

	}

	public List<Point> getPoints() {
		List<Point> results = new LinkedList<>();

		for (int i = 0; i < leftColumn.size(); i++) {
			results.add(leftColumn.get(i));
			results.add(rightColumn.get(i));
		}

		return results;
	}

	/*
	 * Check if a point is in the left or right -1: Not in this bar 0: left 1:
	 * right
	 */
	public int column(Point p) {
		double diffX = Math.abs(p.x - center.x);
		if (2 * diffX > OneMoreTimeStrategy.HORIZONTAL_GAP + 0.3)
			return -1;
		if (p.x < center.x)
			return 0;
		else
			return 1;
	}

	/* Point moving */
	public Point goUp(Point me) {
		Point newLoc = new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
		return newLoc;
	}

	public Point goDown(Point me) {
		Point newLoc = new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
		return newLoc;
	}

	public Point goRight(Point me) {
		Point newLoc = new Point(OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
		return newLoc;
	}

	public Point goLeft(Point me) {
		Point newLoc = new Point(-OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
		return newLoc;
	}

	public Point goLeftToNextBar(Point me) {

		// Point newLoc = new Point(0 - Player.HORIZONTAL_GAP - Player.BAR_GAP,
		// 0);

		// find previous bar
		// Bar prev = LoveBirdStrategy.getInstance().bars.get(this.barId - 1); //CHANNGGEEED
		Bar prev = OneMoreTimeStrategy.getInstance().bars.get(this.id - 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = prev.topLeft;
			} else {
				target = prev.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = prev.bottomLeft;
			} else {
				target = prev.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		// System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	public Point goRightToNextBar(Point me) {
		// Point newLoc = new Point(Player.HORIZONTAL_GAP + Player.BAR_GAP, 0);

		// find next bar
		// Bar next = LoveBirdStrategy.getInstance().bars.get(this.barId + 1);
		Bar next = OneMoreTimeStrategy.getInstance().bars.get(this.id + 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = next.topLeft;
			} else {
				target = next.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = next.bottomLeft;
			} else {
				target = next.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		// System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	/* Bar ID update */
	public void moveToNextBar(int id, Map<Integer, Integer> idToBar) {
		int barId = idToBar.get(id);
		idToBar.put(id, barId + 1);
		// System.out.format("Dancer %d will be moved to bar %d\n", id, idToBar.get(id));
	}

	public void moveToPrevBar(int id, Map<Integer, Integer> idToBar) {
		int barId = idToBar.get(id);
		if (barId <= 0)
			// System.out.println("Error: The dancer will be put to bar -1!");
		idToBar.put(id, barId - 1);
		// System.out.format("Dancer %d will be moved to bar %d\n", id, idToBar.get(id));
	}

	/* Legacy of week 1 */
	public Point move(Point dancer) {
		Point newLoc = null;

		/* 1st week legacy solution */
		// check left column
		if (dancer.x < center.x) {
			// if it's the first element in left column, go right
			if (ToolBox.compareDoubles(dancer.y, topLeft.y)) {
				// newLoc = new Point(dancer.x + HORIZONTAL_GAP, dancer.y);
				newLoc = new Point(OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
			}
			// else, just go up
			else {
				// newLoc = new Point(dancer.x, dancer.y - VERTICAL_GAP);
				newLoc = new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
		}
		// right column
		else {
			// if it's the end of the right column
			if (ToolBox.compareDoubles(dancer.y, bottomRight.y)) {
				// newLoc = new Point(dancer.x - HORIZONTAL_GAP, dancer.y);
				newLoc = new Point(-OneMoreTimeStrategy.HORIZONTAL_GAP, 0);
			}
			// else go down
			else {
				// newLoc = new Point(dancer.x, dancer.y + VERTICAL_GAP);
				newLoc = new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
			}
		}

		return newLoc;
	}

	/* Week 2 moving */
	public Point move(Point dancer, int dancerId, Map<Integer, Integer> idToBar) {
		Point newLoc = null;

		/* 2nd week solution */
		/* Common case */
		// left column
		if (dancer.x < center.x) {
			if (isEven) {
				newLoc = goUp(dancer);
			} else {
				newLoc = goDown(dancer);
			}
		}

		// right column
		if (dancer.x > center.x) {
			if (isEven) {
				newLoc = goDown(dancer);
			} else {
				newLoc = goUp(dancer);
			}
		}

		/* Special case */
		// top left
		if (ToolBox.comparePoints(dancer, topLeft) && !ToolBox.comparePoints(bottomLeft, topLeft)) { //top left
			// System.out.println("");
			// if the bar interacts with another
			if (upConnected) {
				if (isEven) {
					// System.out.println("even upConnected");
					newLoc = goRightToNextBar(dancer);
					// System.out.println("go right");
					moveToNextBar(dancerId, idToBar);

				} else {
					// go down
					newLoc = goDown(dancer);
				}
			} else {
				// go right
				if (isEven){
					newLoc = goRight(dancer);
				} else {
					newLoc = goDown(dancer);					
				}
			}
		} else if (ToolBox.comparePoints(dancer, topRight) && !ToolBox.comparePoints(topRight, bottomRight)) { //top right
			if (upConnected) {
				if (isEven) {
					// go down
					newLoc = goDown(dancer);
				} else {
					newLoc = goLeftToNextBar(dancer);
					moveToPrevBar(dancerId, idToBar);
				}
			} else {
				// go down
				if (isEven){
					newLoc = goDown(dancer);
				} else {
					newLoc = goLeft(dancer);					
				}
			}
		} else if (ToolBox.comparePoints(dancer, bottomLeft)) { //bottom left
			// System.out.println("Bottom left: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					// go
					newLoc = goRightToNextBar(dancer);
					moveToNextBar(dancerId, idToBar);
				}
			} else {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					newLoc = goRight(dancer);
				}
			}
		} else if (ToolBox.comparePoints(dancer, bottomRight)) { //bottom right
			// System.out.println("Bottom right: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					newLoc = goLeftToNextBar(dancer);
					moveToPrevBar(dancerId, idToBar);
				} else {
					newLoc = goUp(dancer);
				}
			} else {
				if (isEven) {
					newLoc = goLeft(dancer);
				} else {
					newLoc = goUp(dancer);
				}
			}

		}

		if (newLoc == null) {
			// System.out.println("New location not decided for point " + dancer);
		}

		// Validation
		Point newPos = dancer.add(newLoc);
		if (!ToolBox.validatePoint(newPos, Player.roomSide)) {
			// System.out.format("Error: Invalid point (%f, %f)\n", newPos.x, newPos.y);
			return dancer;
		} else {
			return newLoc;
		}
	}


	public void swapPoints(Point[] points, int index0, int index1) {
		if (index0 >= points.length) {
			// System.out.format("Index %d out of range in swapping\n", index0);
		} else if (index1 >= points.length) {
			// System.out.format("Index %d out of range in swapping\n", index1);
		}
		Point temp = points[index0];
		points[index0] = points[index1];
		points[index1] = temp;
		return;
	}

	/* Swapping */
	public Point innerSwap(Point dancer) {
		// find the relative id in the bar
		int relativeId = findRelativeIdInBar(dancer, this);

		// Top left and bottom right do not swap whatsoever
		if (ToolBox.comparePoints(dancer, topLeft))
			return new Point(0, 0);
		if (ToolBox.comparePoints(dancer, bottomRight))
			return new Point(0, 0);

		// Left column
		if (dancer.x < center.x) {
			// if going down will exceed the bottom, don't
			if (dancer.y + OneMoreTimeStrategy.VERTICAL_GAP > bottomLeft.y)
				return new Point(0.0, 0.0);
			else {
				if (relativeId % 4 == 2)
					return new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
				if (relativeId % 4 == 0)
					return new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
			// Should not reach here because all situations should have been
			// handled
			// System.out.println("Point " + dancer + " is not handled in left side of bar centering at " + this.center);

		}
		// right column
		else {
			// if going down will reach bottom right point, don't
			if (ToolBox.compareDoubles(dancer.y + OneMoreTimeStrategy.VERTICAL_GAP, bottomRight.y))
				return new Point(0, 0);
			else {
				if (relativeId % 4 == 1)
					return new Point(0, OneMoreTimeStrategy.VERTICAL_GAP);
				if (relativeId % 4 == 3)
					return new Point(0, -OneMoreTimeStrategy.VERTICAL_GAP);
			}
			// Should not reach here because all situations should have been
			// handled
			// System.out.println("Point " + dancer + " is not handled in right side of bar centering at " + this.center);
		}

		return new Point(0, 0);
	}

	/* Static functions */
	public static int findRelativeIdInBar(Point p, Bar b) {
		// check whether it's in this bar
		double diffX = p.x - b.center.x;
		if (Math.abs(diffX) > 0.26) {
			// System.out.println("Point " + p + "is not in bar centering at " +
			// b.center);
			return -1;
		}

		// calculate the row number
		double diffY = p.y - b.topLeft.y;
		int row = (int) Math.round(diffY / OneMoreTimeStrategy.VERTICAL_GAP);
		int index;
		if (p.x < b.center.x)
			return row * 2;
		else
			return row * 2 + 1;
	}

	@Override
	public String toString() {
		return "Bar " + id;
	}

	@Override
	public void recordDancer(int pid, Point positon, int row) {
		// TODO Auto-generated method stub

	}

	@Override
	public void recordDancers(List<Integer> pids) {
		int totalNum = leftColumn.size() + rightColumn.size();

		if (pids.size() != totalNum) {
			// System.out.format("Error: dancer number %d doesn't match ID number %d in %s!", totalNum, pids.size(), this);
			return;
		}

		// Update records in Shape
		for (int i = 0; i < pids.size(); i++) {
			int pid = pids.get(i);

			// find the position
			Point position;
			if (i % 2 == 0) {
				position = leftColumn.get(i / 2);
			} else {
				position = rightColumn.get((i - 1) / 2);
			}
			// = spots.get(i);
			int row = findRowByPosition(position);

			// Update data structure in Shape
			dancerId.add(pid);
			idToRow.put(pid, row);
			idToPosition.put(pid, position);
		}

		// System.out.println("Recorded " + pids.size() + " dancers in line " + this.id);
		return;

	}

	@Override
	public int findRowByPosition(Point position) {
		double diffX = position.x - this.center.x;
		if (Math.abs(diffX) > 0.6 * LineStrategy.HORIZONTAL_GAP) {
			// System.out.println("Error: " + position + " is not in " + this);
			return -1;
		}

		double diffY = position.y - this.topLeft.y;
		double row = diffY / LineStrategy.VERTICAL_GAP;

		if (Math.abs(Math.round(row) - row) > 0.2) {
			// System.out.println("Error: " + row + " is too far away from a row by distance of " + diffY + " to head");
		}

		return (int) Math.round(row);
	}

	public Set<Integer> findLeftColumn() {
		Set<Integer> results = new HashSet<>();

		for (int t : dancerId) {
			Point p = idToPosition.get(t);

			int column = column(p);
			if (column == 0) {
				results.add(t);
			} else if (column == -1) {
				// System.out.println("Error: Dancer " + t + " is recorded in the bar but not in any column!");
				return results;
			}

		}

		if (results.size() != (dancerId.size() / 2)) {
			// System.out.println(
			// 		"Error: Only " + results.size() + " dancers in the left column in a bar of " + dancerId.size());
		}

		return results;
	}

	public void updateRecord(int pid, int row, Point position) {
		if (!idToRow.containsKey(pid)) {
			// System.out.println("Error: " + pid + " is not in " + this);
			return;
		}
		int currentRow = idToRow.get(pid);
		int diffRow = row - currentRow;
		if (diffRow != 1 && diffRow != -1) {
			// System.out.println("Error: Moving dancer " + pid + " from row " + currentRow + " to " + row);
		}
		idToRow.put(pid, row);

		idToPosition.put(pid, position);
	}
	/*************************************Copy of Move and Copy in order to do soulmate stufff    */



	/* Soulmate movement */
	public Map<Integer, Point> doSoulmateMove(int direction, Pair p, Map<Integer, Integer> idToBar) {
		//if even go down //if odd go up
		//check if next is in honeymoon suite
		// decides which of the partners is on the left side and right side
		Map<Integer, Point> moves = new HashMap<>();
		Point dancer1 = new Point(0,0);
		Point dancer2 = new Point(0,0);
		Bar prev = LoveBirdStrategy.getInstance().bars.get(0);
		Bar next = LoveBirdStrategy.getInstance().bars.get(0);
		if (id>0)
			prev = LoveBirdStrategy.getInstance().bars.get(this.id - 1);
		if (id<LoveBirdStrategy.getInstance().bars.size()-1)
			next = LoveBirdStrategy.getInstance().bars.get(this.id + 1);

		/* 2nd week solution */
		if (direction == -1 && isEven || direction ==1 && !isEven) {
			dancer1 = goDown(p.leftdancer);
			dancer2 = goDown(p.rightdancer);
		} else {
			dancer1 = goUp(p.leftdancer);
			dancer2 = goUp(p.rightdancer);
		}

		//top
			//go left if 

		/* Special case */
		// top 
		if (ToolBox.comparePoints(p.leftdancer, topLeft)) {
			// if the bar interacts with another
			if (!upConnected){
				dancer1 = new Point(0,0);
				dancer2 = new Point(0,0);
			} else{
				if (isEven&&direction == 1){
					//right
					dancer1 = ToolBox.pointsDifferencer(p.leftdancer, next.topLeft);
					dancer2 = ToolBox.pointsDifferencer(p.rightdancer, next.topRight);
					// System.out.println("In order to reach the target I should go " + dancer1);
					// System.out.println("In order to reach the target I should go " + dancer2);
					moveToNextBar(p.leftid, idToBar);
					moveToNextBar(p.rightid, idToBar);
				}
				else if( !isEven && direction == -1){
					//left
					dancer1 = ToolBox.pointsDifferencer(p.leftdancer, prev.topLeft);
					dancer2 = ToolBox.pointsDifferencer(p.rightdancer, prev.topRight);
					// System.out.println("In order to reach the target I should go " + dancer1);
					// System.out.println("In order to reach the target I should go " + dancer2);
					moveToPrevBar(p.leftid, idToBar);
					moveToPrevBar(p.rightid, idToBar);
				}
			}
			
		}
		// bottom
		else if (ToolBox.comparePoints(p.leftdancer, bottomLeft)||ToolBox.comparePoints(p.rightdancer, bottomLeft)) {
			if(!bottomConnected){
				dancer1 = new Point(0,0);
				dancer2 = new Point(0,0);				
			} else {
				if (isEven&&direction == -1){
					//left
					dancer1 = ToolBox.pointsDifferencer(p.leftdancer, prev.bottomLeft);
					dancer2 = ToolBox.pointsDifferencer(p.rightdancer, prev.bottomRight);
					// System.out.println("In order to reach the target I should go " + dancer1);
					// System.out.println("In order to reach the target I should go " + dancer2);
					moveToPrevBar(p.leftid, idToBar);
					moveToPrevBar(p.rightid, idToBar);
				}
				else if( !isEven && direction == 1){
					//right
					dancer1 = ToolBox.pointsDifferencer(p.leftdancer, next.bottomLeft);
					dancer2 = ToolBox.pointsDifferencer(p.rightdancer, next.bottomRight);
					// System.out.println("In order to reach the target I should go " + dancer1);
					// System.out.println("In order to reach the target I should go " + dancer2);
					moveToNextBar(p.leftid, idToBar);
					moveToNextBar(p.rightid, idToBar);
				}
			}
		} 


		moves.put(p.leftid, dancer1);
		moves.put(p.rightid, dancer2);

		
		// System.out.println("Moved Soulmates: Bar: "+id+", Dancer"+p.leftid+": "+dancer1.x+","+dancer1.y+";Dancer"+p.rightid+": "+dancer2.x+","+dancer2.y);
		return moves;

	}

	public List<Integer> updateHoneymoonSuite(List<Pair> soulmates, int[] enjoyment_gained, Map<Integer, Integer> idToBar){
		List<Integer> newlyweds = new ArrayList<>();

		int numOfBars = LoveBirdStrategy.getInstance().bars.size();
		int numOfPairs = enjoyment_gained.length/2;
		int direction = 0;
		if (id < numOfBars/2)
			direction = -1;
		else if (id >= numOfBars/2 && numOfBars%2 == 0)
			direction = 1;

		//cuts down to the soulmates in the current bar
		List<Pair> relevant = new ArrayList<Pair>();
		for (Pair p: soulmates){
			if (idToBar.get(p.leftid) == id && enjoyment_gained[p.leftid] == 6)
				relevant.add(p);
		}
		// System.out.print ("Newlywed direction: "+direction);
		boolean found = true;
		if (!bottomConnected && (direction ==-1 && isEven || direction==1 && !isEven || direction == 0)){
			Point row = bottomLeft;
			while (found && (row.y > topLeft.y || ToolBox.comparePoints(row, topLeft))){
				found = false;
				for (Pair p: relevant){
					if (ToolBox.comparePoints(p.leftdancer, row)){
						newlyweds.add(p.leftid);
						// System.out.print("found");
						found = true;
						row = row.add(goUp(row));
					}
				}
			}
			if (row.y < topLeft.y){
				bottomLeft = topLeft;
				bottomRight = topRight;
				upConnected = false;
				if(isEven && LoveBirdStrategy.getInstance().bars.size() > id + 1)
					LoveBirdStrategy.getInstance().bars.get(id+1).upConnected = false;
				if(!isEven && id > 0)
					LoveBirdStrategy.getInstance().bars.get(id-1).upConnected = false;
				// System.out.println("CLOSED BAR");
			} else if (row.y < bottomLeft.y){
				bottomLeft = row;
				bottomRight = row.add(goRight(row));
			}

		} 
		found = true;
		if (!upConnected && (direction ==1 && isEven || direction==-1 && !isEven || direction == 0)) {
			Point row = topLeft;
			while (found && (row.y < bottomLeft.y || ToolBox.comparePoints(row, bottomLeft))){
				found = false;
				for (Pair p: relevant){
					if (ToolBox.comparePoints(p.leftdancer, row)){
						newlyweds.add(p.leftid);
						// System.out.print("found");
						found = true;
						row = row.add(goDown(row));
					}
				}
			}
			if (row.y > bottomLeft.y){
				topLeft = bottomLeft;
				topRight = bottomRight;
				bottomConnected = false;
				if(isEven && id > 0)
					LoveBirdStrategy.getInstance().bars.get(id-1).bottomConnected = false;
				if(!isEven && LoveBirdStrategy.getInstance().bars.size() > id + 1)
					LoveBirdStrategy.getInstance().bars.get(id+1).bottomConnected = false;
				// System.out.println("CLOSED BAR");

			} else if (row.y > topLeft.y){
				topLeft = row;
				topRight = row.add(goRight(row));
			}



		}
		return newlyweds;
		//for a given bar 
			//check if bottom equals top, all soulmates in that bar should be honeymooners
			//otherwise start and bottom and as long as they are soulmates chop list add to honeymoon list
		//return updated honey moon list or list of new honeymooners

	}

	public Point goLeftToNextBarSoul(Point me) {

		// Point newLoc = new Point(0 - Player.HORIZONTAL_GAP - Player.BAR_GAP,
		// 0);

		// find previous bar
		// Bar prev = LoveBirdStrategy.getInstance().bars.get(this.barId - 1); //CHANNGGEEED
		Bar prev = LoveBirdStrategy.getInstance().bars.get(this.id - 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = prev.topLeft;
			} else {
				target = prev.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = prev.bottomLeft;
			} else {
				target = prev.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		// System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	public Point goRightToNextBarSoul(Point me) {
		// Point newLoc = new Point(Player.HORIZONTAL_GAP + Player.BAR_GAP, 0);

		// find next bar
		// Bar next = LoveBirdStrategy.getInstance().bars.get(this.barId + 1);
		Bar next = LoveBirdStrategy.getInstance().bars.get(this.id + 1);

		// find the target to go to
		Point target;
		if (me.y < center.y) {
			if (me.x < center.x) {
				target = next.topLeft;
			} else {
				target = next.topRight;
			}
		} else {
			if (me.x < center.x) {
				target = next.bottomLeft;
			} else {
				target = next.bottomRight;
			}
		}

		// Debug: Validation
		Point diff = ToolBox.pointsDifferencer(me, target);
		// System.out.println("In order to reach the target I should go " + diff);

		return diff;
	}

	/* Week 2 moving */
	public Point moveSoul(Point dancer, int dancerId, Map<Integer, Integer> idToBar) {
		Point newLoc = null;

		/* 2nd week solution */
		/* Common case */
		// left column
		if (dancer.x < center.x) {
			if (isEven) {
				newLoc = goUp(dancer);
			} else {
				newLoc = goDown(dancer);
			}
		}

		// right column
		if (dancer.x > center.x) {
			if (isEven) {
				newLoc = goDown(dancer);
			} else {
				newLoc = goUp(dancer);
			}
		}

		/* Special case */
		// top left
		if (ToolBox.comparePoints(dancer, topLeft) && (!ToolBox.comparePoints(bottomLeft, topLeft)||isEven)) { //top left
			// System.out.println("");
			// if the bar interacts with another
			if (upConnected) {
				if (isEven) {
					// System.out.println("even upConnected");
					newLoc = goRightToNextBarSoul(dancer);
					// System.out.println("go right");
					moveToNextBar(dancerId, idToBar);

				} else {
					// go down
					newLoc = goDown(dancer);
				}
			} else {
				// go right
				if (isEven){
					newLoc = goRight(dancer);
				} else {
					newLoc = goDown(dancer);					
				}
			}
		} else if (ToolBox.comparePoints(dancer, topRight) && (!ToolBox.comparePoints(topRight, bottomRight)||!isEven)) { //top right
			if (upConnected) {
				if (isEven) {
					// go down
					newLoc = goDown(dancer);
				} else {
					newLoc = goLeftToNextBarSoul(dancer);
					moveToPrevBar(dancerId, idToBar);
				}
			} else {
				// go down
				if (isEven){
					newLoc = goDown(dancer);
				} else {
					newLoc = goLeft(dancer);					
				}
			}
		} else if (ToolBox.comparePoints(dancer, bottomLeft)) { //bottom left
			// System.out.println("Bottom left: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					// go
					newLoc = goRightToNextBarSoul(dancer);
					moveToNextBar(dancerId, idToBar);
				}
			} else {
				if (isEven) {
					// go up
					newLoc = goUp(dancer);
				} else {
					newLoc = goRight(dancer);
				}
			}
		} else if (ToolBox.comparePoints(dancer, bottomRight)) { //bottom right
			System.out.println("Bottom right: Dancer " + dancerId);
			if (bottomConnected) {
				if (isEven) {
					newLoc = goLeftToNextBarSoul(dancer);
					moveToPrevBar(dancerId, idToBar);
				} else {
					newLoc = goUp(dancer);
				}
			} else {
				if (isEven) {
					newLoc = goLeft(dancer);
				} else {
					newLoc = goUp(dancer);
				}
			}

		}

		if (newLoc == null) {
			// System.out.println("New location not decided for point " + dancer);
		}

		// Validation
		Point newPos = dancer.add(newLoc);
		if (!ToolBox.validatePoint(newPos, Player.roomSide)) {
			// System.out.format("Error: Invalid point (%f, %f)\n", newPos.x, newPos.y);
			return dancer;
		} else {
			return newLoc;
		}
	}


}
