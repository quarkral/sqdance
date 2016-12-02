package sqdance.g7;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import sqdance.sim.Point;

public class Belt {
	int numDancers;
	Map<Integer,Point> indexToPositionSide;
	ArrayList<Dancer> dancerList;
	
	private Map<Integer,Integer> beltIndexToRowIndex;
	private Map<Integer,Integer> beltIndexToColumnIndex;
	private Map<Integer,Integer> beltIndexToDancerId = new HashMap<>();


	
	Point[][] tablePositions;
	Block[][] tableBlocks;
	int[][] dancersPerBlock;
	int[] dancersPerRow;
	
	boolean beltParity = false;
	
	private static int MaxNumPairRows = 22;
	private static int MaxNumCols = 39;
	private static int MaxPairNum = 2;
	private static int MaxContinuousDancers = 2;
	
	public int recommendedLowestDancerNumber = -1;
	
	public Belt(int numDancers){
		if(numDancers%2!=0)
			throw new RuntimeException("Number of Dancers must be even.");
		
		this.numDancers = numDancers;
		
		if(numDancers > 1716){
			initializeDancersPerBlock(numDancers);
			initializeTableBlocks();
		} 
		initializeTablePositions(numDancers);
		
		indexToPositionSide = new HashMap<Integer,Point>();
		beltIndexToRowIndex = new HashMap<Integer,Integer>();
		beltIndexToColumnIndex = new HashMap<Integer,Integer>();
		dancerList = new ArrayList<Dancer>();
		
		int numCols = MaxNumCols;
		int numRows;
		if(numDancers <= 1716){
			numRows = (numDancers/2)/numCols * 2;
			numRows += (numDancers%numCols==0)? 0 : 2;
			dancersPerRow = new int[44];
			for (int i=0 ; i<44 ; ++i)
				dancersPerRow[i] = MaxNumCols;
		}
		else
			numRows = 44;
		
		int row_index = 0;
		int col_index = -1;
		int counter = 0;
		int dancersOnRow = MaxNumCols;	// the number of dancers on current row
		boolean goingRight = true;
		boolean goingDown = true;

		for(int i=0; i<numDancers; i++, counter++){

			// reset the counter, directions and dancersOnRow
//			if (counter == dancersOnRow) {
			//System.out.println("DancersPerRow: " + dancersPerRow[row_index]);
			int threshold = numDancers <= 1716 ? dancersOnRow : dancersPerRow[row_index];
			if (counter == threshold) {
				//System.out.println("Inside if-statement");
				goingRight = !goingRight;
				if (row_index == numRows - 2) { // second last row
					row_index += 1;
				} else if (row_index == numRows -1) { // last row
					row_index -= 2;
					goingDown = !goingDown;
				} else {
					row_index += goingDown ? 2 : -2;
				}

				dancersOnRow = getNumOfCols(numDancers, numRows, row_index);
				//System.out.println(i + "---" + row_index + ":" + col_index);
				
				col_index = getNextColStartPos(goingRight, row_index, numRows, dancersOnRow);
				counter = 0;
			}

			col_index += goingRight ? 1 : -1;
			//System.out.println(i + "---" + row_index + ":" + col_index);
			indexToPositionSide.put(i, tablePositions[row_index][col_index]);
			beltIndexToRowIndex.put(i, row_index);
			beltIndexToColumnIndex.put(i, col_index);
		}
		
		for(int i=0; i<numDancers;i++){
			dancerList.add(new Dancer(i, i));
			beltIndexToDancerId.put(i,i);
		}
		
	}
	

	private int getNextColStartPos(boolean goingRight, int row_index, int numRows, int dancersOnRow) {
		if (row_index == numRows - 1) {
			//System.out.println("Hello:" + row_index);
			return goingRight ? dancersPerRow[row_index]-dancersOnRow-1 : dancersOnRow;
		} else {
			return goingRight ? -1 : dancersPerRow[row_index];
		}
	}
	private boolean isLastBelt(int row_index, int numRows) {
		return (row_index == numRows - 1) || (row_index == numRows - 2);
	}

	private int getNumOfCols(int numDancers, int numRows, int row_index) {
		if (!isLastBelt(row_index, numRows)) {
			return dancersPerRow[row_index];
		} else {
			int dancersBefore = 0;
			for(int i=0; i<numRows-2;i++) dancersBefore += dancersPerRow[i];
			//return (numDancers - (numRows-2)*MaxNumCols)/2;
			return (numDancers - dancersBefore)/2;
		}
	}
	

