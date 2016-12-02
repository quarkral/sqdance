package sqdance.g2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import sqdance.sim.Point;

public class RegionsStrategy implements Strategy {
	
	private static final double EPSILON = 0.0000001;
	boolean not_est = true;
	private static final int DANCERS_IN_A_LINE = 40;
	private static final int IDLERS_IN_A_LINE = 200;
	private  double SCORE_RATIO = 0.95;
	private static final double root3 = Math.sqrt(3);
	
	//turns to dance with stranger, dont change.
	private static final int STRANGER_DANCE_TURNS = 20;
	private static final double DISTANCE_BETWEEN_DANCERS = 0.500001;
	private static final double DANCER_LINE_GAP = root3 / 2 * DISTANCE_BETWEEN_DANCERS;
	private static final double DISTANCE_BETWEEN_IDLERS = 0.1;
	private static final double IDLER_LINE_GAP = root3 / 2 * DISTANCE_BETWEEN_IDLERS;
	
	Vector[] region1;
	int region1Size;
	Vector[] region2;
	Vector[] region3;

	int d;
	
	DancingTime dankDancers;
	
	HashMap<Integer, Integer> local_ids, actual_ids;
	HashMap<Integer, Integer> region1Dancers, region2Dancers, region3Dancers;
	
	// Uses global IDs
	HashMap<Integer, Location> dancer_locations;

	int batch_size;
	int num_batches;
	int target_score;
	int num_dancing_cols;


	// YOGESH ZONE STARTS

    // Target position for each dancer, null if not in moving phase
    private Vector[] move_targets;
    private static double MOVE_DIST = 2.0;
    private static double MOVE_EPSILON = 0.1;


    // Uses dancer_locations and move_targets to come up with direction to move in each step
    private Vector[] generateMoveInstructions() {
        if (move_targets == null) {
            return null;
        }
        Random r = new Random();

        // Move to the target locations as fast as possible
        Vector[] instructions = new Vector[d];
        Vector difference;
        double norm;
        for (int i = 0; i < d; ++i) {
        	difference = move_targets[i].add( dancer_locations.get(i).getVector().multiply(-1) );
        	// if(i==1160) {
        	// 	System.out.println(difference);
        	// }
        	norm = difference.norm();


            if (norm - MOVE_DIST > MOVE_EPSILON) {
            	// difference.x += 0.1 * r.nextDouble();
            	// difference.y += 0.1 * r.nextDouble();
            	// if( Math.abs(difference.x) > Math.abs(difference.y) ) {
            	// 	difference.y = 0;
            	// } else {
            	// 	difference.x = 0;
            	// }
            }


        	// if(i==1160) {
        	// 	System.out.println(difference);
        	// }
            difference = difference.getLengthLimitedVector(MOVE_DIST - MOVE_EPSILON);
        	// if(i==1160) {
        	// 	System.out.println(difference);
        	// }
            instructions[i] = difference;
        }

        // for (int i = 0; i < d; i+=100) {
        // 	System.out.print(move_targets[i]);
        // 	System.out.print("\t");
        // }
        // System.out.println();
        // for (int i = 0; i < d; i+=100) {
        // 	System.out.print(dancer_locations.get(i).getVector());
        // 	System.out.print("\t");
        // }
        // System.out.println();
        // System.out.println();
        for(int i=0; i<d; ++i) {
        	// if(i==1160) {
        	// 	System.out.println(instructions[i] + " " + dancer_locations.get(i).getVector());
        	// }
        	dancer_locations.get(i).getVector().x += instructions[i].x;
        	dancer_locations.get(i).getVector().y += instructions[i].y;
        	// if(i==1160) {
        	// 	System.out.println(instructions[i] + " " + dancer_locations.get(i).getVector());
        	// }
        }
        return instructions;
    }

    // If all dancers have reached target, return true Otherwise, return false
    private boolean isMovementComplete() {
    	// System.out.print("isMovementComplete:");
		if(move_targets!=null) {
	        for (int i = 0; i < d; ++i) {
	            if (	dancer_locations.get(i).getVector().x != move_targets[i].x
	            	||	dancer_locations.get(i).getVector().y != move_targets[i].y ) {
			        // System.out.println("false");
	                return false;
	            }
	        }
	        move_targets=null;
        }
        // System.out.println("true");
        return true;
    }

    // Set move target to the corresponding Point in the next tile
    private int region3size = 0;

