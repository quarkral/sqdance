package sqdance.g5;

import java.util.*;

import sqdance.sim.Point;

public class LineStrategy implements ShapeStrategy {

	private static LineStrategy instance;

	public List<Line> leftLines;
	public List<Line> rightLines;
	public List<Bar> bars;

	public Map<Integer, Shape> mapping;

	// Global count
	public int turn = 0;

	/* Debug purpose: Keep move of last turn */
	public Point[] lastMoves;

	// Define gaps
	public static final double HORIZONTAL_GAP = 0.5 + 0.00001; // Horizontal gap
																// between
																// dancers in a
																// bar
	public static final double VERTICAL_GAP = 0.5 + 0.002; // Vertical gap
															// between dancers
															// in a bar
	public static final double BAR_GAP = 0.5 + 0.002; // Gap between bars
	public static final double LINE_GAP = 0.1 + 0.00001; // Gap between lines

	// Define intervals
	public static int MOVE_INTERVAL;
	public static final int SWAP_INTERVAL = 2;
	public static int VERTICAL_SWAP_INTERVAL = 2;

	public static Random random = new Random();

	// Record target member in the line to swap
	public int barLineOffset = 0; // This will be 0 - 4

	private LineStrategy() {
		leftLines = new ArrayList<>();
		rightLines = new ArrayList<>();
		bars = new ArrayList<>();

		mapping = new HashMap<>();
		// System.out.println("Line strategy is chosen");
	}

	public static LineStrategy getInstance() {
		if (instance == null)
			instance = new LineStrategy();
		return instance;
	}

	public static LineStrategy anotherInstance() {
		return new LineStrategy();
	}

	@Override
	public Point[] generateStartLocations(int number) {
		// decide number of bars in the middle
		int numberOfBars = bruteForceNumberOfBars(number);

		// finalize number of lines on each side
		int lineOnEachSide = numberOfLinesEachSide(number, numberOfBars);
		// System.out.format("%d bars in the middle and %d lines on each side", numberOfBars, lineOnEachSide);

		// Calculate how many turns to dance before move
		int t = estimateResultAndBruteForceT(numberOfBars, lineOnEachSide);
		// int t = 1;
		MOVE_INTERVAL = t + 1;
		// System.out.println("Able to dance " + t + " turns before moving. Total interval is " + MOVE_INTERVAL);

		// Calculate how many moves in horizontal before vertically swap
		int h = estimateVerticalSwapInterval(numberOfBars, lineOnEachSide);
		VERTICAL_SWAP_INTERVAL = h;
		// System.out.println("Vertical swap happens every " + VERTICAL_SWAP_INTERVAL + " moves");

		// generate population allocation
		Map<Integer, Integer> barsPopMap = OneMoreTimeStrategy.getInstance()
				.distributePeopleToBars(numberOfBars * Player.BAR_MAX_VOLUME);
		int waitInLines = number - ToolBox.totalCount(barsPopMap);
		Map<Integer, Integer> linePopMap = distributePeopleToLines(waitInLines / 2, lineOnEachSide);

		// generate start location
		List<Point> points = new LinkedList<>();

		// lines on the left
		Point startCenter = new Point(0.0000001, 10.0);
		for (int i = 0; i < lineOnEachSide; i++) {
			int targetPop = linePopMap.get(i);
			Line newLine;

			if (targetPop < 200) {
				newLine = new Line(targetPop, startCenter, i, true);
			} else {
				newLine = new Line(targetPop, startCenter, i);
			}
			leftLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, 10.0);
		}
		startCenter = new Point(startCenter.x + BAR_GAP + HORIZONTAL_GAP * 0.5, startCenter.y);
		// System.out.println("Finished generate " + leftLines.size() + " lines
		// on the left. Now the starting point is "
		// + startCenter);

		// bars in the middle
		for (int i = 0; i < numberOfBars; i++) {
			int targetPop = barsPopMap.get(i);
			Bar newBar = new Bar(targetPop, startCenter, i);
			bars.add(newBar);
			if (i != numberOfBars - 1)
				startCenter = new Point(startCenter.x + HORIZONTAL_GAP + BAR_GAP, startCenter.y);
		}
		startCenter = new Point(startCenter.x + 0.5 * HORIZONTAL_GAP + BAR_GAP, startCenter.y);