	public void initializeDancersPerBlock(int numDancers){
		dancersPerBlock = new int[44][39];
		dancersPerRow = new int[44];
		int numExtraDancers = (numDancers > 1716) ? numDancers - 1716: 0;
		//System.out.println("Number of ExtraDancers: " + numExtraDancers);
		
		int numNeededBlocks = (numExtraDancers%24==0) ? numExtraDancers/24 : numExtraDancers/24 + 1;

		int halfBlocksPerRow = (numNeededBlocks%88==0) ? numNeededBlocks/88 : numNeededBlocks/88 + 1;
		boolean overflow = (numNeededBlocks%88!=0);
		
		//filledBlocksPerRow = (numExtraDancers==0)? 0 : filledBlocksPerRow;


		int dancersInLastCol = (!overflow) ? 24 : (halfBlocksPerRow==1)? numExtraDancers : numExtraDancers%(24*88*(halfBlocksPerRow-1));
		int dancersInLastColBlock = (dancersInLastCol%88==0) ? dancersInLastCol/88 : dancersInLastCol/88 + 1;

		recommendedLowestDancerNumber = 0;//(halfBlocksPerRow-1==1) ? numDancers : (halfBlocksPerRow)*88*25;
		recommendedLowestDancerNumber = (numExtraDancers==0)? numDancers : recommendedLowestDancerNumber;
		
		for(int j=0; j<39; j++){
			//int dancerOnRowCount = 0;
			boolean fillBlock = j<halfBlocksPerRow || j>39-1-halfBlocksPerRow;
			boolean lastColumn = j==halfBlocksPerRow-1 || j==39-1;

			int fillAmount = (!lastColumn) ? 24 : dancersInLastColBlock;

			for(int i=0; i<44; i+=2){
				

				if(!fillBlock || numExtraDancers==0){
					dancersPerBlock[i][j]=1;
					dancersPerBlock[i+1][j]=1;
				}
				else{
					int tmp = lastColumn?dancersInLastColBlock : 24;
					if(numExtraDancers>tmp*2){ //48
						dancersPerBlock[i][j]=tmp+1; //25
						dancersPerBlock[i+1][j]=tmp+1; //25
						numExtraDancers -= tmp*2; //48
					}
					else{
						dancersPerBlock[i][j]=1 + numExtraDancers/2;
						dancersPerBlock[i+1][j]=1 + numExtraDancers/2;
						numExtraDancers = 0;
					}
				}

				dancersPerRow[i] += dancersPerBlock[i][j];
				dancersPerRow[i+1] += dancersPerBlock[i][j];

				//dancerOnRowCount+= dancersPerBlock[i][j];
				//System.out.println("DancersInBlock[" + i + "," + (i+1) + "][" + j + "]: " + dancersPerBlock[i][j]);
			}
			
			//System.out.println("DancersOn[" + i + "," + (i+1) + "]: " + dancerOnRowCount);
		}
		//for(int i=0;i<44;i++){
		//	System.out.println("Dancer per row: "+ dancersPerRow[i]);
		//}
		
	}
	
	public void initializeTableBlocks(){
		tableBlocks = new Block[44][39];
		for(int row=0; row<44; row++){
			for(int column=0; column<39; column++){
				double offSet = (0.01) * (row/2);
				Point startPoint = new Point(column*0.51 + (row%2 * 0.5001/2) + .01,(row+1)*0.5001*(Math.sqrt(3)/2) + offSet + .01);
				//Point startPoint = new Point(column*0.51,(row+1)*0.5001 + offSet);
				//System.out.println("Row[" + row + "] Col[" + column + "]: " + dancersPerBlock[row][column]);
				tableBlocks[row][column] = new Block(startPoint, dancersPerBlock[row][column], row%2==0);
			}
		}
	}

	public void initializeTablePositions(int numDancers){
		 if(numDancers <=1716)
		 	initializeTablePositionsSmall(numDancers);
		 else
			initializeTablePositionsBig(numDancers);					
	}
	
	public void initializeTablePositionsSmall(int numDancers){
		recommendedLowestDancerNumber = numDancers;
		tablePositions = new Point[44][39];
		for(int row=0; row<44; row++){
			for(int column=0; column<39; column++){
				double offSet = (0.01) * (row/2);
				tablePositions[row][column] = new Point(column*0.51 + (row%2 * 0.5001/2) + .01,(row+1)*0.5001*(Math.sqrt(3)/2) + offSet + .01);
			}
		}
	}
	
	public void initializeTablePositionsBig(int numDancers){
		if(numDancers < 1483)
			throw new RuntimeException("Wrong method. Call the small one instead.");
		
		tablePositions = new Point[44][39*25]; //This number will change to 2 later
		for(int row=0; row<44; row++){
			int colCount = 0;
			for(int column=0; column<39; column++){
				//System.out.println("Row[" + row + "] Col[" + column + "]");
				ArrayList<Point>BlockPosList = tableBlocks[row][column].getPositions();
				for(int i=0; i<BlockPosList.size();i++){
					tablePositions[row][colCount] = BlockPosList.get(i);
					colCount++;
				}
			}
		}
		
	}
	
	public Point getPosition(int i){
		return indexToPositionSide.get(i);
	}
	