    private void setMoveTargets() {
        move_targets = new Vector[d];
        int d1_id, d2_id, d3_id;
        for(int i=0; i<d; ++i) {
        	move_targets[i] = dancer_locations.get(i).getVector();
        }
        double curr_x = 0.0;
        double x_max = 0.0;
        double x_min = 20.0;
        HashSet<Integer> move_target_ids_shifted = new HashSet<Integer>();
        int migration_size = batch_size;
        /*if( region1Size < 2*batch_size ) {
        	System.out.println("Migrating half region");
        	migration_size = (batch_size + d%batch_size)/2;
        }*/
        migration_size = Math.min(batch_size, region1Size);
        if (region1Size==0) {
        	System.out.println("No need to migrate");
        	migration_size = 0;
        	return;
        }
        for(int i=0; i<migration_size; ++i) {
        	try {
	        	d1_id = region1Dancers.get(region1Size-1);
	        	d2_id = region2Dancers.get(i);
	        	// d3_id = region3Dancers.get(i)
	        	curr_x = dancer_locations.get(d1_id).getVector().x;
	        	x_max = Math.max(x_max, curr_x);
	        	x_min = Math.min(x_min, curr_x);
	        	move_targets[d1_id] = new Vector( dancer_locations.get(d2_id).getVector());
	        	move_target_ids_shifted.add(d1_id); // slightly left of this
	        	move_targets[d2_id] = new Vector( region3[region3size]);
	        	region3Dancers.put(region3size, d2_id);
	        	region2Dancers.remove(i);
	        	region2Dancers.put(i, d1_id);
	        	region1Dancers.remove(region1Size);
	        	--region1Size;
	        	++region3size;
        	} catch (Exception e) {
        		System.out.println("Exception in migration");
        		break;
        	}
        }
        for(int i=migration_size; i<batch_size; ++i) {
        	// System.out.print(i+" ");
        	d2_id = region2Dancers.get(i);
        	move_target_ids_shifted.add(d2_id);
        	move_targets[d2_id] = new Vector( move_targets[d2_id] );
        }
        // System.out.println();
    	double x_shift = x_min - x_max;
    	System.out.println(x_shift);
    	for(int id : move_target_ids_shifted) {
    		// if(id==1160) {
    		// 	System.out.println(move_targets[id]);
    		// }
    		move_targets[id].x += x_shift;
    		// if(id==1160) {
    		// 	System.out.println(move_targets[id]);
    		// }
    	}
    	return;
    }

	// YOGESH ZONE ENDS


