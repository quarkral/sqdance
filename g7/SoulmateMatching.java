package sqdance.g7;

import sqdance.sim.Point;

import java.io.*;
import java.util.*;
import java.lang.System.*;

public class SoulmateMatching implements sqdance.sim.Player {

	private static double eps = 1e-7;
	private static double delta = 1e-2;

	private double minDis = 0.5;
	private double maxDis = 2.0;
	private double safeDis = 0.1;
	private int[] scorePround = {0, 6, 4, 3}; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger
	private int boredTime = 6; // 6 seconds

	private int d = -1;
	private int room_side = -1;

	private int[][] relation; // kind of relation: 1 for soulmate, 2 for friend, 3 for stranger, initialize to -1
	private int[][] danced; // cumulatived time in seconds for dance together
	private int couples_found = 0;
	private int stay = 0;
	private boolean single_all_the_way = false;
	private int normal_limit = 1600 - 304;
	private int single_limit = 1000;

	public class Dancer{
		int id = -1;
		int soulmate = -1;
		Point next_pos = null;
		Point des_pos = null;
		int pit_id = -1;
		Point cur_pos = null;
		public Dancer(int id,int pit_id){
			this.id = id;
			this.pit_id = pit_id;
		}
	}

	//dancers never stay at pit, legal positions are up/down/left/right this.delta/3;
	public class Pit{
		Point pos = null;
		int pit_id = -1;

		public Pit(int pit_id,Point pos){
			this.pos = new Point(pos.x,pos.y);
			this.pit_id = pit_id;
		}
	}

	private Dancer[] dancers;
	private boolean connected;
	private Point[] starting_positions;
	private Point[] last_positions;
	private Point[] stay_and_dance;
	private Pit[] pits;
	private int state; // 1 represents 0-1 2-3 4-5, 2 represents 0 1-2 3-4 5
	//====================== end =========================

	//============= parameter for dance in turn strategy ========================
	private int numDancer = -1;
	private int roomSide = -1;

	private int numRowAuditoriumBlock = 10;

	private int[] sequence;
	private Point[] position;

	private int timeStamp = 0;