		// lines on the right
		for (int i = 0; i < lineOnEachSide; i++) {
			int targetPop = linePopMap.get(lineOnEachSide - 1 - i);
			// Line newLine = new Line(targetPop, startCenter, i +
			// lineOnEachSide);
			Line newLine;

			if (targetPop < 200) {
				newLine = new Line(targetPop, startCenter, i + lineOnEachSide, true);
			} else {
				newLine = new Line(targetPop, startCenter, i + lineOnEachSide);
			}

			rightLines.add(newLine);
			if (i != lineOnEachSide - 1)
				startCenter = new Point(startCenter.x + LINE_GAP, startCenter.y);
		}

		// get people's positions and store the pid - bar/line mapping
		int pid = 0;
		for (Line line : leftLines) {
			List<Point> spots = line.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					// System.out.println(line + "has returned null point at pid " + pid);
				}

				mapping.put(pid, line);

				pids.add(pid);
				pid++;
			}

			points.addAll(spots);

			// Record
			line.recordDancers(pids);

			// System.out.println("Added line " + line.id + ". Now size is " +
			// points.size());
		}
		for (Bar bar : bars) {
			List<Point> spots = bar.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					// System.out.println(bar + "has returned null point at pid " + pid);
				}

				mapping.put(pid, bar);

				pids.add(pid);

				pid++;
			}
			// Record
			bar.recordDancers(pids);

			points.addAll(spots);
			// System.out.println("Added bar " + bar.id + ". Now size is " + points.size());
		}
		for (Line line : rightLines) {
			List<Point> spots = line.getPoints();
			List<Integer> pids = new LinkedList<>();

			for (int i = 0; i < spots.size(); i++) {
				if (spots.get(i) == null) {
					// System.out.println(line + "has returned null point at pid " + pid);
				}

				mapping.put(pid, line);

				pids.add(pid);

				pid++;
			}
			points.addAll(spots);

			// Record
			line.recordDancers(pids);

			// System.out.println("Added line " + line.id + ". Now size is " + points.size());
		}

		// convert to required form
		Point[] results = points.toArray(new Point[number]);

		// report
		// debrief();

		return results;
	}

	public int estimateVerticalSwapInterval(int barNum, int lineOnEachSide) {
		int interval = 5 * lineOnEachSide + barNum;
		int target = (int) Math.ceil(((1800 + 0.0) / 40) / interval);

		return Math.max(target, 2);
	}

	public Point[] generateDummyMoves(int num) {
		Point[] results = new Point[num];
		for (int i = 0; i < num; i++) {
			results[i] = new Point(0, 0);
		}
		return results;
	}

	public Point[] performMoves(Point[] dancers, Map<Integer, Move> moves) {
		Point[] results = new Point[dancers.length];

		for (int i = 0; i < dancers.length; i++) {
			if (moves.containsKey(i)) {
				Point step = performMove(dancers[i], moves.get(i));
				results[i] = step;
			} else {
				results[i] = new Point(0, 0);
			}
		}

		return results;

	}

	// Take care of the updates and generate the step
	public Point performMove(Point current, Move m) {
		int pid = m.id;
		Point targetPos = m.targetPos;
		int targetRow = m.targetRow;

		/* Debug */
		if (pid == 880 | pid == 882) {
			// System.out.println("Time!");
		}

		// Check if the dancer is moved to another shape
		Shape currentShape = mapping.get(pid);
		Shape shape = m.targetShape;

		// different shape
		if (currentShape != shape) {

			// System.out.println("Dancer " + pid + " is moving from " +
			// currentShape + " to " + shape);

			// Strategy specific
			mapping.put(m.id, shape);

			// Remove from current shape
			currentShape.dancerId.remove(pid);
			currentShape.idToPosition.remove(pid);
			currentShape.idToRow.remove(pid);

			// no need to remove rowToId since it will be overlapped

		}
		// stay in same shape
		else {
			// System.out.println("Dancer " + pid + " is staying in " + shape);
		}

		// Shape generic
		shape.dancerId.add(pid);
		shape.idToPosition.put(pid, targetPos);
		shape.idToRow.put(pid, targetRow);

		// Line specific
		if (shape instanceof Line) {
			((Line) shape).rowToId.put(targetRow, pid);
		}

		// Calculate the step
		Point result = ToolBox.pointsDifferencer(current, targetPos);

		return result;
	}

	public void interrogate(int pid, Point[] dancers, int[] partner_ids, int[] enjoyment_gained) {

		// System.out.println("Interrogating dancer " + pid);

		if (pid == 600 || pid == 882) {
			// System.out.println("Time");
		}

		// Was it moving last turn?
		// System.out.println("Move last turn: " + lastMoves[pid]);

		// Distance too small
		Shape shape = mapping.get(pid);
		if (shape instanceof Line) {
			// System.out.println("Error: Dancer is in line " + shape);

		} else {
			Bar bar = (Bar) shape;

			int row = bar.idToRow.get(pid);

			// find the dancer in same row in same bar
			Set<Integer> rowMates = findDancersWithBarAndRow(row, bar);
			for (int r : rowMates) {
				deeperCheck(pid, r, dancers);
			}

			// check the row above and row below
			Set<Integer> upperRow = findDancersWithBarAndRow(row - 1, bar);
			if (upperRow.size() == 0) {
				;
			} else {
				for (int r : upperRow) {
					deeperCheck(pid, r, dancers);
				}
			}

			Set<Integer> lowerRow = findDancersWithBarAndRow(row + 1, bar);
			if (lowerRow.size() == 0) {
				;
			} else {
				for (int r : lowerRow) {
					deeperCheck(pid, r, dancers);
				}
			}

			// find the dancer in same row in adjacent bars
			int column = bar.column(bar.idToPosition.get(pid));
			if (column == 0) {
				// check with previous bar
				Bar prevBar = findPrevBar(bar);
				if (prevBar != null) {
					Set<Integer> neighborMates = findDancersWithBarAndRow(row, prevBar);
					for (int r : neighborMates) {
						if (pid != r) {
							// System.out.println(pid + " should be dancing with " + r);
							deeperCheck(pid, r, dancers);
						}
					}
				}
			} else if (column == 1) {
				// check with next bar
				Bar nextBar = findNextBar(bar);
				if (nextBar != null) {
					Set<Integer> neighborMates = findDancersWithBarAndRow(row, nextBar);
					for (int r : neighborMates) {
						deeperCheck(pid, r, dancers);
					}
				}
			} else {
				// System.out.println("Error: dancer " + pid + " is not in any column of " + bar);
			}

		}
	}

	public void deeperCheck(int a, int b, Point[] dancers) {
		// Check current distance of the two dancers
		Point pointA = dancers[a];
		Point pointB = dancers[b];

		// Find the recorded distance of the two dancers
		Point recordA = mapping.get(a).idToPosition.get(a);
		Point recordB = mapping.get(b).idToPosition.get(b);

		// check the difference between record and reality
		checkDifference(a, pointA, recordA);
		checkDifference(b, pointB, recordB);

		// calculate the distance
		// System.out.println(a + " and " + b + " has distance " + ToolBox.distance(pointA, pointB));
	}

	public void checkDifference(int pid, Point reality, Point theory) {
		double distance = ToolBox.distance(reality, theory);
		if (distance > 0.001) {
			// System.out.println("Dancer " + pid + " has a difference between " + theory + " and " + reality);
		}
	}

	public Set<Integer> findDancersWithBarAndRow(int row, Bar bar) {
		Set<Integer> set = new HashSet<>();
		for (Integer pid : bar.dancerId) {
			int r = bar.idToRow.get(pid);
			if (r == row)
				set.add(pid);
		}
		return set;
	}

	@Override
	public Point[] nextMove(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		// System.out.println("Turn " + turn);

		/* Debug: check scores */
		int gainedScore = 0;
		for (int i = 0; i < enjoyment_gained.length; i++) {
			if (enjoyment_gained[i] != 0) {
				gainedScore++;
			}
		}
		// System.out.println(gainedScore + " dancers has gained score this turn");

		Point[] results = new Point[dancers.length];

		/* Debug */
		Set<Integer> targets = new HashSet<>();
		targets.add(1360);
		targets.add(619);
		targets.add(1440);

//		/* Debug */
//		for (int t : targets) {
//			System.out.println("Before movement, dancer " + t + " is at " + dancers[t] + " in " + mapping.get(t));
//		}

		/* Moves */
		Map<Integer, Move> moves = new HashMap<>();

		if (turn % MOVE_INTERVAL == 0) {
			// System.out.println("Turn " + turn + ". Time to move.");

//			/* Debug */
//			// Last turn must be dancing
//			if (turn == 0) {
//				System.out.println("Turn 0. No need to check");
//			} else {
//
//				for (int i = 0; i < enjoyment_gained.length; i++) {
//					int enj = enjoyment_gained[i];
//					if (mapping.get(i) instanceof Bar && enj == 0) {
//						System.out.println("Dancer " + i + " didn't get any enjoyment last turn dancing");
//
//						// Check what happened
//						interrogate(i, dancers, partner_ids, enjoyment_gained);
//					}
//				}
//			}

			// ensure there are at least 1 lines waiting
			Bar leftBar = bars.get(0);
			Line leftLine = leftLines.get(leftLines.size() - 1);

			Bar rightBar = bars.get(bars.size() - 1);
			Line rightLine = rightLines.get(0);

			/* Bar - Line interaction */
			moveBetweenBarAndLine(leftBar, leftLine, moves, true);
			moveBetweenBarAndLine(rightBar, rightLine, moves, false);

			/* Move in bars */
			moveWithinBars(dancers, moves);
			// System.out.println(moves.size() + " moves decided");

			// Swap within column
			swapInLeftColumn(bars, moves);

			/* Sink with lines */
			moveWithinLines(leftLines, rightLines, moves, scores);

			// Perform the moves
			results = performMoves(dancers, moves);

			// update offset
			updateInteractionOffset();

		} else {
			// System.out.println("Turn " + turn + ". Keep dancing.");

			// Introduce some randomness
			randomSwapInLines(leftLines, rightLines, moves);
		
			/* Sink with lines */
			moveWithinLines(leftLines, rightLines, moves, scores);

			// results = generateDummyMoves(dancers.length);

			// Perform the moves
			results = performMoves(dancers, moves);
		}

		// update turn count
		// System.out.println("Performed all moves. Now update turn");
		turn++;

		// Record the moves
		lastMoves = results;

		return results;
	}

	// For each bar, swap the order in the left column
	public void swapInLeftColumn(List<Bar> bars, Map<Integer, Move> moves) {
		// validation
		if (turn % MOVE_INTERVAL != 0) {
			// System.out.println("Error: The swap should not happen.");
			return;
		}

		// Decide whether to swap
		int moveTime = turn / MOVE_INTERVAL;
		boolean shouldSwap = (moveTime % SWAP_INTERVAL == 0);
		if (!shouldSwap) {
			// System.out.println("Turn " + turn + ". No need for vertical swap.");
			return;
		}

		// Decide whether swapping contains head and tail
		int swapTime = moveTime / SWAP_INTERVAL;
		boolean swapFromHead = (swapTime % VERTICAL_SWAP_INTERVAL == 0);
		// boolean swapFromHead = (random.nextDouble() < 0.5);

		// System.out.println("Swap includes head and tail? " + swapFromHead);

		// Swapping action
		for (Bar bar : bars) {
			if (bar.id == 0) {
				// System.out.println("The leftmost bar doesn't need to swap");
				continue;
			}

			// Find the dancers to swap
			Set<Integer> dancersInLeft = bar.findLeftColumn();

			// Start swapping
			for (int d : dancersInLeft) {
				int row = bar.idToRow.get(d);
				Point current = bar.idToPosition.get(d);

				// If swap starts from head
				Point targetDir;
				int targetRow;
				if (swapFromHead) {
					// go down
					if (row % 2 == 0) {
						targetDir = new Point(0, VERTICAL_GAP);
						targetRow = row + 1;
					}
					// go up
					else {
						targetDir = new Point(0, -VERTICAL_GAP);
						targetRow = row - 1;
					}
				}
				// If swap excludes head
				else {
					// Exclude head and tail
					if (row == 0 || row == 39) {
						continue;
					}

					// go up
					if (row % 2 == 0) {
						targetDir = new Point(0, -VERTICAL_GAP);
						targetRow = row - 1;
					}
					// go down
					else {
						targetDir = new Point(0, VERTICAL_GAP);
						targetRow = row + 1;
					}
				}

				// Combine the move if existing
				if (moves.containsKey(d)) {
					Move lastMove = moves.get(d);

					// Combine the move
					Point targetPos = lastMove.targetPos;
					Point combined = ToolBox.addTwoPoints(targetPos, targetDir);

					// Update
					Move combinedMove = new Move(d, (Bar) lastMove.targetShape, targetRow, 0);

					moves.put(d, combinedMove);

				}
				// Record the move if not
				else {
					// System.out
							// .println("Error: How come dancer " + d + " is not moving before swapping in turn " + turn);

					Point combined = ToolBox.addTwoPoints(current, targetDir);

					// Update
					Move theMove = new Move(d, bar, targetRow, 0);

					moves.put(d, theMove);
				}
			}

		}

		// System.out.println(moves.size() + " moves decided in vertical swapping.");
	}

	public void randomSwapInLines(List<Line> leftLines, List<Line> rightLines, Map<Integer, Move> moves) {
		Random random = new Random();
		Line thisLeftLine = leftLines.get(leftLines.size() - 1);
		Line thisRightLine = rightLines.get(0);
		double[] randomLeft = new double[200];
		double[] randomRight = new double[200];
		for (int i = 0; i < 200; i++) {
			randomLeft[i] = random.nextDouble();
			randomRight[i] = random.nextDouble();
		}

		for (int i = 0; i < 195; i++) {
			if (randomLeft[i] < 0.2) {
				int thisId = thisLeftLine.rowToId.get(i);
				int nextId = thisLeftLine.rowToId.get(i + 5);
				if (moves.containsKey(thisId) || moves.containsKey(nextId))
					continue;
				Move swapOut = new Move(thisId, thisLeftLine, i + 5);
				Move swapIn = new Move(nextId, thisLeftLine, i);
				moves.put(thisId, swapOut);
				moves.put(nextId, swapIn);

				// System.out.println(thisId + " swapping with " + nextId + " in " + thisLeftLine);

				i++;
			}

		}
		for (int i = 0; i < 195; i++) {
			if (randomRight[i] < 0.2) {
				int thisId = thisRightLine.rowToId.get(i);
				int nextId = thisRightLine.rowToId.get(i + 5);
				if (moves.containsKey(thisId) || moves.containsKey(nextId))
					continue;
				Move swapOut = new Move(thisId, thisRightLine, i + 5);
				Move swapIn = new Move(nextId, thisRightLine, i);
				moves.put(thisId, swapOut);
				moves.put(nextId, swapIn);

				// System.out.println(thisId + " swapping with " + nextId + " in " + thisRightLine);

				i++;
			}
		}
	}

	public void moveWithinLines(List<Line> leftLines, List<Line> rightLines, Map<Integer, Move> moves, int[] scores) {
		/* For left lines, sink to left and float to right */
		// int lengthOfLine = leftLines.get(0).spots.size();
		int lengthOfLine = 200;

		LoopRows: for (int i = 0; i < lengthOfLine; i++) {
			// Left side
			LoopLeftLines: for (int j = 0; j < leftLines.size(); j++) {
				if (j + 1 < leftLines.size()) {

					// Check this row exists
					Line thisLine = leftLines.get(j);
					Line nextLine = leftLines.get(j + 1);
					if (!thisLine.rowToId.containsKey(i)) {
						// System.out.println("No row " + j + " in " + thisLine
						// + ". Stop.");
						continue;
					}
					if (!nextLine.rowToId.containsKey(i)) {
						// System.out.println("No row " + j + " in next " +
						// thisLine + ". Stop.");
						continue;
					}

					// If row exists
					Map<Integer, Integer> thisRowToId = leftLines.get(j).rowToId;
					Map<Integer, Integer> nextRowTowId = leftLines.get(j + 1).rowToId;

					int thisId = leftLines.get(j).rowToId.get(i);
					int nextId = leftLines.get(j + 1).rowToId.get(i);
					
					//If already swapped, ignore
					if(moves.containsKey(thisId)||moves.containsKey(nextId))
						continue;
					
					
					
					Point thisPoint = leftLines.get(j).findPositionById(thisId);
					Point nextPoint = leftLines.get(j + 1).findPositionById(nextId);
					int thisScore = scores[thisId];
					int nextScore = scores[nextId];
					if (thisScore < nextScore) {

						// Move swapOut = new Move(thisId, nextPoint, nextLine,
						// i);
						// Move swapIn = new Move(nextId, thisPoint, thisLine,
						// i);

						// Update
						Move swapOut = new Move(thisId, nextLine, i);
						Move swapIn = new Move(nextId, thisLine, i);

						moves.put(thisId, swapOut);
						moves.put(nextId, swapIn);

						j++;
					}
				}
			}
			// Right side
			LoopRightLines: for (int j = rightLines.size() - 1; j >= 0; j--) {
				if (j - 1 >= 0) {
					// Check this row exists
					Line thisLine = rightLines.get(j);
					Line nextLine = rightLines.get(j - 1);
					if (!thisLine.rowToId.containsKey(i)) {
						// System.out.println("No row " + j + " in " + thisLine
						// + ". Stop.");
						continue;
					}
					if (!nextLine.rowToId.containsKey(i)) {
						// System.out.println("No row " + j + " in next " +
						// thisLine + ". Stop.");
						continue;
					}

					int thisId = rightLines.get(j).rowToId.get(i);
					int nextId = rightLines.get(j - 1).rowToId.get(i);
					
					//If already swapped, ignore
					if(moves.containsKey(thisId)||moves.containsKey(nextId))
						continue;
					
					Point thisPoint = rightLines.get(j).findPositionById(thisId);
					Point nextPoint = rightLines.get(j - 1).findPositionById(nextId);
					int thisScore = scores[thisId];
					int nextScore = scores[nextId];
					if (thisScore < nextScore) {

						// Move swapOut = new Move(thisId, nextPoint, nextLine,
						// i);
						// Move swapIn = new Move(nextId, thisPoint, thisLine,
						// i);

						// Update
						Move swapOut = new Move(thisId, nextLine, i);
						Move swapIn = new Move(nextId, thisLine, i);

						moves.put(thisId, swapOut);
						moves.put(nextId, swapIn);

						j--;
					}
				}
			}
		}
	}

	public void moveCheck(Point[] dancers, Map<Integer, Point> moves) {
		int pop = dancers.length;

		for (int i = 0; i < pop; i++) {
			if (!moves.containsKey(i)) {
				// System.out.format("Dancer %d at %s doesn't know how to move\n", i, dancers[i]);
			}
		}
	}

	public void updateInteractionOffset() {
		// System.out.println("Updating offset of bar - line interaction");
		if (barLineOffset == 4) {
			// System.out.println("Offset reaches 4. Reset to 0.");
			barLineOffset = 0;
		} else {
			barLineOffset++;

		}
		// System.out.println("Now the offset is " + barLineOffset);
	}

	public void moveBetweenBarAndLine(Bar bar, Line line, Map<Integer, Move> moves, boolean swapLeft) {
		// find dancers id in the bar
		Set<Integer> inBar = new HashSet<>(bar.getDancerId());
		if (inBar.size() > 80) {
			// System.out.println("Error: more than 80 people in the bar!");
			// System.out.println(inBar);
		}

		// find dancer id in the line
		Set<Integer> inLine = new HashSet<>(line.getDancerId());

		// find dancers in
		Set<Integer> toSwap = new HashSet<>();

		if (swapLeft) {
			// System.out.println("Swap only left column");
		} else {
			// System.out.println("Swap only right column");
		}

		for (int bid : inBar) {

			// find the point in bar
			Point barPos = bar.findPositionById(bid);
			int rowInbar = bar.findRowByPosition(barPos);

			// decide whether to swap based on bar - line alignment
			int column = bar.column(barPos);
			// left column
			if (swapLeft) {
				if (column == 0) {
					// System.out.println(bid + " in left column of left bar");
					toSwap.add(bid);
				} else if (column == 1)
					continue;
				else {
					// System.out.println("Error: " + barPos + " found by id " + bid + "is not in " + bar);
					return;
				}
			}
			// right column
			else {

				if (column == 1) {
					toSwap.add(bid);
					// System.out.println(bid + " in right column of right
					// bar");
				} else if (column == 0)
					continue;
				else {
					// System.out.println("Error: " + barPos + " found by id " + bid + "is not in " + bar);
					return;
				}
			}
		}
		// System.out.println(toSwap.size() + " nominated to swap: " + toSwap);

		// Swap action!
		int count = 0;
		for (Integer bid : toSwap) {

			// find the point in bar
			Point barPos = bar.findPositionById(bid);
			int rowInBar = bar.findRowByPosition(barPos);

			// find the partner in line to swap position
			int targetRowInLine = rowInBar * 5 + barLineOffset;

			// If the row exceeds the range of line
			int totalRows = line.spots.size() - 1;
			if (targetRowInLine > totalRows) {
				// System.out.println("Target row " + targetRowInLine + " exceeds " + line);
				continue;
			}

			// If the row can be found
			int lid = line.findIdByRow(targetRowInLine);

			// validate
			if (lid == -1) {
				// System.out.println("Error: Id in line is -1");
			}

			Point linePos = line.findPositionById(lid);

			/* Rewrite */

			// Calculate move of dancer in bar
			// Move barToLine = new Move(bid, linePos, line, targetRowInLine);

			// Update
			Move barToLine = new Move(bid, line, targetRowInLine);

			// Calculate move of dancer in line
			Point targetInBar;
			int targetColumn;
			// If add to left bar
			if (bar.id == 0) {
				targetInBar = new Point(barPos.x + HORIZONTAL_GAP, barPos.y);
				targetColumn = 1;
			}
			// add to right bar
			else if (bar.id == bars.size() - 1) {
				targetInBar = new Point(barPos.x - HORIZONTAL_GAP, barPos.y);
				targetColumn = 0;
			} else {
				// System.out.println("Error: What bar is it " + bar);
				return;
			}
			// Move lineToBar = new Move(lid, targetInBar, bar, rowInBar);

			// Update
			Move lineToBar = new Move(lid, bar, rowInBar, targetColumn);

			// Record the moves
			moves.put(bid, barToLine);
			moves.put(lid, lineToBar);
			count += 2;
		}

		// System.out.println("Finished movements from bar to line. " + count + " moves are added.");
		return;
	}

	public void debrief() {
		for (Line line : leftLines) {
			line.debrief();
		}
		for (Bar bar : bars) {
			// bar.debrief();
		}
		for (Line line : rightLines) {
			line.debrief();
		}
	}

	public boolean rightmostOfLeftLines(Line line) {
		int lineNum = leftLines.size();
		if (line.id == lineNum - 1)
			return true;
		return false;
	}

	public boolean leftmostOfRightLines(Line line) {
		int lineNum = leftLines.size();
		if (line.id == lineNum)
			return true;
		return false;
	}

	public Bar findNextBar(Bar bar) {
		/* Strategy specific */
		int nextBarId = bar.id + 1;
		if (nextBarId >= bars.size()) {
			// System.out.println("Error: Bar id " + nextBarId + " exceeds range");
			return null;
		}
		return bars.get(nextBarId);

	}

	public Bar findPrevBar(Bar bar) {
		/* Strategy specific */
		int prevBarId = bar.id - 1;
		if (prevBarId < 0) {
			// System.out.println("Error: Bar id " + prevBarId + " exceeds range");
			return null;
		}
		return bars.get(prevBarId);

	}

	public void moveWithinBars(Point[] dancers, Map<Integer, Move> moves) {
		/* Latest */
		for (int i = 0; i < dancers.length; i++) {
			Point p = dancers[i];
			Shape belong = mapping.get(i);

			// in the bars
			if (belong instanceof Bar) {
				Bar bar = (Bar) belong;

				// get current position of the dancer
				Point currentPos = bar.idToPosition.get(i);

				int column = bar.column(currentPos);
				int row = bar.findRowById(i);

				// Right column moves right
				if (column == 1) {
					if (bar.id == bars.size() - 1) {
						// System.out.println("Dancer " + i + " is in right
						// column of rightmost bar. Wait to exit.");
						continue;
					} else {
						if (moves.containsKey(i)) {
							// System.out.println("Error: Dancer " + i + " has decided to move " + moves.get(i));
						} else {
							Point dir = new Point(HORIZONTAL_GAP + BAR_GAP, 0);
							Point target = ToolBox.addTwoPoints(p, dir);

							// Record the move
							Bar nextBar = findNextBar(bar);
							if (nextBar == null) {
								// System.out.println("Error: Cannot find next bar for dancer " + i);
							} else {
								// Move moveRight = new Move(i, target, nextBar,
								// row);

								// Update
								Move moveRight = new Move(i, nextBar, row, column);

								moves.put(i, moveRight);
							}
						}
					}
				}
				// Left column moves left
				else if (column == 0) {
					// Left column of leftmost bar waits to exit in bar/line
					if (bar.id == 0) {
						// System.out.println("Dancer " + i + " is in left
						// column of leftmost bar. Wait to exit.");
						continue;
					} else {
						if (moves.containsKey(i)) {
							// System.out.println("Error: Dancer " + i + " has decided to move " + moves.get(i));
						} else {
							Point dir = new Point(-HORIZONTAL_GAP - BAR_GAP, 0);
							Point target = ToolBox.addTwoPoints(p, dir);

							// Record the move
							Bar prevBar = findPrevBar(bar);
							if (prevBar == null) {
								// System.out.println("Error: Cannot find previous bar for dancer " + i);
							} else {

								// Move moveLeft = new Move(i, target, prevBar,
								// row);

								// Update
								Move moveLeft = new Move(i, prevBar, row, column);

								moves.put(i, moveLeft);
							}

						}
					}
				}
				// Not in column
				else {
					// System.out.println("Error: Dancer " + i + "at " + currentPos + " is not in a bar");
				}

			}

		}
	}

	public int numberOfLinesEachSide(int total, int barNum) {
		int left = total - Player.BAR_MAX_VOLUME * barNum;
		// System.out.println(left + " people waiting in the lines");

		int eachLine = left / 2;
		int lineNum = (int) Math.ceil((eachLine + 0.0) / 200);
		// if (lineNum % 2 == 1)
		// lineNum++;
		return lineNum;
	}

	public int bruteForceNumberOfBars(int total) {
		// at most 20 bars in the map
		int bestBarNum = 0;
		for (int i = 20; i >= 0; i--) {
			// System.out.println("Try " + i + " bars");
			int barPop = 80 * i;
			int waiting = total - barPop;
			// estimate the number of lines
			int lineNum = (int) Math.ceil((waiting + 0.0) / 200);
			if (lineNum % 2 == 1)
				lineNum++;
			// System.out.println("There should be " + lineNum + " lines");

			// validate the allocation
			double barCoverage = (i + 1) * BAR_GAP + i * HORIZONTAL_GAP;
			// System.out.println(barCoverage + " reserved for bars");
			int lineNumOnSide = lineNum / 2;
			double lineCoverage = 2 * ((lineNumOnSide - 1) * LINE_GAP);
			// System.out.println(lineCoverage + " reserved for lines");

			double totalCoverage = barCoverage + lineCoverage;
			if (totalCoverage >= 20.0) {
				// System.out.println(i + " bars will take " + totalCoverage + ". Invalid.");
				continue;
			} else {
				// System.out.println(i + " bars take only " + totalCoverage + ". Go on to check line alignments. ");
				bestBarNum = i;
			}

			// If there's to be lines, check if the inner lines can be filled
			if (total > 1600) {
				// System.out.println("Total " + total + " people. Need to validate lines");
				int leftInLines = total - 80 * i;
				if (leftInLines < 400) {
					// System.out.println(i + " bars cannot ensure 2 filled lines. Invalid.");
					continue;
				} else {
					// System.out.println(i + " bars can ensure 2 filled inner lines. Valid.");
					bestBarNum = i;
					break;
				}
			} else {
				// System.out.println(total + " people. No need for lines.");
				break;
			}
		}

		if (bestBarNum == 0) {
			// System.out.println(total + " people cannot fit in the square?!");
			return -1;
		} else {
			return bestBarNum;
		}
	}

	public Map<Integer, Integer> distributePeopleToLines(int total, int numLines) {
		Map<Integer, Integer> mapping = new HashMap<>();
		int avg = total / numLines;
		// System.out.println(avg + "people to put in the line as base situation");
		if (avg > 200) {
			// System.out.println("Error: more than 200 people in one line");
		}

		int lineOnEachSide = numLines;
		int innerLines = 1;
		int outerLines = numLines - innerLines;

		// Find the inner line
		int innerLineIndex = numLines - 1;

		// if total is not enough to fill a line
		if (total <= 200) {
			mapping.put(innerLineIndex, total);

			// Outer lines
			for (int i = numLines - 1; i >= 0; i--) {
				if (i == innerLineIndex) {
					continue;
				} else {
					mapping.put(i, 0);
				}
			}

		} else {
			// put 200 people in the innermost line

			if (outerLines == 0) {
				// System.out.println("Just 1 inner line");
				avg = 0;
			} else {
				// System.out.println(outerLines + " outer lines");
				avg = (total - 200) / outerLines;
			}

			// System.out.println("Put " + avg + " people in outer lines");

			// Inner line first
			int notAssigned = total;
			// System.out.println("Line " + innerLineIndex + " will have " + 200);
			mapping.put(innerLineIndex, 200);
			notAssigned -= 200;

			// Outer lines
			for (int i = numLines - 1; i >= 0; i--) {

				if (i == innerLineIndex) {
					continue;
				} else {
					if (notAssigned >= 200) {
						// System.out.println("Line " + i + " will have " + 200);
						mapping.put(i, 200);
						notAssigned -= 200;
					} else {
						// System.out.println("Line " + i + " will have " + notAssigned);
						mapping.put(i, notAssigned);
						notAssigned = 0;
					}
				}
			}

		}

		// System.out.println("Population allocation for the lines is " + mapping);
		return mapping;
	}

	public int estimateResultAndBruteForceT(int numberOfBars, int numberOfLines) {
		int t;
		int x = numberOfBars;
		int k = numberOfLines;
		int maxScore = Integer.MIN_VALUE;
		int ourT = 0;
		for (t = 1; t <= 20; t++) {
			int thisSocre = (int) ((Math.floor(1800 / ((t + 1) * (x + 5 * k)))) * (t * x));
			if (thisSocre > maxScore) {
				ourT = t;
				maxScore = thisSocre;
			}
		}
		return ourT;
	}

}
