package sqdance.g9;

import sqdance.sim.Point;

import java.io.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.Collections;

// import sqdance.g9.Pair;
import sqdance.g9.Dancer;
// import sqdance.g9.DancePair;

public class SnakeShiftPlayer implements sqdance.sim.Player {

	/*
	 * E[i][j]: the remaining enjoyment player j can give player i -1 if the
	 * value is unknown (everything unknown upon initialization)
	 */
	private int[][] E = null;

	/*
	 * contains the int id's of dancers dancing with friends and ergo not
	 * shifting in snakeshift
	 */
	private ArrayList<Integer> friending = null;

	// random generator
	private Random random = null;

	// simulation parameters
	private int d = -1;
	private double room_side = -1;

	private int[] idle_turns;

	// # of columns of paired dancers in initial grid configuration placement
	private int strnger_turn;
	private int strnger_turn_lim;

	// dancer count thresholds for different player strategies
	private int threshold1;
	private int threshold2;

	/* holds Dancer nodes in pseudo-doubly linkedlist */
	private Dancer[] snake = null;

	/*
	 * init function called once with simulation parameters before anything else
	 * is called
	 */
	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = (double) room_side;
		this.threshold1 = 900;
		this.threshold2 = 1880;
		this.friending = new ArrayList<Integer>();
		this.strnger_turn = 0;
		this.strnger_turn_lim = 20;
		this.snake = new Dancer[d];
		random = new Random();
		E = new int[d][d];
		idle_turns = new int[d];
		for (int i = 0; i < d; i++) {
			idle_turns[i] = 0;
			for (int j = 0; j < d; j++) {
				E[i][j] = i == j ? 0 : -1;
			}
		}
	}

	/*
	 * setup function called once to generate initial player locations note the
	 * dance caller does not know any player-player relationships, so order
	 * doesn't really matter in the Point[] you return. Just make sure your
	 * player is consistent with the indexing
	 */
	public Point[] generate_starting_locations() {
		Point[] L = this.generate_startgrid();
		this.create_snake();
		return L;
	}

	/*
	 * generate starting grid: place dancers in columns of 2 side-by-side
	 * dancers with alternating columns forming from top and bottom of the
	 * dancefloor respectively.
	 */
	private Point[] generate_startgrid() {
		double x = 0, y = 0;
		double x2 = 0.50000000000001;
		double xOffset = 0.250000000000005;
		double x1alt = x + xOffset;
		double x2alt = x2 + xOffset;
		boolean even = true;
		boolean new_column = false;
		boolean even_col = true;
		boolean odd_row = false;
		Point[] L = new Point[d];
		Point test1, test2 = null;
		for (int i = 0; i < d; ++i) {

			test1 = new Point(x, y);
			test2 = new Point(x2, y);

			// System.out.printf("dancer %d\n", i);
			// System.out.printf("x coordinate: %.15f\ny coordinate: %.15f\n",
			// test1.x, test1.y);

			if (!test1.valid_movement(new Point(0, 0), (int) room_side)
					|| !test2.valid_movement(new Point(0, 0),
							(int) room_side)) {
				even_col = (!even_col);
				if (even_col) {
					y = 0;
				} else {
					y -= 0.43301270189224;
				}
				x += 1.0000000000001;
				x2 += 1.0000000000001;
				x1alt += 1.0000000000001;
				x2alt += 1.0000000000001;
				new_column = true;
				odd_row = (!odd_row);
			} else {
				new_column = false;
			}

			Point coordinates = null;
			if (odd_row) {
				if (even) {
					coordinates = new Point(x1alt, y);
				} else {
					coordinates = new Point(x2alt, y);
				}
			} else {
				if (even) {
					coordinates = new Point(x, y);
				} else {
					coordinates = new Point(x2, y);
				}
			}

			L[i] = coordinates;

			even = (!even);
			if (even)
				odd_row = (!odd_row);
			if (new_column)
				continue;
			if (even_col && even) {
				y += 0.43301270189224;
			} else if (!even_col && even) {
				y -= 0.43301270189224;

				// to fix rounding error at the top
				if (y < 0) {
					if (y > -0.000000000000004)
						y = Math.round(y * 10) / 10;
				}
			}
		}
		return L;
	}

	private void create_snake() {
		int halfway = (d / 2) - 1;
		int last = d - 1;
		int id = 0;
		for (int i = 0; i < d; i++) {

			Dancer cur_dancer = null;
			if (i < halfway) {
				cur_dancer = new Dancer(id);
				id += 2;

			} else if (i == halfway) {
				cur_dancer = new Dancer(id);
				id += 1;
			} else {
				cur_dancer = new Dancer(id);
				id -= 2;
			}
			snake[i] = cur_dancer;
		}
		this.link_snake();
		this.set_initial_partnerIDs();
		// this.print_start_snake();
	}

	private void link_snake() {
		for (int i = 0; i < d; i++) {
			int next_index = (i + 1) % d;
			snake[i].setNext(snake[next_index]);
			snake[next_index].setPrev(snake[i]);
		}
	}

	private void set_initial_partnerIDs() {
		int last = d - 1;
		for (int idx1 = 0; idx1 < (d / 2); idx1++) {
			int idx2 = last - idx1;
			Dancer one = snake[idx1];
			Dancer two = snake[idx2];
			one.setPartnerId(two.id);
			two.setPartnerId(one.id);
		}
	}

	/*
	 * play function dancers: array of locations of the dancers scores:
	 * cumulative score of the dancers partner_ids: index of the current dance
	 * partner. -1 if no dance partner enjoyment_gained: integer amount
	 * (-5,0,3,4, or 6) of enjoyment gained in the most recent 6-second interval
	 */
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids,
			int[] enjoyment_gained) {

		// System.out.println("*** TURN START ****");
		// debugging
		// this.print_ids_and_pids();
		// this.print_partners();

		this.strnger_turn++;
		Point[] instructions = new Point[d];

		// need to update ALL partner id's before manipulating the list
		for (int x = 0; x < d; x++) {
			int j = partner_ids[x];
			Point self = dancers[x];
			if (enjoyment_gained[x] > 0) { // previously had a dance partner
				idle_turns[x] = 0;
				Point dance_partner = dancers[j];
				// update remaining available enjoyment
				if (E[x][j] == -1) {
					E[x][j] = total_enjoyment(enjoyment_gained[x])
							- enjoyment_gained[x];
				} else {
					E[x][j] -= enjoyment_gained[x];
				}

				// Update every dancers' relation status and partner from the
				// last interval
				String relationship = this.partner_type(enjoyment_gained[x]);
				int snake_index = this.snake_index_of_id(x);
				Dancer cur = snake[snake_index];
				cur.setDanceRelationship(relationship);
				cur.setPartnerId(j);

			}
		}

		for (int x = 0; x < d; x++) {
			int snake_index = this.snake_index_of_id(x);
			Dancer cur = snake[snake_index];
			int joyThreshold = (((this.strnger_turn_lim -
										this.strnger_turn) * 3) + 60);
			int remainingJoy = E[cur.id][cur.getPartnerId()];

			if(cur.getDanceRelationship().equals("friend") && remainingJoy > joyThreshold){
				//check if the two guys directly north of cur are already dancing with friends

				// System.out.println("Hey, I would've added " + cur.id + " to the friending list.");

				/*
				if(!this.friending.contains(cur.id)){
					Dancer prev = cur.getPrev();
					//Dancer prevprev = prev.getPrev();
					if (this.friending.contains(prev.id)) {
						System.out.println("Could not add " + cur.id + " to the friending list. His prev, " + prev.id + " is already dancing with a friend.");
					//} else if (this.friending.contains(prevprev.id)) {
					//	System.out.println("Could not add " + cur.id + " to the friending list. His prevprev, " + prevprev.id + " is already dancing with a friend.");
					} else {
						this.friending.add(cur.id);
						System.out.println("Added " + cur.id + " to the friending list.");
					}
				}
				*/
			}
			// check to see if any of the removed from snakeshift dancers
			// should be added back.
			if(cur.getDanceRelationship().equals("friend") && remainingJoy <= joyThreshold) {
				if(this.friending.contains(cur.id)) {
					this.friending.remove(this.friending.indexOf(cur.id));
					// System.out.println("Removed " + cur.id + " from the friending list.");
				}
			}
		}

		//initialize instructions. assume no one is moving
		for (int i = 0; i < d; i++) {
			Point m = null;
			m = new Point(0.0, 0.0);
			instructions[i] = m;
		}

		//now handle if people are moving...
		if (this.strnger_turn == this.strnger_turn_lim) {
			// System.out.println("*** EVERYONE IS GOING TO SHIFT ****");

			// recalibrate who's following who for dancers staying out of snakeshift
			for(int id : this.friending) {
				Dancer current = snake[this.snake_index_of_id(id)];
				// print out id and current.id to make sure they are the same
				//System.out.println("id = " + id + ", current.id = " + current.id);
				this.preshift_calibration(current);
				// System.out.println(current.id + " is not going to snakeshift...");
			}

			// DEBUGGING
			// print dont shift list.
			// System.out.println("\nFriending dancers' IDs list:");
			// for (int z : this.friending) {
			// System.out.printf("%d, ", z);
			// }
			// System.out.println();
			// this.print_snake();
			// this.print_partners();
			//
			// ArrayList<Integer> moving_indices = this.make_moving_list();
			// for(int b : this.friending){
			// if(moving_indices.contains(this.snake_index_of_id(b))){
			// System.out.printf("dancer of index %d and id %d is"
			// + "removed from snakeshift, yet is still either"
			// + "being traveled to or from.\n", this.snake_index_of_id(b),
			// b);
			// }
			// }
			// moving_indices.clear();

			Point m = null;
			for (int k = 0; k < d; k++) {
				// System.out.printf("dancer %d wants to move to %d's
				// position\n", k, movement[k]);

				// stay put if you're listed as friend-dancing
				if(this.friending.contains(k)) {
					m = new Point(0.0, 0.0);
					// System.out.println(k + " is dancing with a friend and won't be moving");
				} else {
					// calculate the index of dancer id = k in snake[]
					int proper_index = this.snake_index_of_id(k);

					Dancer current = snake[proper_index];
					Dancer dest = current.getNext();
					double x2 = dancers[dest.id].x;
					double y2 = dancers[dest.id].y;
					double x1 = dancers[current.id].x;
					double y1 = dancers[current.id].y;
					m = new Point(x2 - x1, y2 - y1);
					// System.out.println(current.id + " is following " + current.getNext().id + " and plans to move: " + m.x + ',' + m.y);
				}
				instructions[k] = m;
			}
			this.strnger_turn = 0;


			java.util.Queue<Dancer> recalbs = new java.util.LinkedList<Dancer>();
			// rejigger Dancer connections post shift instructions
			for(int id : this.friending) {
				Dancer current = snake[this.snake_index_of_id(id)];
				recalbs.add(current);
				// print out id and current.id to make sure they are the same
				//System.out.println("id = " + id + ", current.id = " + current.id);
			}
			while (recalbs.peek() != null) {
				Dancer d = recalbs.remove();
				boolean recalbWorked = this.postshift_calibration(d);
				if (!recalbWorked) {
					// System.out.println(" will try recalibration again later...");
					recalbs.add(d);
				} else {
					// System.out.println(d.id + " has been postshift recalibrated...");
				}

			}
		}
		return instructions;
	}

	private int snake_index_of_id(int id) {
		// calculate the index of dancer id in snake[]
		int proper_index = -1;
		int last = d - 1;
		boolean index_even = ((id % 2) == 0);
		if (index_even) {
			proper_index = id / 2;
		} else {
			// dancer exists calculate dist away from last index
			// of snake[]
			int calculate = (id - 1) / 2;
			proper_index = last - calculate;
		}
		return proper_index;
	}

	/*
	 * Used to remove friend-dancing Dancer from snakeshift. Will have the
	 * lowest scoring dancing couple remain dancing with a friend and outside of
	 * the snakeshift. There are 3 categories of removal. 1) This removal would
	 * mark 3 adjacent dancers dancing with friends. 2) This removal would mark
	 * 2 adjacent dancers dancing with friends. 3) Neither of the dancers above
	 * or below are dancing with friends.
	 */
	private void remove_from_snakeshift(Dancer cur) {
		// only act if cur dancer isn't already dancing with a friend
		if (!this.friending.contains(cur.id)) {
			// int proper_index = this.snake_index_of_id(cur.id);
			// int index_of_next = (proper_index + 1) % d;
			// int index_of_nextnext = (index_of_next + 1) % d;
			// int index_of_prev = proper_index - 1;
			// int index_of_prevprev = index_of_prev - 1;
			// // correct the prev indecies if they go negative
			// if (index_of_prev < 0)
			// 	index_of_prev = d + index_of_prev;
			// if (index_of_prevprev < 0)
			// 	index_of_prevprev = d + index_of_prevprev;

			// these var names are indicative of these dancers' respective
			// positions in the snake array
			Dancer minusOne = cur.getPrev();
			Dancer minusTwo = minusOne.getPrev();
			Dancer plusOne = cur.getNext();
			Dancer plusTwo = plusOne.getNext();

			// var for Dancer that needs to be added back to snakeshift if the
			// case makes it necessary
			Dancer rtrnToShft = null;

			/*
			 * case(1): 3 adjacent dancers will be dancing with friends 3
			 * situations fit the criteria - a) If minusOne and minusTwo are
			 * friend dancing. b) If plusOne and plusTwo are friend dancing. c)
			 * If minusOne and plusOne are friend dancing.
			 */
			boolean case1a = this.friending.contains(minusOne.id)
					&& this.friending.contains(minusTwo.id);
			boolean case1b = this.friending.contains(plusOne.id)
					&& this.friending.contains(plusTwo.id);
			boolean case1c = this.friending.contains(minusOne.id)
					&& this.friending.contains(plusOne.id);

			if (case1a) {
				// determine the dance couple who has the highest low score
				// among its pair from cur, minusOne, and minusTwo with their
				// respective partners and add the dancers of that pair back
				// into the snakeshift
				rtrnToShft = this.determineHighest(cur, minusOne, minusTwo);

				// add the adjacent-to-current dancer of that pair back into
				// snakeshift. adds the dancer AND its partner back into
				// snakeshift
				if (!cur.equals(rtrnToShft)) {
					this.add_to_snakeshift(rtrnToShft);
					this.remove_from_snakeshift(cur);
				}
			} else if (case1b) {
				// ditto to above except specific to case1b (cur, plusOne, and
				// plusTwo)
				rtrnToShft = this.determineHighest(cur, plusOne, plusTwo);

				if (!cur.equals(rtrnToShft)) {
					this.add_to_snakeshift(rtrnToShft);
					this.remove_from_snakeshift(cur);
				}
			} else if (case1c) {
				// ditto to above except specific to case1c (cur, minusOne,
				// and plusOne)
				rtrnToShft = this.determineHighest(cur, plusOne, plusTwo);

				if (!cur.equals(rtrnToShft)) {
					this.add_to_snakeshift(rtrnToShft);
					this.remove_from_snakeshift(cur);
				}
			} else {
				this.preshift_calibration(cur);
			}
		}
	}

	private void preshift_calibration(Dancer cur) {
		// int proper_index = this.snake_index_of_id(cur.id);
		// int index_of_next = (proper_index + 1) % d;
		// int index_of_nextnext = (index_of_next + 1) % d;
		// int index_of_prev = proper_index - 1;
		// int index_of_prevprev = index_of_prev - 1;
		// // correct the prev indecies if they go negative
		// if (index_of_prev < 0)
		// 	index_of_prev = d + index_of_prev;
		// if (index_of_prevprev < 0)
		// 	index_of_prevprev = d + index_of_prevprev;

		// these var names are indicative of these dancers' respective
		// positions in the snake array
		Dancer minusOne = cur.getPrev();
		Dancer minusTwo = minusOne.getPrev();
		Dancer minusThree = minusTwo.getPrev();
		Dancer plusOne = cur.getNext();
		Dancer plusTwo = plusOne.getNext();
		Dancer plusThree = plusTwo.getNext();
		/*
		 * case(2): 2 adjacent dancers will be dancing with friends 2 situations
		 * fit the criteria - a) If minusOne is friend dancing. b) If plusOne is
		 * friend dancing.
		 */
		// boolean case2a = this.friending.contains(minusOne.id);
		// boolean case2b = this.friending.contains(plusOne.id);

		// if (case2a) { // prev is dancing with a friend
		// 	minusTwo.setNext(plusOne);
		// 	plusOne.setPrev(minusTwo);
		// 	minusTwo.setPrev(cur);
		// 	cur.setNext(minusTwo);
		// 	minusOne.setPrev(minusThree);
		// 	minusThree.setNext(minusOne);
		// } else if (case2b) { // next is dancing with a friend
		// 	minusOne.setNext(plusTwo);
		// 	plusTwo.setPrev(minusOne);
		// 	minusOne.setPrev(plusOne);
		// 	plusOne.setNext(minusOne);
		// 	cur.setPrev(minusTwo);
		// 	minusTwo.setNext(cur);
		// } else { // neither next nor prev are dancing with a friend
			plusOne.setPrev(minusOne);
			minusOne.setNext(plusOne);
			// minusOne.setPrev(cur);
			cur.setNext(minusOne);
			// cur.setPrev(minusTwo);
			// minusTwo.setNext(cur);
		// }
		// this.friending.add(cur.id);
		// System.out.println("Added " + cur.id + " to the friending list.");
		// int partnerID = cur.getPartnerId();
		// if (!this.friending.contains(partnerID)) {
		// 	// System.out.println("Going to add his partner as well.");
		// 	int partner_idx = this.snake_index_of_id(partnerID);
		// 	Dancer partner = snake[partner_idx];
		// 	// System.out.println("partnerID =? " + partner.id);
		// 	this.preshift_calibration(partner);
		// }
	}


	private boolean postshift_calibration(Dancer cur) {
		// int proper_index = this.snake_index_of_id(cur.id);
		// int index_of_next = (proper_index + 1) % d;
		// int index_of_nextnext = (index_of_next + 1) % d;
		// int index_of_prev = proper_index - 1;
		// int index_of_prevprev = index_of_prev - 1;
		// // correct the prev indecies if they go negative
		// if (index_of_prev < 0)
		// 	index_of_prev = d + index_of_prev;
		// if (index_of_prevprev < 0)
		// 	index_of_prevprev = d + index_of_prevprev;

		// these var names are indicative of these dancers' respective
		// positions in the snake array
		// Dancer minusOne = cur.getPrev();
		// Dancer minusTwo = minusOne.getPrev();
		// Dancer minusThree = minusTwo.getPrev();
		// Dancer plusOne = cur.getNext();
		// Dancer plusTwo = plusOne.getNext();
		// Dancer plusThree = plusTwo.getNext();
		/*
		 * case(2): 2 adjacent dancers will be dancing with friends 2 situations
		 * fit the criteria - a) If minusOne is friend dancing. b) If plusOne is
		 * friend dancing.
		 */
		// boolean case2a = this.friending.contains(minusOne.id);
		// boolean case2b = this.friending.contains(plusOne.id);

		// if (case2a) { // prev is dancing with a friend
		// 	minusTwo.setNext(plusOne);
		// 	plusOne.setPrev(minusTwo);
		// 	minusTwo.setPrev(cur);
		// 	cur.setNext(minusTwo);
		// 	minusOne.setPrev(minusThree);
		// 	minusThree.setNext(minusOne);
		// } else if (case2b) { // next is dancing with a friend
		// 	minusOne.setNext(plusTwo);
		// 	plusTwo.setPrev(minusOne);
		// 	minusOne.setPrev(plusOne);
		// 	plusOne.setNext(minusOne);
		// 	cur.setPrev(minusTwo);
		// 	minusTwo.setNext(cur);
		// } else { // neither next nor prev are dancing with a friend
			Dancer minusOne = cur.getNext();
			Dancer minusTwo = minusOne.getPrev();
			if (cur.id == minusTwo.id) {
				// System.out.println("OH CRAP current id " + cur.id + " == minusTwo id " + minusTwo.id + ". Postshift calibration failure");
				return false;
			}
			minusTwo.setNext(cur);
			cur.setPrev(minusTwo);
			minusOne.setPrev(cur);
			return true;
			// cur.setPrev(minusTwo);
			// minusTwo.setNext(cur);
		// }
		// this.friending.add(cur.id);
		// System.out.println("Added " + cur.id + " to the friending list.");
		// int partnerID = cur.getPartnerId();
		// if (!this.friending.contains(partnerID)) {
		// 	// System.out.println("Going to add his partner as well.");
		// 	int partner_idx = this.snake_index_of_id(partnerID);
		// 	Dancer partner = snake[partner_idx];
		// 	// System.out.println("partnerID =? " + partner.id);
		// 	this.preshift_calibration(partner);
		// }
	}

	/* add the Dancer arg and its partner back into the snakeshift */
	private void add_to_snakeshift(Dancer toAdd) {
		// int proper_index = this.snake_index_of_id(toAdd.id);
		// int idx_of_nxt = (proper_index + 1) % d;
		// int idx_of_nxtnxt = (idx_of_nxt + 1) % d;
		// int idx_of_prv = proper_index - 1;
		// int idx_of_prvprv = idx_of_prv - 1;
		// if (idx_of_prv < 0)
		// 	idx_of_prv = d + idx_of_prv;
		// if (idx_of_prvprv < 0)
		// 	idx_of_prvprv = d + idx_of_prvprv;

		// these var names are indicative of these dancers' respective
		// positions in the snake array
		// Dancer minusOne = snake[idx_of_prv];
		// Dancer minusTwo = snake[idx_of_prvprv];
		// Dancer plusOne = snake[idx_of_nxt];
		// Dancer plusTwo = snake[idx_of_nxtnxt];

		// Dancer partner = null;

		// if (this.friending.contains(minusOne.id)) {
		// 	minusTwo.setNext(toAdd);
		// 	toAdd.setNext(plusOne);
		// } else if (this.friending.contains(plusOne.id)) {
		// 	minusOne.setNext(toAdd);
		// 	toAdd.setNext(plusTwo);
		// } else {
		// 	minusOne.setNext(toAdd);
		// 	toAdd.setNext(plusOne);
		// }
		// if (this.friending.contains(toAdd.id)) {
		// 	this.friending.remove(this.friending.indexOf(toAdd.id));
		// 	// System.out.println("Removed " + toAdd.id + " from friending
		// 	// list");
		// }
		// should print friending list here to make sure it is removing it
		// like i think it is
		// this.print_partners();
		// System.out.println("Friending IDs");
		// for(int id : this.friending){
		// System.out.printf("%d ", id);
		// }
		// System.out.println();

		// if (this.friending.contains(toAdd.getPartnerId())) {
		// 	// System.out.println("Going to remove his partner as well.");
		// 	int snake_idx = this.snake_index_of_id(toAdd.getPartnerId());
		// 	partner = snake[snake_idx];
		// 	this.add_to_snakeshift(partner);
		// }

		/*
		 * remove the dancer's id and the dancer's partner's id from the
		 * friending list
		 */
		if(this.friending.contains(toAdd.id)) {
			this.friending.remove(this.friending.indexOf(toAdd.id));
		}
		// if(this.friending.contains(toAdd.getPartnerId())) {
		// 	this.friending.remove(this.friending.indexOf(toAdd.getPartnerId()));
		// }
	}

	/*
	 * evaluate the scores of Dancer args and their respective partner dancers.
	 * Compare the lowest scoring Dancers of each pair against each other and
	 * return the highest scoring one of those three.
	 */
	private Dancer determineHighest(Dancer a1, Dancer b1, Dancer c1) {
		int snake_idxa2 = this.snake_index_of_id(a1.getPartnerId());
		int snake_idxb2 = this.snake_index_of_id(b1.getPartnerId());
		int snake_idxc2 = this.snake_index_of_id(c1.getPartnerId());
		Dancer a2 = snake[snake_idxa2];
		Dancer b2 = snake[snake_idxb2];
		Dancer c2 = snake[snake_idxc2];
		int minA, minB, minC;
		if (a1.score <= a2.score) {
			minA = a1.score;
		} else {
			minA = a2.score;
		}
		if (b1.score <= b2.score) {
			minB = b1.score;
		} else {
			minB = b2.score;
		}
		if (c1.score <= c2.score) {
			minC = c1.score;
		} else {
			minC = c2.score;
		}
		if (minA >= minB) {
			if (minA >= minC) {
				return a1;
			} else {
				return c1;
			}
		} else {
			if (minB >= minC) {
				return b1;
			} else {
				return c1;
			}
		}
	}

	private int total_enjoyment(int enjoyment_gained) {
		switch (enjoyment_gained) {
		case 3:
			return 60; // stranger
		case 4:
			return 200; // friend
		case 6:
			return 10800; // soulmate
		default:
			throw new IllegalArgumentException("Not dancing with anyone...");
		}
	}

	private String partner_type(int enjoyment_gained) {
		switch (enjoyment_gained) {
		case 3:
			return "stranger";
		case 4:
			return "friend";
		case 6:
			return "soulmate";
		default:
			return "none";
		}
	}

	/* helper function. aids in my visual comparison. */
	private ArrayList<Integer> make_moving_list() {
		ArrayList<Integer> list = new ArrayList<>();
		int i;
		for (i = 0; i < d; i++) {
			int snake_index = this.snake_index_of_id(i);
			int nextId = snake[snake_index].getNext().id;
			int nextId_snk_idx = this.snake_index_of_id(nextId);
			if (!this.friending.contains(i)) {
				if (!list.contains(snake_index)) {
					list.add(snake_index);
				}
				if (!list.contains(nextId_snk_idx)) {
					list.add(nextId_snk_idx);
				}
			}
		}
		return list;
	}

	/* Some printing functions to help me debug */
	private void print_start_snake() {
		int i;
		for (i = 0; i < d; i++) {
			System.out.printf("%d->", snake[i].id);
		}
		System.out.printf("%d", snake[i - 1].getNext().id);
	}

	private void print_snake() {
		int i;
		for (i = 0; i < d; i++) {
			int snake_index = this.snake_index_of_id(i);
			int nextId = snake[snake_index].getNext().id;
			int nextId_snk_idx = this.snake_index_of_id(nextId);
			if (!this.friending.contains(i)) {
				System.out.printf("%d->%d, ", i, nextId);
			}
		}
		System.out.println();
	}

	// helper function to print and see partner ids and such
	private void print_ids_and_pids() {
		for (int i = 0; i < d; i++) {
			int id = snake[i].id;
			int pid = snake[i].getPartnerId();
			System.out.printf("Dancer %d is partnered to dancer" + "%d\n", id,
					pid);
		}
	}

	private void print_partners() {
		System.out.println("Partnerings:");
		int i;
		for (i = 0; i < d; i++) {
			int snake_index = this.snake_index_of_id(i);
			int partnerID = snake[snake_index].getPartnerId();
			System.out.printf("%d<->%d, ", i, partnerID);
		}
		System.out.println();
	}
}