	@Override
	public Point[] generate_starting_locations(int d) {

		this.d = d;
		SCORE_RATIO = 1;
		if ( d >= 7000) SCORE_RATIO = 0.95;
		if ( d >= 10001) SCORE_RATIO = 0.9;
		
		Vector[] locations = new Vector[d];
		dancer_locations = new HashMap<>();
		region1Dancers = new HashMap<>();
		region2Dancers = new HashMap<>();
		region3Dancers = new HashMap<>();
		
		/*
		 *  Calculate batch size and the target score
		 */
		

		int num_dancing_cols = (int) Math.ceil((18.8 - root3*DISTANCE_BETWEEN_IDLERS*d/400)
											   / (root3*DISTANCE_BETWEEN_DANCERS/2 - root3*DISTANCE_BETWEEN_IDLERS/10));
		if( 1==(num_dancing_cols%2) ) {
			num_dancing_cols-=1;
		}
		batch_size = 40 * num_dancing_cols;
		num_batches = (int)Math.ceil(d * 1.0 / batch_size);
		target_score = 500;
		
		int dancers_in_round_1 = batch_size;
//		if (num_batches == 2) {
//			dancers_in_round_1 = d % 4 == 0 ? d/2 : d/2 + 1;
//			dancers_in_round_1 = ((int)Math.ceil(dancers_in_round_1/80))*80;
//		}
		
		/*
		 * Make regions
		 */
		region1 = new Vector[d];						// for all players
		region2 = new Vector[batch_size];				// for only so many players
		region3 = new Vector[d];						// for all players
		
		Vector current;
		
		// Region 1
		current = new Vector(0, 0);
		for (int i = 0; i < d; ++i) {
			region1[i] = new Vector(current);
			
			int next = i + 1;
			if (next % IDLERS_IN_A_LINE == 0) {
				current.x += IDLER_LINE_GAP + EPSILON;
				current.y = next % (2*IDLERS_IN_A_LINE) == 0 ? 0 : DISTANCE_BETWEEN_IDLERS / 2;
			} else {
				current.y += DISTANCE_BETWEEN_IDLERS + EPSILON;
			}
		}
		
		double maxX = 0;
		for (int i = dancers_in_round_1; i < d; ++i) {
			locations[i] = region1[i - dancers_in_round_1];
			dancer_locations.put(i, new Location(region1, i - dancers_in_round_1));
			++region1Size;

			if (maxX < locations[i].x) {
				maxX = locations[i].x;
			}
			region1Dancers.put(i - dancers_in_round_1, i);
		}
		
		// Region 2
		current = new Vector(maxX + 0.6, EPSILON);
		for (int i = 0; i < batch_size; ++i) {
			region2[i] = new Vector(current);
			
			int next = i + 1;
			if (next % DANCERS_IN_A_LINE == 0) {
				current.x += DANCER_LINE_GAP;
				if (next % (2*DANCERS_IN_A_LINE) == 0) current.x += EPSILON;
				current.y = next % (2*DANCERS_IN_A_LINE) == 0 ? EPSILON : DISTANCE_BETWEEN_DANCERS / 2;
			} else {
				current.y += DISTANCE_BETWEEN_DANCERS + EPSILON;
			}
			
			// Splitting last column if there are odd number of columns
			if (num_dancing_cols % 2 == 1 && next == batch_size - DANCERS_IN_A_LINE/2) {
				current.x += DANCER_LINE_GAP;
				current.y = DISTANCE_BETWEEN_DANCERS / 2;
			}
		}
		
		for (int i = 0; i < dancers_in_round_1; ++i) {
			locations[i] = region2[i];
			dancer_locations.put(i, new Location(region2, i));
			region2Dancers.put(i, i);
		}
		
		// Region 3
		current = new Vector(20, 0);
		for (int i = 0; i < d; ++i) {
			region3[i] = new Vector(current);
			
			int next = i + 1;
			if (next % IDLERS_IN_A_LINE == 0) {
				current.x -= IDLER_LINE_GAP + EPSILON;
				current.y = next % (2*IDLERS_IN_A_LINE) == 0 ? EPSILON : DISTANCE_BETWEEN_IDLERS / 2;
			} else {
				current.y += DISTANCE_BETWEEN_IDLERS + EPSILON;
			}
		}
		
		if (num_dancing_cols % 2 == 1) num_dancing_cols++;
		
		return Vector.getPoints(locations);
	}

	int turnnum = 0;
	
