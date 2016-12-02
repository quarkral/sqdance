package sqdance.g9;

import sqdance.sim.Point;

public class Dancer{

    public int id;
    // partner_id == -1 if dancer had no partner in the last interval

    // nextPos is relative to the dancer's current position
    private Point nextPos;
    private int partner_id;
    public int score;
    private Dancer next;
    private Dancer prev;
    // dance_relationship == "none" if dancer had no partner in the last
    // interval
    private String dance_relationship;
    private boolean shift;

    public Dancer(int id, int score) {
        this.id = id;
        this.score = score;
    }

    /* Used when dancers are initially placed on the dancefloor */
    public Dancer(int id) {
        this.id = id;
        this.partner_id = -1;
        this.score = 0;
        this.next = null;
        this.prev = null;
        this.dance_relationship = "none";
        this.shift = true;
        this.nextPos = new Point(0.0, 0.0);
    }

    /* Used when dancers are initially placed on the dancefloor */
    public Dancer(int id, Point differential) {
        this.id = id;
        this.partner_id = -1;
        this.score = 0;
        this.next = null;
        this.prev = null;
        this.dance_relationship = "none";
        this.shift = true;
        this.nextPos = differential;
    }

    public void setPartnerId(int partner_id){
        this.partner_id = partner_id;
    }

    public int getPartnerId() {
        return this.partner_id;
    }

    public void setNext(Dancer next){
        this.next = next;
    }

    public Dancer getNext() {
        return this.next;
    }

    public void setPrev(Dancer prev){
        this.prev = prev;
    }

    public Dancer getPrev() {
        return this.prev;
    }

    public void setShift(boolean shift){
        this.shift = shift;
    }

    public boolean getShift(){
        return this.shift;
    }

    public void setDanceRelationship(String relationship){
        this.dance_relationship = relationship;
    }

    public String getDanceRelationship(){
        return this.dance_relationship;
    }

    public void setNextPos(Point pnt){
        this.nextPos = pnt;
    }

    public Point getNextPos(){
        return this.nextPos;
    }

    public boolean equals(Dancer other){
        if(other.id == this.id) {
            return true;
        } else {
            return false;
        }
    }
}
