package sqdance.g3;

import sqdance.sim.Point;
import java.util.*;
import java.io.*;

public class SmallPlayer implements sqdance.sim.Player {

    private int d;
    private double room_side;

    private double X_OFFSET = 0.5;
    private double Y_OFFSET = 0.5;

    private double DANCE_DISTANCE = 0.501;
    private double SOULMATE_X = 0.2505;
    private double SOULMATE_Y = 0.434;
    private double NEXT_ROW_DISTANCE = SOULMATE_Y;
    private double NEXT_COL_DISTANCE = 2*DANCE_DISTANCE+0.001;

    private Point[] soulmates;    
    private Point[] soulmates1;
    private Point[] soulmates2;
    private Point[] overall;
    private Point[] grid1;
    private Point[] grid2;

    static private int SHIFT = 0;
    static private int DANCE1 = 1;
    static private int SWAP = 2;
    static private int DANCE2 = 3;
    static private int DANCE_TURNS = 10;

    private int status;
    private int SPLIT_CAP;
    private int time_counter = 0;

    private int num_rows;

    public void init(int d, int room_side) {
    	this.d = d;
    	this.room_side = (double) room_side;

        int half = (int)(d / 2);
        SPLIT_CAP = (((half % 2) == 1) ? (half+1) : half);
        soulmates1 = new Point [half];
        soulmates2 = new Point [half];
        soulmates = new Point [d];
        grid1 = new Point [half];
        grid2 = new Point [half];
        this.num_rows = (int)((room_side - Y_OFFSET) / NEXT_ROW_DISTANCE);
        overall = new Point [d];

        status = DANCE1;
        time_counter = DANCE_TURNS;
    }

    @Override
    public Point[] generate_starting_locations() {
        double gridx = this.X_OFFSET;
        double gridy = this.Y_OFFSET;
        int half = (int)(this.d / 2);
        boolean offcol = false;
        
        int i = 0;
        int i2 = 0;
        String s;

        NEXT_ROW_DISTANCE = 2*SOULMATE_Y;
        num_rows = num_rows / 2;
        while (gridx <= this.room_side) {
            while (gridy <= this.room_side) {
                grid1[i] = new Point(gridx, gridy);
                grid2[i] = new Point((gridx + DANCE_DISTANCE), gridy);
                soulmates1[i] = new Point(gridx + SOULMATE_X, gridy+Y_OFFSET);
                soulmates2[i] = new Point((gridx + DANCE_DISTANCE + SOULMATE_X), gridy+Y_OFFSET);
                i = i + 1;

                if (offcol) {
                    gridy = gridy - NEXT_ROW_DISTANCE;
                } else {
                    gridy = gridy + NEXT_ROW_DISTANCE;
                }
                s = String.format("i is %d, i2 is %d, num_rows is %d, half is %d", i, i2, num_rows, half);
                System.out.println(s);
                i2 = i2 + 1;
                if ((i2 >= num_rows) || (i >= half)){
                    break;
                }
            }
            gridx = gridx + NEXT_COL_DISTANCE;
            offcol = !offcol;
            if (offcol) {
                gridy = Y_OFFSET + ((num_rows-1) * NEXT_ROW_DISTANCE);
            } else {
                gridy = Y_OFFSET;
            }
            i2 = 0;
            if (i >= half) {
                break;
            }
        }
        int limit = half;
        if (i < half) limit = i;

        for (i = 0; i < limit; i++) {
            overall[i] = grid1[i];
            overall[(2*limit)-i-1] = grid2[i];
            soulmates[i] = soulmates1[i];
            soulmates[(2*limit)-i-1] = soulmates2[i];
        }

        return overall;
    }

    @Override
    public Point[] play(Point[] dancers, int[] scores, int[] partner_ids, int[] enjoyment_gained) {
    	Point[] instructions = new Point[d];

        double newx, oldx, newy, oldy;
        String s;


        if (status == SWAP) {
            //System.out.println("WE ARE SWAPPING");
            for (int i = 0; i < d; i++) {
                s = String.format(" we are at %d", i);
                //System.out.println(s);
                if (getIndex(dancers, i) >= SPLIT_CAP) {
                    if ((getIndex(dancers,i) % 2) == 0) {
                        newx = getPoint(dancers,i,1).x;
                        newy = getPoint(dancers,i,1).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    } else {
                        newx = getPoint(dancers,i,-1).x;
                        newy = getPoint(dancers,i,-1).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    }
                } else {
                    instructions[i] = new Point(0,0);
                }

            }
        }

        if (status == SHIFT) {
            //System.out.println("WE ARE SHIFTING");
            for (int i = 0; i < d; i++) {
                s = String.format(" we are at %d", i);
                //System.out.println(s);
                if (getIndex(dancers,i) >= SPLIT_CAP) {
                    if ((getIndex(dancers,i) % 2) == 0) {
                        newx = getPoint(dancers,i,2).x;
                        newy = getPoint(dancers,i,2).y;
                        oldx = getPoint(dancers,i,0).x;
                        oldy = getPoint(dancers,i,0).y;
                        s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                        //System.out.println(s);
                        instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                    } else {
                        instructions[i] = new Point(0,0);
                    }
                } else {
                    newx = getPoint(dancers,i,1).x;
                    newy = getPoint(dancers,i,1).y;
                    oldx = getPoint(dancers,i,0).x;
                    oldy = getPoint(dancers,i,0).y;
                    s = String.format("x move is %f, y move is %f", newx-oldx, newy-oldy);
                    //System.out.println(s);
                    instructions[i] = new Point( (newx - oldx) , (newy - oldy));
                }
            }
        }

        if ((status == DANCE1) || (status == DANCE2)) {
            for (int i = 0; i < d; i++) {

                instructions[i] = new Point(0,0);
                
            }
            time_counter--;
        }

        if (time_counter == 0) {
            status = (status + 1) % 4;
            if ((status == DANCE1) || (status == DANCE2)) {
                time_counter = DANCE_TURNS;
            }
        }

    	return instructions;
    }

    private int getIndex(Point[] dancers, int index) {
        int retIndex = -1;
        for (int i = 0; i < d; i++) {
            if ((overall[i].x == dancers[index].x) && (overall[i].y == dancers[index].y)) {
                retIndex = i;
            }
        }
        String s = String.format("index is %d and corr. overall index is %d, d/2 is %d", index, retIndex,d/2);
        //System.out.println(s);
        return retIndex;
    }

    private Point getPoint(Point[] dancers, int index, int offset) {
        for (int i = 0; i < d; i++) {
            if ((overall[i].x == dancers[index].x) && (overall[i].y == dancers[index].y)) {
                return overall[(i + offset) % this.d];
            }
        }
        return dancers[index];
    }
}