	List<Point> occupiedPos = new ArrayList<>();


	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = room_side;
		init_normal();
	}

	public Point[] generate_starting_locations() {
		return generate_starting_locations_normal();
	}

	public Point[] play(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		return play_normal(old_positions, scores, partner_ids, enjoyment_gained);
	}

	// =================== strategy when d is not so large ===================

	private void init_normal() {
		//data structure initialization
		this.single_all_the_way = this.d > this.single_limit;
		//data structure initialization

		this.relation = new int[d][d];
		this.danced = new int[d][d];
		for (int i = 0; i < d; ++i){
			for (int j = 0; j < d; ++j) {
				relation[i][j] = -1;
			}
		}


		this.connected = true;
		this.pits = new Pit[normal_limit];
		this.dancers = new Dancer[d];
		this.stay_and_dance = new Point[d];

		for(int i = 0; i < d; i++){
			this.stay_and_dance[i] = new Point(0,0);
		} 


		
		double increment = 0.5 + this.delta;
		double border = 2*increment+this.delta;
		double x = this.delta + border;
		double y = this.delta + border;

		int i = 0;
		int old_i = -1;
		int sign = 1;

		double x_min = this.delta - safeDis + border;
		double x_max = this.room_side + safeDis - border;
		double y_min = this.delta + border;
		double y_max = this.room_side + safeDis - border;

		//create the pits in a spiral fashion
		while(old_i != i){
			//go right
			old_i = i;
			while(x + safeDis < x_max){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				x += increment;
			}
			x = this.pits[i-1].pos.x;
			y += increment;
			x_max = x;

			//go down
			while(y + safeDis < y_max){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				y += increment;
			}
			y = this.pits[i-1].pos.y; 
			x -= increment;
			y_max = y;

			//go left
			while(x - safeDis > x_min){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				x -= increment;
			}
			x = this.pits[i-1].pos.x; 
			y -= increment;
			x_min = x;

			//go up
			while(y - safeDis > y_min){
				this.pits[i] = new Pit(i,new Point(x,y));
				i++;
				y -= increment;

			}
			y = this.pits[i-1].pos.y;
			x += increment;
			y_min = y;
		}

		//put players in pits

		for(int j = 0; j < d; j++){
			this.dancers[j] = new Dancer(j,j);
			Point my_pos = this.pits[j].pos;
			Point partner_pos = j%2 == 0? getNext(this.pits[j]).pos : getPrev(this.pits[j]).pos;
			if(j==0)
				this.dancers[0].cur_pos = new Point(this.delta+border,this.delta+border);
			this.dancers[j].cur_pos = this.dancers[j].next_pos;
			this.dancers[j].next_pos = findNearestActualPoint(my_pos,partner_pos);
		}
		this.state = 2;

		if(single_all_the_way) this.boredTime = 120;
	}

	public Point[] generate_starting_locations_normal() {
		this.starting_positions = new Point[this.d];
		for(int j = 0; j < d; j++){
			this.starting_positions[j] = this.dancers[j].next_pos;
		}
		return this.starting_positions;
	}

	public Point[] play_normal(Point[] old_positions, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		updatePartnerInfo(partner_ids,enjoyment_gained);
		this.last_positions = old_positions;

		move_couple();
		if(!this.connected) {
			connect();
		}
		else{
			if(stay < boredTime){
				this.stay += 6;
				return stay_and_dance;
			}
			else{
				swap();
			}
		}
			
		//generate instructions using target positions and current positions
		return generateInstructions();
	}

	//update dancer relations based on enjoyment gained;
	//also arrange couple's destination honeymoon pit number, set them close to each other 
	void updatePartnerInfo(int[] partner_ids, int[] enjoyment_gained) {
		if(single_all_the_way) return;
		int new_couples = 0;
		for(int i = 0; i < d; i++){
			if(enjoyment_gained[i] == 6){
				if(relation[i][partner_ids[i]] != 1) {
					//arrange destination for newly found couples	
					int rem_dancers = this.d - this.couples_found;
					Point myPos = dancers[i].cur_pos;
					Point parPos = dancers[partner_ids[i]].cur_pos;
					if (rem_dancers <= 60 && myPos!=null && parPos!=null) {
						//only one layer left
						double deltaX;
						double deltaY;

						boolean vertical = false;
						if (myPos.x==parPos.x) {
							//l ,r 
							deltaX = 0.52;
							deltaY = 0.52;

						} else {
							deltaY = 0.52;
							deltaX = 0.52;
							vertical = true;
						}

						boolean found = false;
						for (int m = -3;  m < 4 ; ++m) {
							for (int n=-3 ; n < 4 ; ++n) {
								if (m*deltaX*m*deltaX + n*deltaY*n*deltaY > 4)
									continue;

								double mX = myPos.x + m*deltaX;
								double mY = myPos.y + n*deltaY;

								double pX = parPos.x + m*deltaX;
								double pY = parPos.y + n*deltaY;

								if (mX < 0 || mX > this.room_side || mY < 0 || mY > this.room_side) {
									continue;
								}

								if (pX < 0 || pX > this.room_side || pY < 0 || pY > this.room_side) {
									continue;
								}
					

								if (Math.abs(mX - 1.04) < 0.52 || Math.abs(mX - (this.room_side - 1.04) ) < 0.52 ||
								 Math.abs(mY - 1.04) < 0.52 || Math.abs(mY - (this.room_side - 1.04) ) < 0.52) {
									continue;
								} 

								if (Math.abs(pX - 1.04) < 0.52 || Math.abs(pX - (this.room_side - 1.04) ) < 0.52 ||
								 Math.abs(pY - 1.04) < 0.52 || Math.abs(pY - (this.room_side - 1.04) ) < 0.52) {
									continue;
								} 
								boolean allPointLarge = true;
								for(Point p:occupiedPos){
									if((mX-p.x)*(mX-p.x) + (mY-p.y)*(mY-p.y) < 0.52*0.52)
										allPointLarge = false;
									if((pX-p.x)*(pX-p.x) + (pY-p.y)*(pY-p.y) < 0.52*0.52)
										allPointLarge = false;
									if(!allPointLarge) break;
								}
								if(!allPointLarge){
									continue;
								}
								found = true;
								occupiedPos.add(new Point(mX,mY));
								occupiedPos.add(new Point(pX,pY));
								dancers[i].des_pos = new Point(mX,mY);
								dancers[partner_ids[i]].des_pos = new Point(pX,pY);
								break;
							}
							if(found) break;
						}
						if(!found){
							Point des1 = this.pits[this.pits.length - 1 - this.couples_found].pos;
							Point des2 = this.pits[this.pits.length - 2 - this.couples_found].pos;
							dancers[i].des_pos = findNearestActualPoint(des1,des2);
							dancers[partner_ids[i]].des_pos = findNearestActualPoint(des2,des1);
						}
						dancers[i].pit_id = this.pits.length - 1 - this.couples_found;
						dancers[partner_ids[i]].pit_id = this.pits.length - 2 - this.couples_found;
					}else{
						Point des1 = this.pits[this.pits.length - 1 - this.couples_found].pos;
						Point des2 = this.pits[this.pits.length - 2 - this.couples_found].pos;
						dancers[i].des_pos = findNearestActualPoint(des1,des2);
						dancers[i].pit_id = this.pits.length - 1 - this.couples_found;
						dancers[partner_ids[i]].des_pos = findNearestActualPoint(des2,des1);
						dancers[partner_ids[i]].pit_id = this.pits.length - 2 - this.couples_found;
						if(rem_dancers < this.d/2){
							occupiedPos.add(dancers[i].des_pos);
							occupiedPos.add(dancers[partner_ids[i]].des_pos);
						}
					}
					this.connected = false;
					this.couples_found += 2;
					new_couples += 2;
					
				}
				relation[i][partner_ids[i]] = 1;
				relation[partner_ids[i]][i] = 1;
				dancers[i].soulmate = partner_ids[i];
			}
			else if(enjoyment_gained[i] == 4){
				relation[i][partner_ids[i]] = 2;
			}
			else if(enjoyment_gained[i] == 3){
				relation[i][partner_ids[i]] = 3;
			}
			danced[i][partner_ids[i]] += 6;
		}
		//if(new_couples != 0) System.out.println("new couples: " + new_couples);
	}

	// a = (x,y) we want to find least distance between (x+this.delta/3, y) (x-this.delta/3, y) (x, y+this.delta/3) (x, y-this.delta/3) and b
	Point findNearestActualPoint(Point a, Point b) {
		Point left = new Point(a.x-this.delta/3,a.y);
		Point right = new Point(a.x+this.delta/3,a.y);
		Point down = new Point(a.x,a.y-this.delta/3);
		Point up = new Point(a.x,a.y+this.delta/3);
		Point a_neighbor = left;
		if (distance(right,b) < distance(a_neighbor,b)) a_neighbor = right;
		if (distance(down,b) < distance(a_neighbor,b)) a_neighbor = down;
		if (distance(up,b) < distance(a_neighbor,b)) a_neighbor = up;

		return a_neighbor;
	}

	int findDancer(int pit_id){
		for(int i = d-1; i >=0; i--){
			if(this.dancers[i].pit_id == pit_id) return i;
		}
		return -1;
	}

	int getNextPit(int pit_id){
		int remain_singles = this.d - this.couples_found;
		if (pit_id%2 == 0 && this.state == 1 || pit_id%2 == 1 && this.state == 2){
			if(pit_id == remain_singles -1) return -1;
			return pit_id + 1;
		}
		if(pit_id == 0) return -1;
		return pit_id -1;
	}

	//modify the desination positions of active dancers;
	void swap() {
		int remain_singles = this.d - this.couples_found;
		for (int pit_id = 0; pit_id < remain_singles; pit_id++) {
			int dancer_id = findDancer(pit_id);
			if(dancers[dancer_id].soulmate != -1) continue;
			int next_pit = getNextPit(pit_id);
			if(next_pit == -1){
				this.dancers[dancer_id].cur_pos = this.dancers[dancer_id].next_pos;
				dancers[dancer_id].next_pos = new Point(pits[pit_id].pos.x, pits[pit_id].pos.y);
				dancers[dancer_id].pit_id = pit_id;
			}
			else{
				this.dancers[dancer_id].cur_pos = this.dancers[dancer_id].next_pos;
				dancers[dancer_id].next_pos = findNearestActualPoint(pits[next_pit].pos, pits[pit_id].pos);
				dancers[dancer_id].pit_id = next_pit;
			}
		}
 		this.state = 3 - this.state;
 		this.stay = 0;
	}


	Pit getPrev(Pit curr){
		if(curr.pit_id == 0) return null;
		return this.pits[curr.pit_id-1];
	}

	Pit getNext(Pit curr){
		if(curr.pit_id == this.pits.length-1) return null;
		return this.pits[curr.pit_id+1];
	}

	// update single dancer's next position, shrink everyone to the head of the snake;
	void connect() {
		int[] supposed_pit_num = new int[d];
		this.stay = 0;
		int target_pit_id = 0;
		this.connected = true;
		boolean[] moved = new boolean[d];
		while(target_pit_id < this.d - this.couples_found){
			//find the closest dancer along the line to target pit id;
			int dancer_id = -1;
			int min_pit = d;
			for(int j = 0; j < d; j++){
				if(dancers[j].soulmate != -1 || moved[j]) continue;
				if(dancers[j].pit_id < min_pit){
					min_pit = dancers[j].pit_id;
					dancer_id = j;
				}
			}
			moved[dancer_id] = true;
			
			Pit curr_pit = pits[dancers[dancer_id].pit_id];
			Pit pointer = curr_pit;
			Pit prev = null;
			Point curr_pos = this.last_positions[dancer_id];
			boolean stop = false;
			while(!stop){
				prev = pointer;
				if(pointer.pit_id < target_pit_id){
					pointer = getNext(pointer);
				}
				else if(pointer.pit_id > target_pit_id){
					pointer = getPrev(pointer);
				}
				stop = distance(pointer.pos,curr_pos) > 2 || pointer.pit_id == target_pit_id || findDancer(pointer.pit_id) != -1;
			}
			if(distance(pointer.pos,curr_pos) > 2 || findDancer(pointer.pit_id) != -1){
				pointer = prev;
			}
			dancers[dancer_id].pit_id = pointer.pit_id;
			this.dancers[dancer_id].cur_pos = this.dancers[dancer_id].next_pos;
			dancers[dancer_id].next_pos = pointer.pos;
			this.connected = this.connected && pointer.pit_id == target_pit_id;
			supposed_pit_num[dancer_id] = pointer.pit_id;
			target_pit_id++;
		}

		boolean swap_and_dance = true;
		
		//after arranged pits, see if it is possible to swap all the dancers in this round
		if(this.connected){
			Point[] next_pos = new Point[d];
			int[] next_pit_id = new int[d];
			for (int i = 0; i < d; i++) {
				if(dancers[i].soulmate != -1) continue;
				int curr_pit = dancers[i].pit_id;
				int next_pit = getNextPit(curr_pit);
				if(next_pit != -1){
					next_pos[i] = findNearestActualPoint(pits[next_pit].pos, pits[curr_pit].pos);
					next_pit_id[i] = next_pit;
				}
				else{
					next_pos[i] = dancers[i].next_pos;
					next_pit_id[i] = dancers[i].pit_id;
				}
				swap_and_dance = swap_and_dance && distance(next_pos[i],this.last_positions[i]) < 2;
			}
			if(swap_and_dance){
				for(int i = 0; i < d; i++){
					if(dancers[i].soulmate != -1) continue;
					this.dancers[i].cur_pos = this.dancers[i].next_pos;
					dancers[i].next_pos = next_pos[i];
					dancers[i].pit_id = next_pit_id[i];
				}
				this.state = 3 - this.state;
				this.stay = 0;
			}
		}
	}

	//calculate Euclidean distance between two points
	double distance(Point p1,Point p2){
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx*dx+dy*dy);
	}

	// according to the information of the dancers, 
	void move_couple() {
		for(int i = 0; i < d; i++){
			if(dancers[i].soulmate == -1) continue;
			Point curr = this.last_positions[i];
			Point des = this.dancers[i].des_pos;
			this.dancers[i].cur_pos = this.dancers[i].next_pos;
			this.dancers[i].next_pos = findNextPosition(curr, des);
		}
	}

	Point findNextPosition(Point curr, Point des) {
		if (distance(curr,des) < 2) return des;
		else {
			double x = des.x - curr.x;
			double y = des.y - curr.y;
			Point next = new Point(curr.x + (2-this.delta)*x/Math.sqrt(x*x+y*y), curr.y + (2-this.delta)*y/Math.sqrt(x*x+y*y));
			return next;
		}
	}

	// generate instruction according to this.dancers
	private Point[] generateInstructions(){
		Point[] movement = new Point[d];
		for(int i = 0; i < d; i++){
			movement[i] = new Point(dancers[i].next_pos.x-this.last_positions[i].x,dancers[i].next_pos.y-this.last_positions[i].y);
			if(movement[i].x * movement[i].x + movement[i].y * movement[i].y > 4){
				movement[i] = new Point(0,0);
			}
		}
		return movement;
	}

	private boolean samepos(Point p1,Point p2){
		return Math.abs(p1.x - p2.x) < this.delta && Math.abs(p1.y - p2.y) < this.delta;
	}

	

	
}