	public Point[] spinBelt(Set<Integer> curDancers){
		Point[] instructions = new Point[numDancers];
		for (int i=0 ; i<numDancers ; ++i) {
			instructions[i] = new Point(0, 0);
		}
		

		/*for (Integer x : curDancers) {
			System.out.print("," + x);
		}

		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println();*/
		
		for(int i=0; i<numDancers; i++){
			Dancer oldDancer = dancerList.get(i);
			if (curDancers.contains(oldDancer.dancerId)){
				continue;
			} 
//			Dancer newDancer = dancerList.get((i+1)%numDancers);
			int oldBeltIndex = oldDancer.beltIndex; 
			int newBeltIndex = (oldBeltIndex + 1)%numDancers;

			while(curDancers.contains(beltIndexToDancer(newBeltIndex))){
				newBeltIndex = (newBeltIndex+1)%numDancers;
			}

			Point oldPos = getPosition(oldBeltIndex);
			Point newPos = getPosition(newBeltIndex);
			//oldDancer.beltIndex = newBeltIndex;
			//System.out.println("HHHHH");
			Point instruct = new Point(newPos.x-oldPos.x, newPos.y-oldPos.y); 
			instructions[i] = instruct;
		}

		for(int i=0; i<numDancers ;i++){
			Dancer oldDancer = dancerList.get(i);
//			Dancer newDancer = dancerList.get((i+1)%numDancers);
			if (curDancers.contains(oldDancer.dancerId)) {
				continue;
			} 
			int oldBeltIndex = oldDancer.beltIndex; 
//			int newBeltIndex = newDancer.beltIndex;
			int newBeltIndex = (oldBeltIndex + 1)%numDancers;
			while(curDancers.contains(beltIndexToDancer(newBeltIndex))){
				newBeltIndex = (newBeltIndex+1)%numDancers;
			}
			
//			newBeltIndex -= (i==numDancers-1) ? 1 : 0;
			
			oldDancer.beltIndex = newBeltIndex; 
			beltIndexToDancerId.put(oldDancer.beltIndex, oldDancer.dancerId);
		}

		//System.out.println(instructions[0])
		return instructions;
	}
	
	public int[] CoordinatePosition(int beltIndex){
		return new int[]{beltIndexToRowIndex.get(beltIndex),beltIndexToColumnIndex.get(beltIndex)};
	}
	
	public int getPartnerBeltIndex(int beltIndex){
		return numDancers-1-beltIndex;
	}
	
	//ToDo: Easy to improve performance here but we're lazy and this is good enough.
	public int beltIndexToDancer(int beltIndex){
		return beltIndexToDancerId.get(beltIndex);
	}

	/*public int beltIndexToDancer_debug(int beltIndex){
		for (Dancer d : dancerList){
			if (d.beltIndex == beltIndex) {
				if (beltIndexToDancer(beltIndex) != d.dancerId) {
					System.out.println("beltInde : " + beltIndex);
					System.out.println("dancerId(corrent) : " + d.dancerId);
					System.out.println("dancerId(wrong) : " + beltIndexToDancer(beltIndex));
				}
				return d.dancerId;
			}
		}
		return -1;

	}*/

	public int getPartnerDancerID(int dancerId) {
		int beltIndex = dancerList.get(dancerId).beltIndex;
		int partnerBeltIndex = getPartnerBeltIndex(beltIndex);
		return beltIndexToDancer(partnerBeltIndex);
	}



	public Set<Integer> verifyDancer(Set<Dancer> curDancers) {
		Set<Integer> validDancers = new HashSet<>();
		if (curDancers.size() == numDancers) { 
			for (Dancer d : curDancers) {
				validDancers.add(d.dancerId);
			}
			return validDancers;
		}
		
		//int[][] dancerMatrix = new int[MaxNumPairRows*2][MaxNumCols];
		//ToDo: (From David)Verify what I did here is correct
		//I didn't write this method, I just patched it up to try and make it work
		int[][] dancerMatrix = new int[44][39*25];
		Map<String, Integer> posToDancerID = new HashMap<>();

		for (Dancer d : curDancers) {

			int[] pos = CoordinatePosition(d.beltIndex);
			int row_index = pos[0];
			int col_index = pos[1];

			dancerMatrix[row_index][col_index] = 1;
			String key = row_index + "#" + col_index;
			posToDancerID.put(key, d.dancerId);
			validDancers.add(d.dancerId);

		}
		
		for (int i=0 ; i<dancerMatrix.length ; ++i) {
			int continuousDancers = 0;
			for (int j=0 ; j<dancerMatrix[0].length ; ++j) {
				if (dancerMatrix[i][j] == 1) {
					++continuousDancers;
				} else {
					continuousDancers = 0;
				}

				if (continuousDancers > MaxContinuousDancers) {
					String key = i + "#" + j;
					int dancerId = posToDancerID.get(i + "#" + j);
					validDancers.remove(dancerId);
					changeDancerStatus(dancerId, Dancer.WILL_MOVE);

					dancerMatrix[i][j] = 0;
					continuousDancers = 0;
				}

			}
				
		}
		
		return validDancers;

	}

	private void printMatrix(int[][] dancerMatrix) {
		for (int i=0 ; i<dancerMatrix.length ; ++i) {
			for (int j=0 ; j<dancerMatrix[0].length ; ++j)
				System.out.print(dancerMatrix[i][j]);
			System.out.println();
		}

		System.out.println();
		System.out.println();
	}
	
	public void changeDancerStatus(int dancerId, int status) {
		this.dancerList.get(dancerId).dancerStatus = status;
	}
	
}
 