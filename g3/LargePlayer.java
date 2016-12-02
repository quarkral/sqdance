package sqdance.g3;

import sqdance.sim.Point;
import java.util.*;
import java.io.*;

public class LargePlayer implements sqdance.sim.Player {

    private int                d;
    private double             room_side;
    private double             dance_dist   = 0.501;
    private double             pair_dist    = 0.502;
    private double             wait_dist    = 0.1004;
    private int                dancer_count = 0;
    private ArrayList<Integer> curr_dancers = new ArrayList<Integer>();

    public void init(int d, int room_side) {
    	this.d = d;
    	this.room_side = (double) room_side;
    }

    @Override
    public Point[] generate_starting_locations() {
        Point[] L         = new Point[d];
        double  x_right   = room_side;
        double  x_left    = 0;
        double  y         = 0;
        int     j         = 0;
        int     col_count = 0;

        for (int i = 0; i < d; i++) {
            //L[i] = new Point(x_left, y);
            if (i % 2 == 0)
                L[i] = new Point(x_left, y);
            else
                L[i] = new Point(x_right, y);
            y += wait_dist;
            if (y > room_side) {
                x_left  += wait_dist;
                x_right -= wait_dist;
                y  = 0;
            }
        }

        x_left  += dance_dist;
        x_right -= dance_dist;
        y = 0;
        while (x_left <= x_right && j < d) {
            dancer_count++;
            curr_dancers.add(j++);
            L[j] = new Point(x_left, y);
            y += pair_dist;
            if (y > room_side) {
                col_count++;
                if (col_count % 2 == 0) x_left += pair_dist;
                else x_left += dance_dist;
                y  = 0;
            }
        }

        System.out.println(dancer_count);

        return L;
    }

    @Override
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids,
                        int[] enjoyment_gained) {
        Point[]             L         = new Point[d];
        ArrayList<double[]> to_switch = new ArrayList<double[]>();
        ArrayList<Integer>  no_change = new ArrayList<Integer>();

        for (int i = 0; i < d; i++) {
            L[i] = new Point(0, 0);
        }

        return L;
    }
}
