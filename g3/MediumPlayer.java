package sqdance.g3;

import sqdance.sim.Point;
import java.util.*;
import java.io.*;

public class MediumPlayer implements sqdance.sim.Player {

    public enum Status { SHIFT, DANCE }

    private final double DANCE_DISTANCE = 0.501;
    private final double SOULMATE_X = 0.2505;
    private final double SOULMATE_Y = 0.434;
    private final double NEXT_ROW_DISTANCE = SOULMATE_Y;
    private final double NEXT_COL_DISTANCE = 2 * DANCE_DISTANCE + 0.001;
    private final double X_OFFSET = 0.01;
    private final double Y_OFFSET = 0.01;
    private final int DANCE_TURNS = 20;

    // Not modified after initialization
    private int SPLIT_CAP;
    private int NUM_ROWS;
    private int HALF;

    private int d;
    private double room_side;

    private Point[] grid_1;
    private Point[] grid_2;
    private Point[] final_grid;

    private Status status;
    private int time_counter;

    public void init(int d, int room_side) {
    	this.d = d;
    	this.room_side = (double) room_side;

        this.HALF = (int) (d / 2);
        SPLIT_CAP = (((this.HALF % 2) == 1) ? (this.HALF+1) : this.HALF);
        this.NUM_ROWS = (int)((room_side - Y_OFFSET) / NEXT_ROW_DISTANCE);

        grid_1 = new Point[this.HALF];
        grid_2 = new Point[this.HALF];
        final_grid = new Point[d];

        status = Status.DANCE;
        time_counter = DANCE_TURNS;
    }

    @Override
    public Point[] generate_starting_locations() {
        double gridx = this.X_OFFSET;
        double gridy = this.Y_OFFSET;
        boolean offcol = false;

        int i = 0;
        int ii = 0;
        
        double xoff = 0;
        while (gridx <= this.room_side) {
            while (gridy <= this.room_side) {
                grid_1[i] = new Point(gridx + xoff, gridy);
                grid_2[i] = new Point((gridx + DANCE_DISTANCE + xoff), gridy);

                if (offcol) {
                    gridy = gridy - NEXT_ROW_DISTANCE;
                } else {
                    gridy = gridy + NEXT_ROW_DISTANCE;
                }

                i = i + 1;
                ii = ii + 1;

                if ((ii >= NUM_ROWS) || (i >= this.HALF)){
                    break;
                }

                if (xoff == 0) {
                    xoff = SOULMATE_X;
                } else {
                    xoff = 0;
                }
            }

            gridx = gridx + NEXT_COL_DISTANCE;
            offcol = !offcol;

            if (offcol) {
                gridy = Y_OFFSET + ((NUM_ROWS  - 1) * NEXT_ROW_DISTANCE);
            } else {
                gridy = Y_OFFSET;
            }

            ii = 0;
            if (i >= this.HALF) {
                break;
            }
        }

        int limit = this.HALF;
        if (i < this.HALF) limit = i;

        for (i = 0; i < limit; i++) {
            final_grid[i] = grid_1[i];
            final_grid[(2*limit)-i-1] = grid_2[i];
        }

        return final_grid;
    }

    @Override
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids,
                        int[] enjoyment_gained) {
    	Point[] instructions = new Point[d];
        double newx, oldx, newy, oldy;

        if (status == Status.DANCE) {
            for (int i = 0; i < d; i++) {
                instructions[i] = new Point(0,0);
            }
            time_counter--;
        }

        if (status == Status.SHIFT) {
            for (int i = 0; i < d; i++) {
                if (get_index(dancers,i) >= SPLIT_CAP) {
                    if ((get_index(dancers, i) % 2) == 0) {
                        newx = get_point(dancers, i, 2).x;
                        newy = get_point(dancers, i, 2).y;
                        oldx = get_point(dancers, i, 0).x;
                        oldy = get_point(dancers, i, 0).y;
                        instructions[i] = new Point((newx - oldx), (newy - oldy));
                    } else {
                        instructions[i] = new Point(0,0);
                    }
                } else {
                    newx = get_point(dancers, i, 1).x;
                    newy = get_point(dancers, i, 1).y;
                    oldx = get_point(dancers, i, 0).x;
                    oldy = get_point(dancers, i, 0).y;
                    instructions[i] = new Point((newx - oldx), (newy - oldy));
                }
            }
        }

        if (time_counter == 0) {
            status = (status == Status.SHIFT ? Status.DANCE : Status.SHIFT);
            if (status == Status.DANCE) {
                time_counter = DANCE_TURNS;
            }
        }

    	return instructions;
    }

    private int get_index(Point[] dancers, int index) {
        int retIndex = -1;
        for (int i = 0; i < d; i++) {
            if (equals(final_grid[i].x, dancers[index].x) &&
                equals(final_grid[i].y, dancers[index].y)) {
                retIndex = i;
            }
        }
        return retIndex;
    }

    private Point get_point(Point[] dancers, int index, int offset) {
        for (int i = 0; i < d; i++) {
            if (equals(final_grid[i].x, dancers[index].x) &&
                equals(final_grid[i].y, dancers[index].y)) {
                return final_grid[(i + offset) % this.d];
            }
        }
        return dancers[index];
    }

    private boolean equals(double a, double b) {
        return Math.abs(a-b) < 0.0001;
    }
}
