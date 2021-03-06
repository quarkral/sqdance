package sqdance.g9;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import sqdance.sim.Point;

public class Player implements sqdance.sim.Player {
	private static int THRESHOLD1 = 900;
	private static int THRESHOLD2 = 1880;
	private static int THRESHOLD3 = 2500;

	private RoundTablePlayer p1 = new RoundTablePlayer();
    private SnakeShiftPlayer p2 = new SnakeShiftPlayer();
	private UltimatePlayer p3 = new UltimatePlayer();
	private HighDensityPlayer p4 = new HighDensityPlayer();

	private int d;
	private int room_side;

	@Override
	public void init(int d, int room_side) {
		this.d = d;
		this.room_side = room_side;
		if (d <= THRESHOLD1)
			p1.init(d, room_side);
		else if (d <= THRESHOLD2)
			p2.init(d, room_side);
		else if (d <= THRESHOLD3)
			p3.init(d, room_side);
		else
			p4.init(d, room_side);
	}

	@Override
	public Point[] generate_starting_locations() {
		if (d <= THRESHOLD1)
			return p1.generate_starting_locations();
		else if (d <= THRESHOLD2)
			return p2.generate_starting_locations();
		else if (d <= THRESHOLD3)
			return p3.generate_starting_locations();
		else
			return p4.generate_starting_locations();
	}

	@Override
	// dancers: array of locations of the dancers
	// scores: cumulative score of the dancers
	// partner_ids: index of the current dance partner. -1 if no dance partner
	// enjoyment_gained: integer amount (-5,0,3,4, or 6) of enjoyment gained in
	// the most recent 6-second interval
	public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
		if (d <= THRESHOLD1)
			return p1.play(dancers, scores, partner_ids, enjoyment_gained);
		else if (d <= THRESHOLD2)
			return p2.play(dancers, scores, partner_ids, enjoyment_gained);
		else if (d <= THRESHOLD3)
			return p3.play(dancers, scores, partner_ids, enjoyment_gained);
		else
			return p4.play(dancers, scores, partner_ids, enjoyment_gained);
	}

}