	void est_f_and_upd_sc(int[] enjg) {
		if(true) return;
		int f = 0;
		for(int i = 0 ; i < region2Dancers.size(); ++i) {
			int id = region2Dancers.get(i);
			if(enjg[id] > 3) ++f;
		}
		double f_f = f * 1.0 / region2Dancers.size();
		this.target_score = (int)((1800.0/num_batches - 9) * (20*(3.0 + f_f/2)/21));
		System.out.println("new score " + this.target_score);
	}
	@Override
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained, int[] soulmate,
			int current_turn, int[][] remainingEnjoyment) {
		int d = dancers.length;
		Point[] instructions = new Point[d];
		for(int i = 0 ; i < d ;++i) 
			instructions[i] = new Point(0,0);
		if (current_turn == 2) {
			this.target_score = (int)((1800.0/num_batches - 9) * (20*(3.0)/21)*SCORE_RATIO);
			System.out.println(num_batches + " ** " +this.target_score);
//			this.target_score = 20;
		}
		
		if (isMovementComplete()) {
			if (target_score_reached(scores)) {
				setMoveTargets();
				return Vector.getPoints(generateMoveInstructions());
			} else {
				if(turnnum < STRANGER_DANCE_TURNS) {
					
					++ turnnum;
					if(turnnum == 3 && not_est) {
						est_f_and_upd_sc(enjoyment_gained);
						not_est = false;
					}
				} else {
					turnnum = 0;
					dance(instructions);
				}
				
			}
		} else {
			return Vector.getPoints(generateMoveInstructions());
		}
		return instructions;
	}
	Point getDirection(Point a, Point b) {
		return new Point(b.x - a.x, b.y - a.y);
	}
	private boolean target_score_reached(int[] scores) {
		for(int i = 0 ; i < region2Dancers.size(); ++ i) {
			int k = region2Dancers.get(i);
			if (scores[k] < target_score) {
				return false;
			}
		}
		return true;
	}
	
	//assumptions : even dancers per column
	// at least 2 columns
	void dance(Point[] instructions) {
		int d = region2Dancers.size();
		int[] newregion2dancers = new int[d];
		int[] ids = new int[d];
		System.out.println(d);
		//int current = 0;
		int maxcol = d/(2*DANCERS_IN_A_LINE);
		for(int i = 0 ; i < region2Dancers.size(); ++i) {
			int next = -1;
			int col = i / (2 *DANCERS_IN_A_LINE);
			int row = i % (2*DANCERS_IN_A_LINE);
			int col2 = 0;
			if(row >= DANCERS_IN_A_LINE) {
				row -= DANCERS_IN_A_LINE;
				col2 = 1;
			}
			if(col2 == col % 2 && row != DANCERS_IN_A_LINE - 1) {
				next = i + 1;
			} else if(col2 == col % 2) {
				if(col2 == 0) {
					if(col == maxcol-1) {
						next = i + DANCERS_IN_A_LINE;
					} else {
						next = i + 2 * DANCERS_IN_A_LINE;
					}
				} else {
					next = i - 2 * DANCERS_IN_A_LINE;
				}
			} else if(col2 != col % 2 && row != 0) {
				next = i - 1;
			} else if(col2 != col % 2) {
				if(col2 == 0) {
					if(col == maxcol-1) {
						next = i + DANCERS_IN_A_LINE;
					} else {
						next = i + 2 * DANCERS_IN_A_LINE;
					}
				} else {
					if(col == 0) {
						next = i - DANCERS_IN_A_LINE;
					} else {
						next = i - 2 * DANCERS_IN_A_LINE;
					}
				}
			}
			/*if(col == d / (2*DANCERS_IN_A_LINE)) {
				int num = d % (2 * DANCERS_IN_A_LINE);
				int row = i % (2 * DANCERS_IN_A_LINE);
				int col2 = 0;
				if(row >= num/2) {
					row -= num/2;
					col2 = 1;
				}
				if(col2 == 1 && row == 0) {
					next = current - 2 * DANCERS_IN_A_LINE;
				} else if(col2 == 1) {
					next = current - 1;
				} else if(col2 == 0 && row == num/2 - 1) {
					next = current + DANCERS_IN_A_LINE;
				} else if(col2 == 0) {
					next = current + 1;
				}
			} else {
				int row = i % (2*DANCERS_IN_A_LINE);
				int col2 = 0;
				if(row >= DANCERS_IN_A_LINE/2) {
					row -= DANCERS_IN_A_LINE/2;
					col2 = 1;				
				}
				int maxcol = d/(2*DANCERS_IN_A_LINE);
				int dir = -1;
				if(col % 2 == maxcol % 2) {
					dir = 1;
				}	
				if(col2 == 0 && ) {
					next = current - 2 * DANCERS_IN_A_LINE;
				} else if(col2 == maxcol % 2) {
					next = current - 1;
				} else if(col2 != maxcol % 2 && row == num/2 - 1) {
					next = current + DANCERS_IN_A_LINE;
				} else if(col2 != maxcol % 2) {
					next = current + 1;
				}
			}*/
			int id = region2Dancers.get(i);
			ids[i] = id;
			Point location = dancer_locations.get(id).getVector().getPoint();
			
			//System.out.println(i + " -> " + next);
			int id2 = region2Dancers.get(next);
			Point location2 = dancer_locations.get(id2).getVector().getPoint();
			instructions[id] = getDirection(location, location2);
			newregion2dancers[next] = id;
			//current = next;
		}
		
		//r2d
		for(int i = 0 ; i < region2Dancers.size(); ++i) {
			region2Dancers.put(i, newregion2dancers[i]);
		}
		//dlocs
		for(int i = 0 ; i < region2Dancers.size(); ++i) {
			int id = ids[i];
			dancer_locations.get(id).getVector().x += instructions[id].x;
			dancer_locations.get(id).getVector().y += instructions[id].y;
		}
	}
}

// Has an array reference and the index of a point into it
class Location {
	Vector[] region;
	int index;
	
	public Location(Vector[] region, int index) {
		this.region = region;
		this.index = index;
	}
	
	public Vector getVector() {
		return this.region[this.index];
	}
}
